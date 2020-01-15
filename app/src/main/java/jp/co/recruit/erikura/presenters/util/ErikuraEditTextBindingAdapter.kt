package jp.co.recruit.erikura.presenters.util

import android.view.View
import androidx.databinding.BindingAdapter
import android.view.View.OnFocusChangeListener
import android.widget.EditText



object ErikuraEditTextBindingAdapter {
    @BindingAdapter("android:onFocusChanged")
    @JvmStatic
    fun onFocusChanged(text: EditText, listener: View.OnFocusChangeListener) {
        text.onFocusChangeListener = listener
    }
}