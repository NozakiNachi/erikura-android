package jp.co.recruit.erikura.presenters.fragments

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ToggleButton
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import jp.co.recruit.erikura.ErikuraApplication.Companion.applicationContext
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.FragmentToggleSwitchViewBinding
import kotlinx.android.synthetic.main.fragment_toggle_switch_view.view.*

class ToggleSwitchView : FrameLayout, ToggleSwitchHandlers {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        val binding: FragmentToggleSwitchViewBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.fragment_toggle_switch_view, this, true)
        binding.handlers = this
    }

        var isChecked: Boolean
        get() {
            return this.notification.isChecked
        }
        set(value) {
            this.notification.isChecked = value
        }


    override fun onCheckedChanges(view: View, isChecked: Boolean) {
        if (isChecked) {
            animOn()
        }
        else {
            animOff()
        }
    }

    private fun animOn() {
        val btnAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_btn_on
        )
        val bgWhiteAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_bg_white_on
        )
        val bgGreenAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_bg_green_on
        )

        var TglBtn = findViewById<View>(R.id.notification) as ToggleButton
        var TglBgWhite = findViewById(R.id.white) as View
        var TglBgGreen = findViewById(R.id.green) as View

        TglBtn.startAnimation(btnAnim)
        TglBgWhite.startAnimation(bgWhiteAnim)
        TglBgGreen.startAnimation(bgGreenAnim)
    }

    private fun animOff() {
        val btnAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_btn_off
        )
        val bgWhiteAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_bg_white_off
        )
        val bgGreenAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_bg_green_off
        )

        var TglBtn = findViewById<View>(R.id.notification) as ToggleButton
        var TglBgWhite = findViewById(R.id.white) as View
        var TglBgGreen = findViewById(R.id.green) as View

        TglBtn.startAnimation(btnAnim)
        TglBgWhite.startAnimation(bgWhiteAnim)
        TglBgGreen.startAnimation(bgGreenAnim)
    }
}

interface ToggleSwitchHandlers {
    fun onCheckedChanges(view: View, isChecked: Boolean)
}

object ToggleSwitchAdapter {
    @BindingAdapter("checkedAttrChanged")
    @JvmStatic fun setListeners(view: ToggleSwitchView, attrChange: InverseBindingListener) {
        attrChange?.let {
            view.notification.setOnCheckedChangeListener { buttonView, isChecked ->
                view.onCheckedChanges(view, isChecked)
                attrChange.onChange()
            }
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
