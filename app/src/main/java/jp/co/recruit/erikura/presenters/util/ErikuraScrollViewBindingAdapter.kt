package jp.co.recruit.erikura.presenters.util

import android.view.View
import androidx.databinding.BindingAdapter

object ErikuraScrollViewBindingAdapter {
    @BindingAdapter("android:onScrollChange")
    @JvmStatic
    fun onScrollChange(view: View, listener: View.OnScrollChangeListener) {
        view.setOnScrollChangeListener(listener)
    }
}