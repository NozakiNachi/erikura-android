package jp.co.recruit.erikura.presenters.fragments

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.NumberPicker
import android.widget.RelativeLayout
import android.widget.ToggleButton
import androidx.databinding.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication.Companion.applicationContext
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.databinding.FragmentSearchBarConditionItemBinding
import jp.co.recruit.erikura.databinding.FragmentSearchBarConditionLabelBinding
import jp.co.recruit.erikura.databinding.FragmentToggleSwitchViewBinding
import jp.co.recruit.erikura.presenters.activities.job.ItemPicker
import jp.co.recruit.erikura.presenters.activities.job.PickerItem
import kotlinx.android.synthetic.main.fragment_toggle_switch_view.view.*
import okhttp3.internal.notifyAll

class ToggleSwitchView : FrameLayout, ToggleSwitchHandlers {
//    var adapter: ToggleSwitchAdapter

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val binding: FragmentToggleSwitchViewBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.fragment_toggle_switch_view, this, true)
        binding.handlers = this

//        adapter = ToggleSwitchAdapter()
        var items: List<PickerItem<*>> = listOf()
    }

    var isChecked: Boolean
        get() {
            return this.notification.isChecked
        }
        set(value) {
            this.notification.isChecked = value
            if (value) {
                animOn()
            }
            else {
                animOff()
            }
        }

    override fun onClick(view: View) {
        if (this.notification.isChecked) {
            animOff()
        }
        else {
            animOn()
        }
        // if(notification_boolean == true){
        // 現在通知がオンになっている状態
        // notification_settings.xmlのandroid:checkdから値を取得=notidication_booleanに格納

        // ①アニメーションオフ処理
        // ②該当のViewModelにfalseを保持

        // notification_settingの該当スイッチに対してandroid:checkd=falseにする
        // android:checkd={viewmodel.***}の形になっているのでCheckdにtrue/falseを渡せばviewmodelに保持できる
    //}

        // if(notification_boolean == false){
        // 現在通知がオフになっている状態
        // ①アニメーションオン処理
        // ②該当のViewModelにtrueを保持
        //}



//        adapter.notifyAll()
    }

    fun animOn() {
        val btnAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_btn_on
        )
        val bgWhiteAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_bg_white_on
        )
        val bgGreenAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_bg_green_on
        )

        var mTglBtn = findViewById<View>(R.id.notification) as ToggleButton
        var mTglBgWhite = findViewById(R.id.white) as View
        var mTglBgGreen = findViewById(R.id.green) as View

        mTglBtn.startAnimation(btnAnim)
        mTglBgWhite.startAnimation(bgWhiteAnim)
        mTglBgGreen.startAnimation(bgGreenAnim)
    }

    fun animOff() {
        val btnAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_btn_off
        )
        val bgWhiteAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_bg_white_off
        )
        val bgGreenAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_bg_green_off
        )

        var mTglBtn = findViewById<View>(R.id.notification) as ToggleButton
        var mTglBgWhite = findViewById(R.id.white) as View
        var mTglBgGreen = findViewById(R.id.green) as View

        mTglBtn.startAnimation(btnAnim)
        mTglBgWhite.startAnimation(bgWhiteAnim)
        mTglBgGreen.startAnimation(bgGreenAnim)
    }
}

interface ToggleSwitchHandlers {
    fun onClick(view: View)
}

class ToggleSwitchViewModel: ViewModel() {
    val notification_boolean: MutableLiveData<Boolean> = MutableLiveData()
}


object ToggleSwitchAdapter {
    @BindingAdapter("checkedAttrChanged")
    @JvmStatic fun setListeners(view: ToggleSwitchView, attrChange: InverseBindingListener) {
        attrChange?.let {
//            view.setOnValueChangedListener { picker: NumberPicker, oldValue: Int, newValue: Int ->
//                attrChange.onChange()
//            }
        }
    }

