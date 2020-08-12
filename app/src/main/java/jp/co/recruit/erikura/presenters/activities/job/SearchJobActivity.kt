package jp.co.recruit.erikura.presenters.activities.job

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.realm.Realm
import io.realm.Sort
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.ErikuraConfig
import jp.co.recruit.erikura.business.models.JobKind
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.storage.SearchHistory
import jp.co.recruit.erikura.databinding.ActivitySearchJobBinding
import jp.co.recruit.erikura.databinding.FragmentMinMaxPickerBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.util.setOnSafeClickListener
import jp.co.recruit.erikura.presenters.view_models.BaseJobQueryViewModel
import kotlinx.android.parcel.Parcelize
import java.util.*
import kotlin.collections.ArrayList

class SearchJobActivity : BaseActivity(), SearchJobHandlers, MinMaxPickerDialogEventListener {
    companion object {
        val EXTRA_SEARCH_CONDITIONS = "jp.co.recruit.erikura.job.SearchJobActivity.SEARCH_CONDITIONS"
    }

    private val realm: Realm get() = ErikuraApplication.realm
    private val viewModel: SearchJobViewModel by lazy {
        ViewModelProvider(this).get(SearchJobViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivitySearchJobBinding = DataBindingUtil.setContentView(this, R.layout.activity_search_job)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        val textField: AutoCompleteTextView = findViewById(R.id.search_job_text_field)
        val adapter = SearchHistoryAdapter(this, mutableListOf(SearchHistoryItem.CurrentLocation))
        textField.setAdapter(adapter)

        textField.setOnSafeClickListener {
            // すでに表示されている場合のチェックは不要？
            if (!textField.isPopupShowing()) {
                textField.showDropDown()
            }
        }
        textField.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                textField.showDropDown()
            }
        }
        // 完了ボタンでソフトキーボードを閉じるようにする
        textField.setOnEditorActionListener { v, actionId, event ->
            false
        }

        adapter.clear()
        adapter.add(SearchHistoryItem.CurrentLocation)
        adapter.addAll(getHistoryItems().map { SearchHistoryItem.Item(it.keyword) })

