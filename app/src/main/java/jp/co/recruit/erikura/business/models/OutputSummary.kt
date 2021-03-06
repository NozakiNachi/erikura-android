package jp.co.recruit.erikura.business.models

import android.media.ExifInterface
import android.net.Uri
import android.os.Parcelable
import androidx.fragment.app.FragmentActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import jp.co.recruit.erikura.business.util.ExifUtils
import jp.co.recruit.erikura.data.storage.PhotoTokenManager
import kotlinx.android.parcel.Parcelize
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
data class OutputSummary(
    var id: Int? = null,
    var place: String? = null,
    var evaluation: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var photoTakedAt: Date? = null,
    var comment: String? = null,
    var beforeCleaningPhotoToken: String? = null,
    var beforeCleaningPhotoUrl: String? = null,
    var operatorLikes: Boolean = false,
    var operatorComments: List<OperatorComment> = listOf(),
    var willDelete: Boolean = false,
    var photoAsset: MediaItem? = null
) : Parcelable {
    /**
     * 画像を選択して変更したかを返却します
     */
    val isPhotoChanged: Boolean get() {
        // アップロードされた場合、contentUri が設定される。
        // アプリから取得された作業報告の場合は、contentUri と、PhotoUrlの両方に同じ値が設定されている
        if (photoAsset?.contentUri == null || photoAsset?.contentUri.toString() == beforeCleaningPhotoUrl) {
            return false
        }
        return true
    }

    /**
     * API でのポスト時にリクエストにPOSTする必要があるかを判断します
     */
    val needsToSendAPI: Boolean get() {
        // ID が存在する場合は、一度永続化されているため、APIにポストします
        if (this.id != null) { return true }
        // 削除フラグが off の場合も、登録が必要なので、APIにポストします
        if (!this.willDelete) { return true }

        // それ以外(永続化されておらず、かつ削除予定)の場合は、ポストは不要
        return false
    }

    /**
     * 画像アップロードが完了しているかを確認します
     */
    fun isUploadCompleted(job: Job): Boolean {
        return if (isPhotoChanged) {
            if (beforeCleaningPhotoToken.isNullOrBlank()) {
                beforeCleaningPhotoToken = PhotoTokenManager.getToken(job, photoAsset?.contentUri.toString())
            }
            // トークンが設定されていればアップロード済みとして想定します
            !beforeCleaningPhotoToken.isNullOrBlank()
        }
        else {
            // 写真が変更されていないので、アップロード完了として処理します
            true
        }
    }

    fun isUploading(): Boolean {
        return if (isPhotoChanged) { photoAsset?.uploading ?: false } else { false }
    }

    /**
     * EXIF情報から撮影日時、緯度/経度などの情報を取得します
     */
    fun retrieveImageProperties(activity: FragmentActivity) {
        // 写真が選択されていない場合は抜ける
        if (photoAsset == null) { return }
        // 写真が変更されていない場合は抜ける
        if (!isPhotoChanged) { return }

        try {
            val exifInterface = ExifUtils.exifInterface(activity, photoAsset?.contentUri ?: Uri.EMPTY)
            // 撮影日時を取得します
            this.photoTakedAt = ExifUtils.takenAt(exifInterface) ?: photoAsset?.dateTaken?.let {
                Date(it)
            } ?: photoAsset?.dateAdded?.let {
                Date(it * 1000) // 秒単位のための、x1000してミリ秒単位とする
            }
            ExifUtils.latLng(exifInterface)?.let { latLng ->
                this.latitude = latLng.latitude
                this.longitude = latLng.longitude
            }
        }
        catch (e: IOException) {
            // 例外によって取得できない場合
            this.photoTakedAt = photoAsset?.dateTaken?.let {
                Date(it)
            } ?: photoAsset?.dateAdded?.let {
                Date(it * 1000) // 秒単位のための、x1000してミリ秒単位とする
            }
            // Crashlytics に例外を通知しておきます
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    class UnknownDatetimeFormat(msg: String): Exception(msg) {}
}
