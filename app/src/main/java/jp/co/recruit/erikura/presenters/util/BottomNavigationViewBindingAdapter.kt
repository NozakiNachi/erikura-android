package jp.co.recruit.erikura.presenters.util

import androidx.databinding.BindingAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView

object BottomNavigationViewBindingAdapter {
    @BindingAdapter("onNavigationItemSelected")
    @JvmStatic fun setOnNavigationItemSelected(view: BottomNavigationView, listener: BottomNavigationView.OnNavigationItemSelectedListener) {
        view.setOnNavigationItemSelectedListener(listener)
    }
}