    @BindingAdapter("checked")
    @JvmStatic fun setValue(view: ToggleSwitchView, value: Boolean) {
        view.isChecked = value
    }

    @InverseBindingAdapter(attribute = "checked")
    @JvmStatic fun getValue(view: ToggleSwitchView): Boolean {
        return view.isChecked
    }
}

//class SearchBarView : RelativeLayout {
//    var adapter: SearchBarConditionsAdapter
//    private var conditionsView: RecyclerView
//
//    constructor(context: Context) : this(context, null)
//    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
//    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
//        LayoutInflater.from(context).inflate(R.layout.fragment_search_bar, this, true)
//
//        adapter = SearchBarConditionsAdapter(listOf(JobQuery.CURRENT_LOCATION))
//        conditionsView = findViewById(R.id.search_bar_conditions)
//        conditionsView.setHasFixedSize(true)
//        conditionsView.adapter = adapter
//        conditionsView.addItemDecoration(SearchBarConditionItemDecorator())
//
//        val tapGestureDetector = GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {
//            override fun onDoubleTap(e: MotionEvent?): Boolean {
//                return true
//            }
//
//            override fun onSingleTapUp(e: MotionEvent?): Boolean {
//                this@SearchBarView.callOnClick()
//                return true
//            }
//        })
//        conditionsView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
//            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
//                return true
//            }
//
//            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
//                tapGestureDetector.onTouchEvent(e)
//            }
//
//            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
//            }
//        })
//    }
//}

//object SearchBarViewAdapter {
//    @BindingAdapter("conditions")
//    @JvmStatic
//    fun setConditions(view: SearchBarView, conditions: List<String>?) {
//        view.adapter.conditions = conditions ?: listOf(JobQuery.CURRENT_LOCATION)
//        view.adapter.notifyDataSetChanged()
//    }
//}
//
//class SearchBarConditionsHolder(val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root)
//
//class SearchBarConditionsAdapter(var conditions: List<String>) : RecyclerView.Adapter<SearchBarConditionsHolder>() {
//    companion object {
//        const val VIEW_CONDITION = 1
//        const val VIEW_LABEL = 2
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchBarConditionsHolder {
//        val binding = when(viewType) {
//            VIEW_CONDITION -> DataBindingUtil.inflate<FragmentSearchBarConditionItemBinding>(
//                LayoutInflater.from(parent.context),
//                R.layout.fragment_search_bar_condition_item,
//                parent,
//                false)
//            VIEW_LABEL -> DataBindingUtil.inflate<FragmentSearchBarConditionLabelBinding>(
//                LayoutInflater.from(parent.context),
//                R.layout.fragment_search_bar_condition_label,
//                parent,
//                false)
//            else -> DataBindingUtil.inflate<FragmentSearchBarConditionItemBinding>(
//                LayoutInflater.from(parent.context),
//                R.layout.fragment_search_bar_condition_item,
//                parent,
//                false)
//
//        }
//        return SearchBarConditionsHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: SearchBarConditionsHolder, position: Int) {
//        when(holder.binding) {
//            is FragmentSearchBarConditionLabelBinding -> {
//                // 何も行いません
//            }
//            is FragmentSearchBarConditionItemBinding -> {
//                holder.binding.condition = conditions[position]
//            }
//        }
//    }
//
//    override fun getItemCount(): Int {
//        return conditions.size + 1
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        return if (position < conditions.size) {
//            VIEW_CONDITION
//        }
//        else {
//            VIEW_LABEL
//        }
//    }
//}
//
//class SearchBarConditionItemDecorator: RecyclerView.ItemDecoration() {
//    override fun getItemOffsets(
//        outRect: Rect,
//        view: View,
//        parent: RecyclerView,
//        state: RecyclerView.State
//    ) {
//        super.getItemOffsets(outRect, view, parent, state)
//        outRect.left = view.resources.getDimensionPixelSize(R.dimen.condition_margin)
//    }
//}