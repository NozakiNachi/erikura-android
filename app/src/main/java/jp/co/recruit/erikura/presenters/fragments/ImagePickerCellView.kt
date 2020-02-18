package jp.co.recruit.erikura.presenters.fragments

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ToggleButton
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.FragmentImagePickerCellViewBinding
import android.widget.Toast
import android.widget.CompoundButton


class ImagePickerCellView : FrameLayout {
    var imageView: ImageView
    var toggleButton: ToggleButton
    var toggleClickListener: ToggleClickListener? = null

    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): this(context, attrs, defStyleAttr, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes) {
        // Viewの中身を作成します
        val binding = DataBindingUtil.inflate<FragmentImagePickerCellViewBinding>(
            LayoutInflater.from(context),
            R.layout.fragment_image_picker_cell_view,
            this, true)

        imageView = binding.root.findViewById(R.id.image_picker_cell_image)
        toggleButton = binding.root.findViewById(R.id.image_picker_cell_check)

        // FIXME: Layout の中身には何が必要か
        // 1. ImageView
        // 2. ToggleButton

        toggleButton.setOnClickListener {
            toggleClickListener?.apply {
                onClick(toggleButton.isChecked)
            }
        }

    }

    interface ToggleClickListener {
        fun onClick(isChecked: Boolean)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}



object ImagePickerCellViewAdapter {
    @BindingAdapter("checked")
    @JvmStatic
    fun setChecked(view: ImagePickerCellView, checked: Boolean) {
        view.toggleButton.isChecked = checked
    }

    @InverseBindingAdapter(attribute = "checked")
    @JvmStatic
    fun getChecked(view: ImagePickerCellView): Boolean {
        return view.toggleButton.isChecked
    }

    @BindingAdapter("checkedAttrChanged")
    @JvmStatic
    fun setListeners(view: ImagePickerCellView, attrChange: InverseBindingListener) {
        attrChange?.let {
            view.toggleButton.setOnCheckedChangeListener { _buttonView, _isChecked ->
                attrChange.onChange()
            }
        }
    }
}