        Api(this).jobKinds { jobKinds ->
            viewModel.jobKinds.value = jobKinds.filter { it.refine ?: false }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val view = this.currentFocus
        if (view != null) {
            val layout = findViewById<ConstraintLayout>(R.id.search_job_layout)
            layout.requestFocus()

            val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(layout.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    fun getHistoryItems(limit: Int = 3): List<SearchHistory> {
        return realm.where(SearchHistory::class.java).sort("lastUpdatedAt", Sort.DESCENDING).limit(limit.toLong()).findAll()
    }

    fun addHistoryItem(keyword: String) {
        // 検索履歴を保存します
        realm.executeTransaction { realm ->
            // キーワードに一致する履歴があれば時刻を更新、なければ新しく履歴を保存します
            var history = realm.where(SearchHistory::class.java).equalTo("keyword", keyword).findFirst() ?: run {
                realm.createObject(SearchHistory::class.java, keyword)
            }
            history.lastUpdatedAt = Date()
        }
    }

    override fun onClickClear(view: View) {
        viewModel.keyword.value = ""
    }

    override fun onClickDetailButton(view: View) {
        viewModel.detailButtonVisibility.value = View.GONE
        viewModel.detailConditionsVisibility.value = View.VISIBLE
    }

    override fun onClickSearchButton(view: View) {
        Log.v("KEYWORD: ", viewModel.keyword.value)

        viewModel.normalizedKeyword?.also { keyword ->
            Api(this).geocode(keyword) { latLng ->
                addHistoryItem(keyword)
                Log.v("検索", latLng.toString())

                val intent = Intent()
                intent.putExtra(EXTRA_SEARCH_CONDITIONS, viewModel.query(latLng))
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        } ?: run {
            val locationManager = ErikuraApplication.locationManager
            val latLng = locationManager.latLngOrDefault
            Log.v("検索", latLng.toString())

            val intent = Intent()
            intent.putExtra(EXTRA_SEARCH_CONDITIONS, viewModel.query(latLng))
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    val workingTimeId = "WorkingTime"
    val rewardId = "Reward"

    override fun onClickWorkingTimeSpinner(view: View) {
        val fragment = MinMaxPickerDialogFragment.newInstance(
            workingTimeId,
            viewModel.minimumWorkingTimeItems.value ?: listOf(),
            viewModel.maximumWorkingTimeItems.value ?: listOf(),
            viewModel.minimumWorkingTime.value,
            viewModel.maximumWorkingTime.value
        )
        fragment.show(supportFragmentManager, "workingTimePicker")
    }

    override fun onClickRewardSpinner(view: View) {
        val fragment = MinMaxPickerDialogFragment.newInstance(
            rewardId,
            viewModel.minimumRewardItems.value ?: listOf(),
            viewModel.maximumRewardItems.value ?: listOf(),
            viewModel.minimumReward.value,
            viewModel.maximumReward.value
        )
        fragment.show(supportFragmentManager, "rewordPicker")
    }

    override fun onJobKindSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.jobKindsItems.value?.let {
            val item = it[position]
            viewModel.jobKind.value = item?.value
        }
    }

    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)

        if (fragment is MinMaxPickerDialogFragment) {
            val minMaxFragment = fragment as MinMaxPickerDialogFragment
            minMaxFragment
        }
    }

    override fun onItemPicked(picker: MinMaxPickerDialogFragment, id: String, min: Int, max: Int) {
        when(id) {
            workingTimeId -> {
                viewModel.minimumWorkingTime.value = min
                viewModel.maximumWorkingTime.value = max
            }
            rewardId -> {
                viewModel.minimumReward.value = min
                viewModel.maximumReward.value = max
            }
        }
    }
}

class SearchJobViewModel: BaseJobQueryViewModel() {
    val jobKinds: MutableLiveData<List<JobKind>> = MutableLiveData()
    val detailButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val detailConditionsVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    val minimumWorkingTimeItems: MutableLiveData<List<IntPickerItem>> = MutableLiveData()
    val maximumWorkingTimeItems: MutableLiveData<List<IntPickerItem>> = MutableLiveData()
    val minimumRewardItems: MutableLiveData<List<IntPickerItem>> = MutableLiveData()
    val maximumRewardItems: MutableLiveData<List<IntPickerItem>> = MutableLiveData()

    val jobKindsItems = MediatorLiveData<List<JobKindItem>>().also { result ->
        result.addSource(jobKinds) {
            val items = mutableListOf<JobKindItem>(JobKindItem.Nothing)
            it.forEach { jobKind -> items.add(JobKindItem.Item(jobKind)) }
            result.value = items
        }
    }

    val rewardLabel = MediatorLiveData<String>().also { result ->
        result.addSource(minimumReward) { result.value = formatRewardText() }
        result.addSource(maximumReward) { result.value = formatRewardText() }
    }

    val workingTimeLabel = MediatorLiveData<String>().also { result ->
        result.addSource(minimumWorkingTime) { result.value = formatWorkingTimeText() }
        result.addSource(maximumWorkingTime) { result.value = formatWorkingTimeText() }
    }

    val isSearchButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(keyword) { result.value = isValid() }
    }

    var workingTimes: List<Int> = listOf()
        set(value) {
            field = value
            minimumWorkingTimeItems.value = (listOf(JobQuery.MIN_WORKING_TIME) + value).filterNotNull().map {
                Log.v("TEST", "${formatWorkingTime(it)}, ${it}")
                IntPickerItem(formatWorkingTime(it), it)
            }
            maximumWorkingTimeItems.value = (value + listOf(JobQuery.MAX_WORKING_TIME)).filterNotNull().map {
                Log.v("TEST", "${formatWorkingTime(it)}, ${it}")
                IntPickerItem(formatWorkingTime(it), it)
            }
        }
    var rewards: List<Int> = listOf()
        set(value) {
            field = value
            minimumRewardItems.value = (listOf(JobQuery.MIN_REWARD) + value).map { IntPickerItem(formatReward(it), it) }
            maximumRewardItems.value = (value + listOf(JobQuery.MAX_REWARD)).map { IntPickerItem(formatReward(it), it) }
        }

    init {
        this.workingTimes = ErikuraConfig.workingTimeRange
        this.rewards = ErikuraConfig.rewardRange
        this.workingTimeLabel.value = formatWorkingTimeText()
        this.rewardLabel.value = formatRewardText()
    }

    fun isValid(): Boolean {
        var valid = true
        // キーワードの必須チェック
        if (keyword.value?.isBlank() ?: true) {
            valid = false
        }
        return valid
    }

    fun formatWorkingTimeText(): String {
        return arrayOf(
            minimumWorkingTime.value?.let { formatWorkingTime(it) } ?: "下限なし",
            maximumWorkingTime.value?.let { formatWorkingTime(it) } ?: "上限なし"
        ).joinToString(" 〜 ")
    }

    fun formatRewardText(): String {
        return arrayOf(
            minimumReward.value?.let { formatReward(it) } ?: "下限なし",
            maximumReward.value?.let { formatReward(it) } ?: "上限なし"
        ).joinToString(" 〜 ")
    }

    private fun formatWorkingTime(value: Int): String {
        if (value == JobQuery.MIN_WORKING_TIME) {
            return "下限なし"
        }
        else if (value == JobQuery.MAX_WORKING_TIME) {
            return "上限なし"
        }
        else {
            return String.format("%,d分", value)
        }
    }

    private fun formatReward(value: Int): String {
        if (value == JobQuery.MIN_REWARD) {
            return "下限なし"
        }
        else if (value == JobQuery.MAX_REWARD) {
            return "上限なし"
        }
        else {
            return String.format("%,d円", value)
        }
    }
}

interface SearchJobHandlers {
    fun onClickClear(view: View)
    fun onClickDetailButton(view: View)
    fun onClickSearchButton(view: View)
    fun onClickRewardSpinner(view: View)
    fun onClickWorkingTimeSpinner(view: View)

    fun onJobKindSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
}

sealed class SearchHistoryItem {
    companion object {
        val iconCurrentLocation = R.drawable.history_location
        val iconHistory = R.drawable.history_item
    }

    val name: String
    val iconResourceId: Int

    constructor(name: String, icon: Int) {
        this.name = name
        this.iconResourceId = icon
    }

    override fun toString(): String {
        return name
    }

    object CurrentLocation: SearchHistoryItem(JobQuery.CURRENT_LOCATION, iconCurrentLocation)
    class Item(name: String): SearchHistoryItem(name, iconHistory)
}

class SearchHistoryAdapter(context: Context, items: MutableList<SearchHistoryItem>): ArrayAdapter<SearchHistoryItem>(context, R.layout.fragment_history_downdown_item, R.id.history_dropdown_item_text, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val convertView = convertView ?: run {
            val inflater = LayoutInflater.from(context)
            inflater.inflate(R.layout.fragment_history_downdown_item, parent, false)
        }

        getItem(position)?.let {
            val textView: TextView = convertView.findViewById(R.id.history_dropdown_item_text)
            val imageView: ImageView = convertView.findViewById(R.id.history_dropdown_item_icon)

            textView.text = it.name
            imageView.setImageResource(it.iconResourceId)
        }
        return convertView
    }
}

sealed class JobKindItem(val label: String, val value: JobKind?) {
    object Nothing: JobKindItem("すべて", null)
    class Item(jobKind: JobKind) : JobKindItem(jobKind.name ?: "", jobKind)

