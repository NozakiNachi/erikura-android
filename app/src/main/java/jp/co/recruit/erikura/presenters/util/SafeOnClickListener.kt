package jp.co.recruit.erikura.presenters.util

import android.os.SystemClock
import android.view.View
import androidx.databinding.BindingAdapter

abstract class SafeOnClickListener(
    private val safeClickInterval: Long = 1000
) : View.OnClickListener {
    private var lastClickTime: Long = 0

    override fun onClick(v: View?) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime < safeClickInterval) {
            // 前回のクリック時間から、一定時間経過していない場合は、イベントを無視します
            return
        }
        onSafeClick(v)
        lastClickTime = SystemClock.elapsedRealtime()
    }

    abstract fun onSafeClick(v: View?)
}

fun View.setOnSafeClickListener(listener: View.OnClickListener?) {
    setOnClickListener(object: SafeOnClickListener() {
        override fun onSafeClick(v: View?) {
            listener?.onClick(v)
        }
    })
}

fun View.setOnSafeClickListener(onSafeClickHandler: ((View?) -> Unit)?) {
    setOnClickListener(object: SafeOnClickListener() {
        override fun onSafeClick(v: View?) {
            onSafeClickHandler?.invoke(v)
        }
    })
}

object SafeOnClickBindingAdapter {
    @BindingAdapter("onSafeClick")
    @JvmStatic fun setOnSafeClick(view: View?, listener: View.OnClickListener?) {
        view?.setOnSafeClickListener(listener)
    }
}