package jp.co.recruit.erikura.business.models

import com.google.android.gms.maps.model.LatLng
import java.util.*

enum class JobStatus {
    /** 募集中 */
    Normal,
    /** 応募済み */
    Applied,
    /** 作業中 */
    Working,
    /** 作業完了 */
    Finished,
    /** 作業報告済み */
    Reported,
    /** 作業期間終了 */
    Past,
    /** 募集前 */
    Future,
}

data class Job(
    var id: Int,
    var placeId: Int,
    var place: Place,
    var title: String?,
    var workingStartAt: Date,
    var workingFinishAt: Date,
    var fee: Int,
    var workingTime: Int,
    var workingPlace: String,
    var summary: String,
    var tools: String,
    var entryQuestion: String?,
    var latitude: Double,
    var longitude: Double,
    var thumbnailUrl: String?,
    var manualUrl: String?,
    var modelReportUrl: String?,
    var wanted: Boolean = false,
    var boost: Boolean = false,
    var distance: Int?,
    var jobKind: JobKind,
    var entryId: Int?,
    var entry: Entry?,
    var reportId: Int?,
    var report: Report?,
    var reEntryPermitted: Boolean = false,
    var summaryTitles: List<String> = listOf()
) {

    // isReportCreatable
    // isReportEditable
    // getSummaryTitles

    val isActive: Boolean get() = !(isFuture || isPastOrInactive)
    /** 期限切れ、もしくは応募済みかを判定します */
    val isPastOrInactive: Boolean get() = (isPast || isEntried)
    /** 募集期間が過ぎたタスクか? */
    val isPast: Boolean get() = (Date() > this.workingFinishAt)
    /** 募集期間前のタスクか? */
    val isFuture: Boolean get() = (Date() < this.workingStartAt)
    /** 作業期間切れのタスクか? */
    val isExpired: Boolean get() = (this.limitAt?.let { Date() > it } ?: false)
    /** 応募済みの場合の作業リミット時間 */
    val limitAt: Date? get() = entry?.limitAt
    /** 自身が応募済みか */
    val isOwner: Boolean get() = (entry?.owner ?: false)
    /** 応募済みか */
    val isEntried: Boolean get() = (entry != null)
    /** 作業報告済みか */
    val isReported: Boolean get() = (report != null)
    /** 作業終了済みか */
    val isFinished: Boolean get() = (entry?.isFinished ?: false)
    /** 作業開始済みか */
    val isStarted: Boolean get() = (entry?.isStarted ?: false)
    /** 作業報告が確認されているか */
    val isAccepted: Boolean get() = (report?.isAccepted ?: false)
    /** 作業報告が差し戻しされているか */
    val isRejected: Boolean get() = (report?.isRejected ?: false)
    /** 募集直前(24時間以内)のタスクか */
    val isStartSoon: Boolean get() {
        return isFuture && ((Date().time - this.workingStartAt.time) < (24 * 60 * 60 * 1000))
    }

    /** 案件の状態を取得します */
    val status: JobStatus get() {
        if (isEntried && isOwner) {
            if (isReported) {
                return JobStatus.Reported
            }
            else if (isFinished) {
                return JobStatus.Finished
            }
            else if (isStarted) {
                return JobStatus.Working
            }
            else {
                return JobStatus.Applied
            }
        }
        else {
            return JobStatus.Normal
        }
    }

    /** 作業報告を作成可能かを判定します */
    val isReportCreatable: Boolean get() {
        return when {
            isReported -> false     // レポートがすでに存在するので作成できない
            isExpired -> false      // 作業期限が切れているので作成できない
            else -> true            // 上記以外は作成可能
        }
    }
    /** 作業報告を編集可能かを判定します */
    val isReportEditable: Boolean get() {
        return when {
            !isReported -> false    // レポートが存在しないので、編集はできない
            isAccepted -> false     // レポートが確定済みなので、編集はできない
            isRejected -> true      // レポートが差し戻しされた場合は編集可能
            else -> true            // 上記以外のケースは編集可能
        }
    }

    val latLng: LatLng get() = LatLng(latitude, longitude)
}