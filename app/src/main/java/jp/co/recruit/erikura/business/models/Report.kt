package jp.co.recruit.erikura.business.models

import android.app.Activity
import android.graphics.Bitmap
import android.os.Parcelable
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.annotations.SerializedName
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.storage.PhotoTokenManager
import kotlinx.android.parcel.Parcelize
import okhttp3.internal.closeQuietly
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors

enum class ReportStatus {
    Unconfirmed,
    Accepted,
    Rejected
}

enum class EvaluateType(val value: String, val resourceId: Int) {
    // 選択してください
    UNSELECTED("unselected", R.string.please_select),
    // 異常あり、未対応
    BAD("bad", R.string.bad),
    // 異常あり、対応済み
    ORDINARY("ordinary", R.string.ordinary),
    // 異常あり、未対応
    GOOD("good", R.string.good),
}

@Parcelize
data class Report (
    var id: Int? = null,
    var jobId: Int = 0,
    var workingMinute: Int? = null,
    var additionalComment: String? = null,
    var additionalPhotoAsset: MediaItem? = null,
    var additionalReportPhotoUrl: String? = null,
    var additionalReportPhotoWillDelete: Boolean = false,
    var additionalReportPhotoToken: String? = null,
    var additionalOperatorLikes: Boolean = false,
    var additionalOperatorComments: List<OperatorComment> = listOf(),
    var evaluation: String? = null,
    var comment: String? = null,
    var owner: Boolean = false,
    var operatorLikesCount: Int = 0,
    var operatorCommentsCount: Int = 0,
    var acceptedAt: Date? = null,
    var rejectedAt: Date? = null,
    var rejectComment: String? = null,
    var createdAt: Date? = null,
    @SerializedName("output_summaries_attributes")
    var outputSummaries: List<OutputSummary> = listOf(),
    var deleted: Boolean = false
): Parcelable {
    companion object {
        val executor = Executors.newFixedThreadPool(5)
        val scheduler = Schedulers.from(executor)
    }

    // photoAsset
    // isUploadCompleted
    // activeOutputSummaryCount
    // activeOUputSummary
    // activeIndexOf
    // validate

    // 削除されていない実施箇所のリストを取得します
    val activeOutputSummaries: List<OutputSummary> get() = outputSummaries.filter { !it.willDelete }

    val isAccepted: Boolean get() = (acceptedAt != null)
    val isRejected: Boolean get() = (rejectedAt != null && !isAccepted)

    val status: ReportStatus get() {
        if (isAccepted) {
            return ReportStatus.Accepted
        }
        else if (isRejected) {
            return ReportStatus.Rejected
        }
        else {
            return ReportStatus.Unconfirmed
        }
    }

    /**
     * 画像を選択して変更したかを返却します
     */
    val isPhotoChanged: Boolean get() {
        // アップロードされた場合、contentUri が設定される。
        // アプリから取得された作業報告の場合は、contentUri と、PhotoUrlの両方に同じ値が設定されている
        if (additionalPhotoAsset?.contentUri == null || additionalReportPhotoUrl == additionalPhotoAsset?.contentUri.toString()) {
            return false
        }
        return true
    }

    /**
     * 画像アップロードが完了しているかを確認します
     */
    fun isUploadCompleted(job: Job): Boolean {
        return if (isPhotoChanged) {
            if (additionalReportPhotoToken.isNullOrBlank()) {
                additionalReportPhotoToken = PhotoTokenManager.getToken(job, additionalPhotoAsset?.contentUri.toString())
            }
            // トークンが設定されていればアップロード済みとして想定します
            !additionalReportPhotoToken.isNullOrBlank()
        }
        else {
            // 写真が変更されていないので、アップロード完了として処理します
            true
        }
    }

    fun isUploading(): Boolean {
        return if (isPhotoChanged) { additionalPhotoAsset?.uploading ?: false } else { false }
    }

    /**
     * 報告箇所のアップロードが完了しているか確認します
     */
    fun isOutputSummaryPhotoUploadCompleted(job: Job): Boolean {
        return activeOutputSummaries.all { it.isUploadCompleted(job) }
    }

    fun isOutputSummaryPhotoUploading(): Boolean {
        return activeOutputSummaries.any { it.isUploading() }
    }

    // 画像アップロード処理
    fun uploadPhoto(activity: Activity, job: Job, item: MediaItem?, onComplete: (token: String) -> Unit) {
        val completable = Completable.fromAction {
            // 画像リサイズ処理
            item?.let {
                item.uploading = true
                item.resizeImage(activity, 640, 640, onComplete = { resource: Bitmap ->
                    // リサイズしたものをファイルに書き出しておきます
                    val temp = File.createTempFile("resizedImage", "jpg", activity.cacheDir)
                    val os = temp.outputStream()
                    try {
                        resource.compress(Bitmap.CompressFormat.JPEG, 90, os)
                    }
                    finally {
                        os.closeQuietly()
                    }

                    // 画像アップロード処理
                    Api(activity).imageUpload(item, temp, scheduler = Report.scheduler, onError = {
                        if (temp.exists()) {
                            temp.delete()
                        }
                        Log.e("Error in waiting upload", it.toString())
                        item.uploading = false
                        ErikuraApplication.instance.notifyUpload()
                    }) { token ->
                        if (temp.exists()) {
                            temp.delete()
                        }
                        item.uploading = false
                        onComplete(token)
                        ErikuraApplication.instance.notifyUpload()
                    }
                }, onError = { e ->
                    if (e != null) {
                        FirebaseCrashlytics.getInstance().recordException(e)
                    }

                    // エラーが発生した場合には、そのままの形でアップロードを行います
                    try {
                        // 画像アップロード処理
                        Api(activity).imageUpload(item, activity, onError = {
                            Log.e("Error in waiting upload", it.toString())
                            item.uploading = false
                            ErikuraApplication.instance.notifyUpload()
                        }) { token ->
                            item.uploading = false
                            onComplete(token)
                            ErikuraApplication.instance.notifyUpload()
                        }
                        item.uploading = false
                        ErikuraApplication.instance.notifyUpload()
                    }
                    catch(e: IOException) {
                        FirebaseCrashlytics.getInstance().recordException(e)
                        item.uploading = false
                        ErikuraApplication.instance.notifyUpload()
                    }
                })
            }
        }
        completable.subscribeOn(Report.scheduler).subscribe()
    }
}