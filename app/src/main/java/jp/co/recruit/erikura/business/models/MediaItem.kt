package jp.co.recruit.erikura.business.models

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import kotlinx.android.parcel.Parcelize
import java.io.FileOutputStream


@Parcelize
data class MediaItem(val id: Long = 0, val mimeType: String = "", val size: Long = 0, val contentUri: Uri? = null) :
    Parcelable {
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

    fun resizeImage(context: Context, imageHeight: Int, imageWidth: Int) {
        Glide.with(context)
            .asBitmap()
            .load(contentUri)
            .into(object : CustomTarget<Bitmap>(imageWidth, imageHeight){
                override fun onLoadCleared(placeholder: Drawable?) {}

                override fun onResourceReady(
                    resource: Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                ) {
                    write(context, "photo.jpg", resource)
                }
            })
    }

    fun write(context: Context, fileName: String?, bitmap: Bitmap) {
        val outputStream: FileOutputStream
        try {
            outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

}