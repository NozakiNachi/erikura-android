package jp.co.recruit.erikura.presenters.util

import android.view.View
import android.widget.EditText
import androidx.databinding.BindingAdapter


object ErikuraEditTextBindingAdapter {
    @BindingAdapter("android:onFocusChanged")
    @JvmStatic
    fun onFocusChanged(text: EditText, listener: View.OnFocusChangeListener) {
        text.onFocusChangeListener = listener
    }
}