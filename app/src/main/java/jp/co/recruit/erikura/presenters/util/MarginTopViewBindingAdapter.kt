package jp.co.recruit.erikura.presenters.util

import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView

object MarginTopViewBindingAdapter {
    @BindingAdapter("bind:layout_marginTop")
    @JvmStatic fun setOnMarginTop(view: View, marginTop: Int) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val displayMetrics = view.resources.displayMetrics
            val density = displayMetrics?.density ?: 1f
            var p: ViewGroup.MarginLayoutParams  = view.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(p.leftMargin, (marginTop * density).toInt(), p.rightMargin, p.bottomMargin)
            view.requestLayout()
        }
    }
}