package jp.co.recruit.erikura.business.models

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.MediaStore
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import jp.co.recruit.erikura.business.util.UrlUtils
import kotlinx.android.parcel.Parcelize
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.net.URL


@Parcelize
data class MediaItem(val id: Long = 0, val mimeType: String = "", val size: Long = 0, val contentUri: Uri? = null, val photoTakenAt: String = "") :
    Parcelable {
    companion object {
        fun from(cursor: Cursor): MediaItem {
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
            val mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE))
            val size = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE))
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            val takenAt = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN))

            return MediaItem(id = id, mimeType = mimeType, size = size, contentUri = uri, photoTakenAt = takenAt.toString())
        }
    }

    fun loadImage(context: Context, imageView: ImageView) {
        Glide.with(context).load(contentUri).into(imageView)
    }

    fun loadImageFromString(context: Context, imageView: ImageView) {
        val s = UrlUtils.parse(contentUri.toString()).toString()
        Glide.with(context).load(s).into(imageView)
    }

    fun resizeImage(context: Context, imageHeight: Int, imageWidth: Int, onComplete: (bytes: ByteArray) -> Unit) {
        Glide.with(context)
            .asBitmap()
            .load(contentUri)
            .into(object : CustomTarget<Bitmap>(imageWidth, imageHeight){
                override fun onLoadCleared(placeholder: Drawable?) {}

                override fun onResourceReady(
                    resource: Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                ) {
                    val outputStream = ByteArrayOutputStream()
                    resource.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.close()
                    val bytes: ByteArray = outputStream.toByteArray()
                    onComplete(bytes)
                }
            })
    }
}