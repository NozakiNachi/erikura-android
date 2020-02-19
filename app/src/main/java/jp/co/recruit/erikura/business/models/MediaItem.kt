package jp.co.recruit.erikura.business.models

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.widget.ImageView
import com.bumptech.glide.Glide

data class MediaItem(val id: Long, val mimeType: String, val size: Long, val contentUri: Uri) {
    companion object {
        fun from(cursor: Cursor): MediaItem {
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
            val mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE))
            val size = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE))
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

            return MediaItem(id = id, mimeType = mimeType, size = size, contentUri = uri)
        }
    }

    fun loadImage(context: Context, imageView: ImageView) {
        Glide.with(context).load(contentUri).into(imageView)
    }
}