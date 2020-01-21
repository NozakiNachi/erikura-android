package jp.co.recruit.erikura.presenters.fragments

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.ViewModel
import jp.co.recruit.erikura.R

class ThumbnailImageFragment(val bitmap: Bitmap) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //container!!.findViewById<ImageView>(R.id.thumbnailImage_image).setImageBitmap(bitmap)
        return inflater.inflate(R.layout.fragment_thumbnail_image, container, false)
    }
}



