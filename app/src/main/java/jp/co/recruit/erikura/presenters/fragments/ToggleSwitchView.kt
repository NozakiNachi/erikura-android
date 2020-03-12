package jp.co.recruit.erikura.presenters.fragments

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.NumberPicker
import android.widget.ToggleButton
import androidx.databinding.*
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication.Companion.applicationContext
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.databinding.FragmentSearchBarConditionItemBinding
import jp.co.recruit.erikura.databinding.FragmentSearchBarConditionLabelBinding
import jp.co.recruit.erikura.databinding.FragmentToggleSwitchViewBinding
import jp.co.recruit.erikura.presenters.activities.job.ItemPicker
import jp.co.recruit.erikura.presenters.activities.job.PickerItem

class ToggleSwitchView : FrameLayout, ToggleSwitchHandlers {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val binding: FragmentToggleSwitchViewBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.fragment_toggle_switch_view, this, true)
        binding.handlers = this

    }

    override fun onClick(view: View) {
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
}



interface ToggleSwitchHandlers {
    fun onClick(view: View)
}