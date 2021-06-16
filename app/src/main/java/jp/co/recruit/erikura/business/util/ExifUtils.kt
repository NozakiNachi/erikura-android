package jp.co.recruit.erikura.business.util

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.crashlytics.FirebaseCrashlytics
import jp.co.recruit.erikura.business.models.OutputSummary
import java.text.SimpleDateFormat
import java.util.*

object ExifUtils {
    open class TakenAtFormat(val pattern: String) {
        private val regex: Regex = Regex(pattern)
        fun matches(takenAt: String): Boolean {
            return regex.matches(takenAt)
        }

        open fun parse(takenAt: String): Date? = null
    }
    class TakenAtBySimpleDateFormat(pattern: String, val format: String): TakenAtFormat(pattern) {
        private val formatter: SimpleDateFormat = SimpleDateFormat(format)

        override fun parse(takenAt: String): Date? {
            return formatter.parse(takenAt)
        }
    }
    class TakenAtAsTimeInMillis(): TakenAtFormat("""\d{13,}""") {
        override fun parse(takenAt: String): Date? {
            val timeInMillis = takenAt.toLong()
            return Date(timeInMillis)
        }
    }

    private val takenAtFormats: Array<TakenAtFormat> = arrayOf(
        // EXIF規格書で定義されているフォーマット
        TakenAtBySimpleDateFormat("""\d{4}:\d{2}:\d{2} \d{2}:\d{2}:\d{2}""", "yyyy:MM:dd HH:mm:ss"),
        // Xperia XZs のフォーマット?
        TakenAtBySimpleDateFormat("""\d{4}/\d{2}/\d{2}_\d{2}:\d{2}:\d{2}""", "yyyy/MM/dd_HH:mm:ss"),
        // Galaxy A30 でのフォーマット?
        TakenAtAsTimeInMillis()
    )


    /**
     * 画像のURIを元に ExifInterface を取得します
     */
    fun exifInterface(context: Context, uri: Uri): ExifInterface? {
        return context.contentResolver.openInputStream(uri)?.let { input ->
            ExifInterface(input)
        }
    }

    /**
     * EXIFより撮影日時の情報を取得します
     */
    fun takenAt(exifInterface: ExifInterface?): Date? {
        return exifInterface?.getAttribute(ExifInterface.TAG_DATETIME)?.let { takenAtString ->
            takenAtFormats.find { it.matches(takenAtString) }?.let { fmt ->
                fmt.parse(takenAtString)
            } ?:run {
                FirebaseCrashlytics.getInstance()
                    .recordException(OutputSummary.UnknownDatetimeFormat("Unknown datetime format: $takenAtString"))
                null
            }
        }
    }

    fun latLng(exifInterface: ExifInterface?): LatLng? {
        return exifInterface?.let { exifInterface ->
            val latitudeHMS = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
            val latitudeRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
            val longitudeHMS = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
            val longitudeRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)

            val latitude = latitudeHMS?.let { lat ->
                latitudeRef?.let { ref ->
                    exifLatitudeToDegrees(ref, lat)
                }
            }
            val longitude = longitudeHMS?.let { lon ->
                longitudeRef?.let { ref ->
                    exifLongitudeToDegrees(ref, lon)
                }
            }
            return if (latitude != null && longitude != null) {
                LatLng(latitude, longitude)
            } else {
                null
            }
        }
    }

    fun size(context: Context, uri: Uri, exifInterface: ExifInterface?): Pair<Int, Int> {
        return exifInterface?.let {
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
            if (width > 0 && height > 0) {
                Pair(width, height)
            } else { null }
        } ?: run {
            // uriから読み込み用InputStreamを生成
            val inputStream = context.contentResolver?.openInputStream(uri)
            // inputStreamからbitmap生成
            val imageBitmap = BitmapFactory.decodeStream(inputStream)
            val height = imageBitmap.height
            val width = imageBitmap.width
            Pair(width, height)
        }
    }

    private fun exifLatitudeToDegrees(ref: String, latitude: String): Double {
        return if (ref == "S") {
            -1.0 * parseExifHourMinSrcToDegrees(latitude)
        }
        else {
            1.0  * parseExifHourMinSrcToDegrees(latitude)
        }
    }

    private fun exifLongitudeToDegrees(ref: String, longitude: String): Double {
        return if (ref == "W") {
            -1.0 * parseExifHourMinSrcToDegrees(longitude)
        }
        else {
            1.0  * parseExifHourMinSrcToDegrees(longitude)
        }
    }

    private fun parseExifHourMinSrcToDegrees(hourMinSec: String): Double {
        // FIXME: 形式が一致しない場合の対策をする必要がある
        // num1/denom1,num2/denum2,num3/denum3
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