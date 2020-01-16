package jp.co.recruit.erikura.presenters.util

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.databinding.BindingAdapter

object MarkerViewIconBindingAdapter {
    @BindingAdapter("app:bitmap")
    @JvmStatic
    fun loadImage(view: ImageView, bitmap: Bitmap?) {
        bitmap?.let {
            view.setImageBitmap(it)
        }
    }
}