    override fun toString(): String {
        return label
    }
}

class MinMaxPickerDialogFragment: DialogFragment(), MinMaxPickerDialogHandlers {
    companion object {
        const val ID_ARGUMENT = "id"
        const val MIN_VALUES_ARGUMENT = "minValues"
        const val MAX_VALUES_ARGUMENT = "maxValues"
        const val MIN_ARGUMENT = "min"
        const val MAX_ARGUMENT = "max"

        fun newInstance(id: String, minValues: List<IntPickerItem>, maxValues: List<IntPickerItem>, min: Int?, max: Int?): MinMaxPickerDialogFragment {
            return MinMaxPickerDialogFragment().also {
                it.arguments = Bundle().also { args ->
                    args.putString(ID_ARGUMENT, id)
                    args.putParcelableArrayList(MIN_VALUES_ARGUMENT, ArrayList(minValues))
                    args.putParcelableArrayList(MAX_VALUES_ARGUMENT, ArrayList(maxValues))
                    args.putParcelable(MIN_ARGUMENT, OptionalInt(min))
                    args.putParcelable(MAX_ARGUMENT, OptionalInt(max))
                }
            }
        }

    }

    private var id: String = ""
    private var minValues: List<IntPickerItem> = listOf()
    private var maxValues: List<IntPickerItem> = listOf()
    private var min: Int? = null
    private var max: Int? = null
    private var listener: MinMaxPickerDialogEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            id = args.getString(ID_ARGUMENT) ?: ""
            minValues = args.getParcelableArrayList<IntPickerItem>(MIN_VALUES_ARGUMENT)?.toList() ?: listOf()
            maxValues = args.getParcelableArrayList<IntPickerItem>(MAX_VALUES_ARGUMENT)?.toList() ?: listOf()
            min = args.getParcelable<OptionalInt>(MIN_ARGUMENT)?.value
            max = args.getParcelable<OptionalInt>(MAX_ARGUMENT)?.value
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)

        val binding: FragmentMinMaxPickerBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context), R.layout.fragment_min_max_picker, null, false
        )
        // viewModel とのバインドします
        val viewModel = MinMaxPickerDialogViewModel(minValues, maxValues)
        binding.viewModel = viewModel
        binding.handlers = this
        binding.lifecycleOwner = this

        // 初期値を選択します
        min?.let { minValue ->
            viewModel.minItemIndex.value = Math.max(minValues.indexOfFirst { item ->
                item.value == minValue
            }, 0)
        }
        max?.let { maxValue ->
            viewModel.maxItemIndex.value = Math.min(maxValues.indexOfFirst { item ->
                item.value == maxValue
            }, maxValues.size - 1)
        }

        builder
            .setView(binding.root)
            .setPositiveButton(getString(R.string.button_ok)) { dialog: DialogInterface, which: Int ->
                val min: IntPickerItem = viewModel.minItem
                val max: IntPickerItem = viewModel.maxItem

                Log.v("MIN-MAX:", "min: ${min.toString()}, max: ${max.toString()}")
                listener?.onItemPicked(this, id, min.value, max.value)
            }
            .setNegativeButton(getString(R.string.button_cancel), null)
        return builder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as? MinMaxPickerDialogEventListener
        }
        catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((context.toString() + " must implement NoticeDialogListener"))
        }
    }
}

class MinMaxPickerDialogViewModel(val minValues: List<IntPickerItem>, val maxValues: List<IntPickerItem>): ViewModel() {
    val minItemIndex: MutableLiveData<Int> = MutableLiveData()
    val maxItemIndex: MutableLiveData<Int> = MutableLiveData()

    val minItem: IntPickerItem get() = minValues[minItemIndex.value ?: 0]
    val maxItem: IntPickerItem get() = maxValues[maxItemIndex.value ?: 0]

    init {
        minItemIndex.value = 0
        maxItemIndex.value = maxValues.size - 1
    }
}

interface MinMaxPickerDialogHandlers {
}

interface MinMaxPickerDialogEventListener {
    fun onItemPicked(picker: MinMaxPickerDialogFragment, id: String, min: Int, max: Int)
}

class ItemPicker : NumberPicker {
    /** コンストラクタの定義 */
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes)

    init {
        wrapSelectorWheel = false
    }

    var items: List<IntPickerItem> = listOf()
}

object ItemPickerAdapter {
    @BindingAdapter("items")
    @JvmStatic fun setItems(view: ItemPicker, items: List<IntPickerItem>) {
        view.items = items
        view.displayedValues = null
        view.minValue = 0
        view.maxValue = items.size - 1
        view.displayedValues = items.map { it.label }.toTypedArray()
    }

    @BindingAdapter("valueAttrChanged")
    @JvmStatic fun setListeners(view: NumberPicker, attrChange: InverseBindingListener) {
        attrChange?.let {
            view.setOnValueChangedListener { picker: NumberPicker, oldValue: Int, newValue: Int ->
                attrChange.onChange()
            }
        }
    }

    @BindingAdapter("value")
    @JvmStatic fun setValue(view: NumberPicker, value: Int) {
        view.value = value
    }

    @InverseBindingAdapter(attribute = "value")
    @JvmStatic fun getValue(view: NumberPicker): Int {
        return view.value
    }
}

@Parcelize
class IntPickerItem(val label: String, val value: Int): Parcelable {}

@Parcelize
data class OptionalInt(val value: Int?): Parcelable {}