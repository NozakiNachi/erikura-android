package jp.co.recruit.erikura.presenters.activities.job

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.PointerIcon
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.realm.Realm
import io.realm.Sort
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.JobKind
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.storage.SearchHistory
import jp.co.recruit.erikura.databinding.ActivitySearchJobBinding
import jp.co.recruit.erikura.databinding.FragmentMinMaxPickerBinding
import jp.co.recruit.erikura.presenters.view_models.BaseJobQueryViewModel
import java.util.*

class SearchJobActivity : AppCompatActivity(), SearchJobHandlers {
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

        textField.setOnClickListener {
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

        adapter.clear()
        adapter.add(SearchHistoryItem.CurrentLocation)
        adapter.addAll(getHistoryItems().map { SearchHistoryItem.Item(it.keyword) })

        Api(this).jobKinds { jobKinds ->
            viewModel.jobKinds.value = jobKinds
        }
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
            locationManager.latLng?.let { latLng ->
                Log.v("検索", latLng.toString())

                val intent = Intent()
                intent.putExtra(EXTRA_SEARCH_CONDITIONS, viewModel.query(latLng))
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    override fun onClickWorkingTimeSpinner(view: View) {
        val fragment = MinMaxPickerDialogFragment(
            viewModel.minimumWorkingTimeItems.value ?: listOf(),
            viewModel.maximumWorkingTimeItems.value ?: listOf(),
            viewModel.minimumWorkingTimeItem.value,
            viewModel.maximumWorkingTimeItem.value
        ) { min: PickerItem<Int>, max: PickerItem<Int> ->
            viewModel.minimumWorkingTimeItem.value = min
            viewModel.maximumWorkingTimeItem.value = max
        }
        // FIXME: 作業時間の最大、最小などどのバインディング
        fragment.show(supportFragmentManager, "workingTimePicker")
    }

    override fun onClickRewardSpinner(view: View) {
        val fragment = MinMaxPickerDialogFragment(
            viewModel.minimumRewardItems.value ?: listOf(),
            viewModel.maximumRewardItems.value ?: listOf(),
            viewModel.minimumRewardItem.value,
            viewModel.maximumRewardItem.value
        ) { min: PickerItem<Int>, max: PickerItem<Int> ->
            viewModel.minimumRewardItem.value = min
            viewModel.maximumRewardItem.value = max
        }
        // FIXME: 報酬の最大、最小などとのバインディング
        fragment.show(supportFragmentManager, "rewordPicker")
    }

    override fun onJobKindSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        viewModel.jobKindsItems.value?.let {
            val item = it[position]
            viewModel.jobKind.value = item?.value
        }
    }
}

class SearchJobViewModel: BaseJobQueryViewModel() {
    val jobKinds: MutableLiveData<List<JobKind>> = MutableLiveData()
    val detailButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val detailConditionsVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    val minimumWorkingTimeItems: MutableLiveData<List<PickerItem<Int>>> = MutableLiveData()
    val maximumWorkingTimeItems: MutableLiveData<List<PickerItem<Int>>> = MutableLiveData()
    val minimumRewardItems: MutableLiveData<List<PickerItem<Int>>> = MutableLiveData()
    val maximumRewardItems: MutableLiveData<List<PickerItem<Int>>> = MutableLiveData()

    val minimumWorkingTimeItem: MutableLiveData<PickerItem<Int>> = MutableLiveData()
    val maximumWorkingTimeItem: MutableLiveData<PickerItem<Int>> = MutableLiveData()
    val minimumRewardItem: MutableLiveData<PickerItem<Int>> = MutableLiveData()
    val maximumRewardItem: MutableLiveData<PickerItem<Int>> = MutableLiveData()

    val jobKindsItems = MediatorLiveData<List<JobKindItem>>().also { result ->
        result.addSource(jobKinds) {
            val items = mutableListOf<JobKindItem>(JobKindItem.Nothing)
            it.forEach { jobKind -> items.add(JobKindItem.Item(jobKind)) }
            result.value = items
        }
    }

    val rewardLabel = MediatorLiveData<String>().also { result ->
        result.addSource(minimumRewardItem) { result.value = formatRewardText() }
        result.addSource(maximumRewardItem) { result.value = formatRewardText() }
    }

    val workingTimeLabel = MediatorLiveData<String>().also { result ->
        result.addSource(minimumWorkingTimeItem) { result.value = formatWorkingTimeText() }
        result.addSource(maximumWorkingTimeItem) { result.value = formatWorkingTimeText() }
    }

