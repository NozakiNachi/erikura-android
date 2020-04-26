package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import jp.co.recruit.erikura.data.storage.PhotoTokenManager
import kotlinx.android.parcel.Parcelize
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
    // photoAsset
    // isUploadCompleted
    // validate

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
}
