package jp.co.recruit.erikura.presenters.fragments

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.databinding.FragmentSearchBarConditionItemBinding
import jp.co.recruit.erikura.databinding.FragmentSearchBarConditionLabelBinding
import kotlinx.android.synthetic.main.activity_map_view.*

class SearchBarView : RelativeLayout {
    var adapter: SearchBarConditionsAdapter
    private var conditionsView: RecyclerView

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.fragment_search_bar, this, true)

        adapter = SearchBarConditionsAdapter(listOf(JobQuery.CURRENT_LOCATION))
        conditionsView = findViewById(R.id.search_bar_conditions)
        conditionsView.setHasFixedSize(true)
        conditionsView.adapter = adapter
        conditionsView.addItemDecoration(SearchBarConditionItemDecorator())

        val tapGestureDetector = GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent?): Boolean {
                return true
            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                this@SearchBarView.callOnClick()
                return true
            }
        })
        conditionsView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return true
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                tapGestureDetector.onTouchEvent(e)
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            }
        })
    }
}

object SearchBarViewAdapter {
    @BindingAdapter("conditions")
    @JvmStatic
    fun setConditions(view: SearchBarView, conditions: List<String>?) {
        view.adapter.conditions = conditions ?: listOf(JobQuery.CURRENT_LOCATION)
        view.adapter.notifyDataSetChanged()
    }
}

class SearchBarConditionsHolder(val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root)

class SearchBarConditionsAdapter(var conditions: List<String>) : RecyclerView.Adapter<SearchBarConditionsHolder>() {
    companion object {
        const val VIEW_CONDITION = 1
        const val VIEW_LABEL = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchBarConditionsHolder {
        val binding = when(viewType) {
            VIEW_CONDITION -> DataBindingUtil.inflate<FragmentSearchBarConditionItemBinding>(
                    LayoutInflater.from(parent.context),
                    R.layout.fragment_search_bar_condition_item,
                    parent,
                    false)
            VIEW_LABEL -> DataBindingUtil.inflate<FragmentSearchBarConditionLabelBinding>(
                    LayoutInflater.from(parent.context),
                    R.layout.fragment_search_bar_condition_label,
                    parent,
                    false)
            else -> DataBindingUtil.inflate<FragmentSearchBarConditionItemBinding>(
                    LayoutInflater.from(parent.context),
                    R.layout.fragment_search_bar_condition_item,
                    parent,
                    false)

        }
        return SearchBarConditionsHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchBarConditionsHolder, position: Int) {
        when(holder.binding) {
            is FragmentSearchBarConditionLabelBinding -> {
                // 何も行いません
            }
            is FragmentSearchBarConditionItemBinding -> {
                holder.binding.condition = conditions[position]
            }
        }
    }

    override fun getItemCount(): Int {
        return conditions.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < conditions.size) {
            VIEW_CONDITION
        }
        else {
            VIEW_LABEL
        }
    }
}

class SearchBarConditionItemDecorator: RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.left = view.resources.getDimensionPixelSize(R.dimen.condition_margin)
    }
}