    val isSearchButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(keyword) { result.value = isValid() }
    }

    var workingTimes: List<Int> = listOf()
        set(value) {
            field = value
            minimumWorkingTimeItems.value =
                listOf(PickerItem("下限なし", JobQuery.MIN_WORKING_TIME)) +
                        workingTimes.map{ PickerItem(String.format("%,d分", it), it) }
            maximumWorkingTimeItems.value =
                workingTimes.map{ PickerItem(String.format("%,d分", it), it) } +
                        listOf(PickerItem("上限なし", JobQuery.MAX_WORKING_TIME))

            minimumWorkingTimeItem.value = minimumWorkingTimeItems.value?.first()!! // 下限なしは必ず存在するので !! を付与
            maximumWorkingTimeItem.value = maximumWorkingTimeItems.value?.last()!!  // 上限なしは必ず存在するので !! を付与
        }
    var rewards: List<Int> = listOf()
        set(value) {
            field = value
            minimumRewardItems.value =
                listOf(PickerItem("下限なし", JobQuery.MIN_REWARD)) +
                        rewards.map{ PickerItem(String.format("%,d円", it), it) }
            maximumRewardItems.value =
                rewards.map{ PickerItem(String.format("%,d円", it), it) } +
                        listOf(PickerItem("上限なし", JobQuery.MAX_REWARD))

            minimumRewardItem.value = minimumRewardItems.value?.first()!! // 下限なしは必ず存在するので !! を付与
            maximumRewardItem.value = maximumRewardItems.value?.last()!!  // 上限なしは必ず存在するので !! を付与
        }

    init {
        // FIXME: API での作業時間の取得
        this.workingTimes = listOf(1, 5, 10, 15, 20, 25, 30, 45, 60, 75, 90, 105, 120, 180, 240, 300, 360, 420, 480)
        // FIXME: APIでの報酬の取得
        this.rewards = listOf(1, 5, 10, 50, 100, 500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000)
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
            minimumWorkingTimeItem.value?.label ?: "",
            maximumWorkingTimeItem.value?.label ?: ""
        ).joinToString(" 〜 ")
    }

    fun formatRewardText(): String {
        return arrayOf(
            minimumRewardItem.value?.label ?: "",
            maximumRewardItem.value?.label ?: ""
        ).joinToString(" 〜 ")
    }
}

interface SearchJobHandlers {
    fun onClickDetailButton(view: View)
    fun onClickSearchButton(view: View)
    fun onClickRewardSpinner(view: View)
    fun onClickWorkingTimeSpinner(view: View)

    fun onJobKindSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
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

class MinMaxPickerDialogFragment<T>(
    val minValues: List<PickerItem<T>>,
    val maxValues: List<PickerItem<T>>,
    val minItem: PickerItem<T>?,
    val maxItem: PickerItem<T>?,
    val onComplete: (min: PickerItem<T>, max: PickerItem<T>) -> Unit
): DialogFragment(), MinMaxPickerDialogHandlers {
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
        minItem?.let { viewModel.minItemIndex.value = minValues.indexOf(it) }
        maxItem?.let { viewModel.maxItemIndex.value = maxValues.indexOf(it) }

        builder
            .setView(binding.root)
            .setPositiveButton("OK") { dialog: DialogInterface, which: Int ->
                val min: PickerItem<T> = viewModel.minItem as PickerItem<T>
                val max: PickerItem<T> = viewModel.maxItem as PickerItem<T>

                Log.v("MIN-MAX:", "min: ${min.toString()}, max: ${max.toString()}")
                onComplete(min, max)
            }
            .setNegativeButton("Cancel", null)
        return builder.create()
    }
}

class MinMaxPickerDialogViewModel(val minValues: List<PickerItem<*>>, val maxValues: List<PickerItem<*>>): ViewModel() {
    val minItemIndex: MutableLiveData<Int> = MutableLiveData()
    val maxItemIndex: MutableLiveData<Int> = MutableLiveData()

    val minItem: PickerItem<*> get() = minValues[minItemIndex.value ?: 0]
    val maxItem: PickerItem<*> get() = maxValues[maxItemIndex.value ?: 0]

    init {
        minItemIndex.value = 0
        maxItemIndex.value = maxValues.size - 1
    }
}

interface MinMaxPickerDialogHandlers {
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

    var items: List<PickerItem<*>> = listOf()
}

object ItemPickerAdapter {
    @BindingAdapter("items")
    @JvmStatic fun setItems(view: ItemPicker, items: List<PickerItem<*>>) {
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

class PickerItem<T>(val label: String, val value: T) {}
