package jp.co.recruit.erikura.business.models

import android.app.Activity
import android.os.Parcelable
import jp.co.recruit.erikura.data.network.Api
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import java.util.*
import io.reactivex.schedulers.Schedulers
import io.reactivex.Completable
import jp.co.recruit.erikura.ErikuraApplication
import okhttp3.internal.notifyAll
import okhttp3.internal.wait


enum class ReportStatus {
    Unconfirmed,
    Accepted,
    Rejected
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
    var additionalOPeratorComments: List<@RawValue OperatorComment> = listOf(),
    var evaluation: String? = null,
    var comment: String? = null,
    var owner: Boolean = false,
    var operatorLikeCount: Int = 0,
    var operatorCommentsCount: Int = 0,
    var acceptedAt: Date? = null,
    var rejectedAt: Date? = null,
    var rejectComment: String? = null,
    var createdAt: Date? = null,
    var outputSummaries: List<@RawValue OutputSummary> = listOf()
): Parcelable {
    // photoAsset
    // isUploadCompleted
    // activeOutputSummaryCount
    // activeOUputSummary
    // activeIndexOf
    // validate

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

    val isUploadCompleted: Boolean get() {
        if (additionalPhotoAsset?.contentUri == null) {
            return true
        }else {
            return !additionalReportPhotoToken.isNullOrBlank()
        }
    }

    /*
        var isUploadCompleted2: Boolean {
        get {
            guard additionalReportPhotoAsset != nil else { return true }
            if let token = additionalReportPhotoToken, !token.isEmpty {
                return true
            } else {
                return false
            }
        }
    }

    // 削除されていないOutputSummariesの数をカウントします
    func activeOutputSummariesCount() -> Int {
        if let summaries = outputSummaries {
            return summaries.reduce(0) { (result, summary) -> Int in
                return summary.willDelete ?? false ? result : result + 1
            }
        }
        else {
            return 0
        }
    }

    func activeOtputSummary(index: Int) -> OutputSummary? {
        if let summaries = outputSummaries {
            let activeSummaries = summaries.filter { (summary) -> Bool in
                return !(summary.willDelete ?? false)
            }
            return activeSummaries[index]
        }
        else {
            return nil
        }
    }

    func activeIndexOf(summary: OutputSummary) -> Int {
        var index = 0;
        if let summaries = outputSummaries {
            for (_, s) in summaries.enumerated() {
                if s === summary {
                    return index
                }
                if !(s.willDelete ?? false) {
                    index = index + 1
                }
            }
        }
        return -1
    }

    func validate() -> Bool {
        var valid = true
        if let summaries = outputSummaries {
            // 報告箇所が存在することを確認します
            if summaries.filter({ !($0.willDelete ?? false) }).count < 1 {
                valid = false
            }
            for summary in summaries {
                if !(summary.willDelete ?? false) {
                    valid = summary.validate() && valid
                }
            }
        }
        else {
            valid = false
        }

        valid = Validator.maxLength(5000).apply(additionalComment) && valid
        valid = Validator.maxLength(5000).apply(comment) && valid

        return valid
    }
     */


    // 画像アップロード処理
    fun uploadPhoto(activity: Activity, job: Job, item: MediaItem?, onComplete: (token: String) -> Unit) {

        val completable = Completable.fromAction {
            // 画像リサイズ処理
            item?.let {
                item.resizeImage(activity, 640, 640) { bytes ->
                    // 画像アップロード処理
                    Api(activity).imageUpload(item, bytes) { token ->
                        outputSummaries[0].beforeCleaningPhotoToken = token
                        onComplete(token)
                        synchronized(ErikuraApplication.instance.uploadMonitor) {
                            ErikuraApplication.instance.uploadMonitor.notifyAll()
                        }
                    }
                }
            }
        }
            .subscribeOn(Schedulers.single())
        completable.subscribe()

    }
}