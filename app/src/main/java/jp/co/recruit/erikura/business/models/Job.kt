package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.presenters.util.LocationManager
import kotlinx.android.parcel.Parcelize
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

    /** み初期化 */
    Uninitialized,
}

@Parcelize
data class Job(
    var id: Int=0,
    var placeId: Int = 0,
    var place: Place? = null,
    var title: String? = null,
    var workingStartAt: Date? = null,
    var workingFinishAt: Date? = null,
    val nextUpdateScheduledAt: Date? = null,
    var fee: Int = 0,
    var workingTime: Int = 0,
    var workingPlace: String? = null,
    var summary: String? = null,
    var tools: String? = null,
    var workableStart: String? = null,
    var workableFinish: String? = null,
    var entryQuestion: String? = null,
    var entryInformation: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var thumbnailUrl: String? = null,
    var manualUrl: String? = null,
    var modelReportUrl: String? = null,
    var wanted: Boolean = false,
    var boost: Boolean = false,
    var distance: Int? = 0,
    var jobKind: JobKind? = null,
    var entryId: Int? = 0,
    var entry: Entry? = null,
    var report: Report? = null,
    var reEntryPermitted: Boolean = false,
    var summaryTitles: List<String> = listOf(),
    var targetGender: Gender? = null,
    var banned: Boolean = false,
    val createdAt: Date? = null,
    var cautionsCount: Int? = null,
    var goodExamplesCount: Int? = null
): Parcelable {
    var uninitialized: Boolean = false
    val reportId: Int? get() = report?.id

    val isActive: Boolean get() {
        return !(isFuture || isPastOrInactive)
    }
    /** 期限切れ、もしくは応募済みかを判定します */
    val isPastOrInactive: Boolean get() {
        return (isPast || isEntried)
    }
    /** 募集期間が過ぎたタスクか? */
    val isPast: Boolean get() {
        return (workingFinishAt?.let { Date() > it } ?: false)
    }
    /** 募集期間前のタスクか? */
    val isFuture: Boolean get() {
        return (workingStartAt?.let { Date() < it } ?: false)
    }
    /** 作業期間切れのタスクか? */
    val isExpired: Boolean get() {
        return (this.limitAt?.let { Date() > it } ?: false)
    }
    /** 応募済みの場合の作業リミット時間 */
    val limitAt: Date? get() = entry?.limitAt
    /** 自身が応募済みか */
    val isOwner: Boolean get() = (entry?.owner ?: false)
    /** 応募済みか */
    val isEntried: Boolean get() = (entry != null)
    /** 作業報告済みか */
    val isReported: Boolean get() = (reportId != null)
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
        return isFuture && (Math.abs(Date().time - this.workingStartAt!!.time) < (24 * 60 * 60 * 1000))
    }

    /** 案件の状態を取得します */
    val status: JobStatus get() {
        if (uninitialized) { return JobStatus.Uninitialized }

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

    val latLng: LatLng get() = LatLng(latitude?: LocationManager.defaultLatLng.latitude, longitude?: LocationManager.defaultLatLng.longitude)

    /**
     * 案件の募集している性別と一致しているかを判定します
     */
    fun isGenderMatched(user: User?): Boolean {
        // 最小の性別が指定されていない場合
        return targetGender?.let { targetGender ->
            user?.let { user ->
                targetGender == user.gender
            } ?: true
        } ?: true
    }

    /**
     * 応募可能可を判定します
     */
    fun isApplicable(user: User?): Boolean {
        return notApplicableReason(user).isNullOrBlank()
    }

    /**
     * 応募不可の理由を取得します
     */
    fun notApplicableReason(user: User?): String? {
        return when {
            // 未来、もしくは過去案件の場合
            (isFuture || isPast) -> ErikuraApplication.instance.getString(R.string.jobDetails_outOfEntryExpire)
            // すでに応募済みの場合
            (isEntried) -> ErikuraApplication.instance.getString(R.string.jobDetails_entryFinished)
            // Ban された案件の場合
            (banned) -> ErikuraApplication.instance.getString(R.string.jobDetails_entryFinished)
            // 対象の性別ではない場合
            (!isGenderMatched(user)) -> ErikuraApplication.instance.getString(R.string.jobDetails_entryFinished)
            // 再応募不可の場合
            (!reEntryPermitted) -> ErikuraApplication.instance.getString(R.string.jobDetails_cantEntry)
            // 応募可能件数を超えている場合
            (user?.let { it.holdingJobs >= it.maxJobs } ?: false) -> ErikuraApplication.instance.getString(R.string.jobDetails_maxEntry, user?.maxJobs ?: 0)
            else -> null
        }

    }
    /*
    private fun decideWarningCaption(): String? {
        return job.value?.let { job ->
        }
    }
     */
}