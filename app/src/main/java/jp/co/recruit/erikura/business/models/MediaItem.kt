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
import androidx.core.database.getLongOrNull
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.business.util.UrlUtils
import kotlinx.android.parcel.Parcelize
import java.io.ByteArrayOutputStream


@Parcelize
data class MediaItem(
    val id: Long = 0,
    val mimeType: String = "",
    val size: Long = 0,
    val contentUri: Uri? = null,
    val dateAdded: Long? = null,
    val dateTaken: Long? = null
) : Parcelable {
    var uploading: Boolean = false

    companion object {
        fun from(cursor: Cursor): MediaItem {
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
            val mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE))
            val size = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE))
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            val dateAdded = cursor.getLongOrNull(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED))
            val dateTaken = cursor.getLongOrNull(cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN))

            return MediaItem(id = id, mimeType = mimeType, size = size, contentUri = uri, dateAdded = dateAdded, dateTaken = dateTaken)
        }

        fun parseExifHourMinSrcToDegrees(hourMinSec: String): Double {
            val (hourStr, minStr, secStr) = hourMinSec.split(",")
            val (hourN, hourD) = hourStr.split("/").map { it.toInt() }
            val (minN,  minD)  = minStr.split("/").map { it.toInt() }
            val (secN,  secD)  = secStr.split("/").map { it.toInt() }
            val hour = hourN.toDouble() / hourD.toDouble()
            val min  = minN.toDouble() / minD.toDouble()
            val sec  = secN.toDouble() / secD.toDouble()
            return hour + (min / 60.0) + (sec / 3600.0)
        }

        fun exifLatitudeToDegrees(ref: String, latitude: String): Double {
            return if (ref == "S") {
                -1.0 * parseExifHourMinSrcToDegrees(latitude)
            }
            else {
                1.0  * parseExifHourMinSrcToDegrees(latitude)
            }
        }

        fun exifLongitudeToDegrees(ref: String, longitude: String): Double {
            return if (ref == "W") {
                -1.0 * parseExifHourMinSrcToDegrees(longitude)
            }
            else {
                1.0  * parseExifHourMinSrcToDegrees(longitude)
            }
        }
    }

    fun loadImage(context: Context, imageView: ImageView) {
        Glide.with(context).load(contentUri).into(imageView)
    }

    fun loadImageFromString(context: Context, imageView: ImageView) {
        val s = UrlUtils.parse(contentUri.toString()).toString()
        Glide.with(context).load(s).into(imageView)
    }

    fun resizeReportImage(context: Context, imageHeight: Int, imageWidth: Int, onComplete: (bytes: ByteArray) -> Unit) {
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

    fun resizeIdentifyImage(context: Context, imageHeight: Int, imageWidth: Int, onComplete: (bytes: ByteArray) -> Unit) {
        Glide.with(context)
            .asBitmap()
            .load(contentUri)
            .override(imageWidth, imageHeight)
            .into(object : CustomTarget<Bitmap>(imageWidth, imageHeight) {
                override fun onLoadCleared(placeholder: Drawable?) {}

                override fun onResourceReady(
                    resource: Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                ) {
                    val outputStream = ByteArrayOutputStream()
                    resource.compress(Bitmap.CompressFormat.JPEG, ErikuraApplication.ID_IMAGE_QUALITY, outputStream)
                    outputStream.close()
                    val bytes: ByteArray = outputStream.toByteArray()
                    onComplete(bytes)
                }
            })
    }
}