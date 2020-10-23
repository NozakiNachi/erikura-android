package jp.co.recruit.erikura.business.models

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.ImageView
import androidx.core.database.getLongOrNull
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import jp.co.recruit.erikura.ErikuraApplication
import com.bumptech.glide.request.target.Target
import jp.co.recruit.erikura.business.util.UrlUtils
import kotlinx.android.parcel.Parcelize
import java.io.ByteArrayOutputStream
import java.io.File


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
        fun createFrom(context: Context, uri: Uri): MediaItem? {
            val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                // DocumentProvider
                when {
                    isExternalStorageDocument(uri) -> {
                        // External Storage Provider
                        return createFromExternalStorageProvider(context, uri)
                    }
                    isDownloadsDocument(uri) -> {
                        // Downloads Provider
                        return createFromDownloadsProvider(context, uri)
                    }
                    isMediaDocument(uri) -> {
                        // Media Provider
                        return createFromMediaProvider(context, uri)
                    }
                }
            }
            else if ("content" == uri.scheme?.toLowerCase()) {
                // MediaStore (and general)
                return createFromMediaStore(context, uri)
            }
            else if ("file" == uri.scheme?.toLowerCase()) {
                // File
                val file = File(uri.path)
                return MediaItem(size = file.length(), contentUri = uri, dateAdded = file.lastModified())
            }
            return null
        }

        private fun createFromExternalStorageProvider(context: Context, uri: Uri): MediaItem?{
            val documentId = DocumentsContract.getDocumentId(uri)
            val (type, path) = documentId.split(":")

            if ("primary" == type.toLowerCase()) {
                return context.getExternalFilesDir(path)?.let { file ->
                    MediaItem(size = file.length(), contentUri = uri, dateAdded = file.lastModified())
                }
            }
            return null
        }

        private fun createFromDownloadsProvider(context: Context, uri: Uri): MediaItem? {
            val documentId = DocumentsContract.getDocumentId(uri)
//            val contentUri =
//                ContentUris.withAppendedId(
//                    Uri.parse("content://downloads/public_downloads"),
//                    documentId.toLong()
//                )
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_ADDED
                ),
                null, null, null
            )
            try {
                return cursor?.let { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
                        val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE))
                        val dateAdded = cursor.getLongOrNull(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED))

                        MediaItem(id = id, size = size, contentUri = uri, dateAdded = dateAdded)
                    } else {
                        null
                    }
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        private fun createFromMediaProvider(context: Context, uri: Uri): MediaItem? {
            val documentId = DocumentsContract.getDocumentId(uri)
            val (type, id) = documentId.split(":")

            val contentUri: Uri = when(type) {
                "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "video" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "audio" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                else    -> throw IllegalArgumentException("unknown media type")
            }
            val cursor = context.contentResolver.query(
                contentUri,
                arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_ADDED,
                    MediaStore.MediaColumns.DATE_TAKEN
                ),
                "_id=?", arrayOf(id), null
            )
            try {
                return cursor?.let { cursor ->
                    if (cursor.moveToFirst()) {
                        MediaItem.from(cursor)
                    } else {
                        null
                    }
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        private fun createFromMediaStore(context: Context, uri: Uri): MediaItem? {
            val documentId = DocumentsContract.getDocumentId(uri)
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DATE_ADDED
                ),
                null, null, null
            )
            try {
                return cursor?.let { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
                        val size = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE))
                        val dateAdded = cursor.getLongOrNull(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED))

                        MediaItem(id = id, size = size, contentUri = uri, dateAdded = dateAdded)
                    } else {
                        null
                    }
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        private fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        private fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        private fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

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

    fun loadImage(context: Context, imageView: ImageView, width: Int, height: Int) {
        Glide.with(context).load(contentUri).override(height).into(imageView)
    }

    fun loadImageFromString(context: Context, imageView: ImageView) {
        val s = UrlUtils.parse(contentUri.toString()).toString()
        Glide.with(context).load(s).into(imageView)
    }

    fun loadImageFromString(context: Context, imageView: ImageView, width: Int, height: Int) {
        val s = UrlUtils.parse(contentUri.toString()).toString()
        Glide.with(context).load(s).override(height).into(imageView)
    }

    fun resizeImage(context: Context, imageHeight: Int, imageWidth: Int, onComplete: (resource: Bitmap) -> Unit, onError: (e: Exception?) -> Unit) {
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
                    onComplete(resource)
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

    fun getWidthAndHeight(activity: Activity, exifInterface: ExifInterface): Pair<Int, Int> {
        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        var width: Int
        var height: Int
        when(orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                height = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                width = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                height = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                width = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            }
            else -> {
                width = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                height = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            }
        }
        // widthとheightが取得できなかった場合はbitmapから取得します
        if (height == 0 && width == 0) {
            // uriから読み込み用InputStreamを生成
            val inputStream = activity.contentResolver?.openInputStream(contentUri!!)
            // inputStreamからbitmap生成
            val imageBitmap = BitmapFactory.decodeStream(inputStream)
            height = imageBitmap.height
            width = imageBitmap.width
        }
        return Pair(width, height)
    }
}