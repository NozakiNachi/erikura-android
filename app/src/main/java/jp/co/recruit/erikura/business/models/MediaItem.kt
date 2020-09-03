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
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.crashlytics.android.Crashlytics
import jp.co.recruit.erikura.business.util.UrlUtils
import kotlinx.android.parcel.Parcelize
import java.io.ByteArrayOutputStream
import java.lang.RuntimeException


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

        private fun parseExifHourMinSrcToDegrees(hourMinSec: String): Double {
            val (hourStr, minStr, secStr) = hourMinSec.split(",")
            val (hourN, hourD) = hourStr.split("/").map { it.toInt() }
            val (minN,  minD)  = minStr.split("/").map { it.toInt() }
            val (secN,  secD)  = secStr.split("/").map { it.toInt() }
            val hour = hourN.toDouble() / hourD.toDouble()
            val min  = minN.toDouble() / minD.toDouble()
            val sec  = secN.toDouble() / secD.toDouble()
            return hour + (min / 60.0) + (sec / 3600.0)
        }
    }

    fun loadImage(context: Context, imageView: ImageView) {
        Glide.with(context).load(contentUri).into(imageView)
    }

    fun loadImageFromString(context: Context, imageView: ImageView) {
        val s = UrlUtils.parse(contentUri.toString()).toString()
        Glide.with(context).load(s).into(imageView)
    }

    fun resizeImage(context: Context, imageHeight: Int, imageWidth: Int, onComplete: (bytes: ByteArray) -> Unit, onError: (e: Exception?) -> Unit) {
        Glide.with(context)
            .asBitmap()
            .load(contentUri)
            .listener(object: RequestListener<Bitmap> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                    // ロードに失敗している場合
                    onError(e)
                    return true
                }

                override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    // ロードに成功している場合
                    return false    // false を返すと以降のメソッドチェーンが実行される
                }
            })
            .into(object : CustomTarget<Bitmap>(imageWidth, imageHeight){
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    // リサイズに失敗している場合
                    onError(RuntimeException("画像のリサイズ処理に失敗しました"))
                }

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