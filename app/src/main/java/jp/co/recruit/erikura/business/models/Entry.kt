package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Entry(
    var id: Int = 0,
    var userId: Int = 0,
    var jobId: Int = 0,
    var comment: String? = null,
    var limitAt: Date? = null,
    var startedAt: Date? = null,
    var startedLatitude: Double? = null,
    var startedLongitude: Double? = null,
    var startedSteps: Int? = 0,
    var startedDistance: Double? = 0.0,
    var startedFloorAsc: Int? = 0,
    var startedFloorDesc: Int? = 0,
    var finishedAt: Date? = null,
    var finishedLatitude: Double? = null,
    var finishedLongitude: Double? = null,
    var finishedSteps: Int? = 0,
    var finishedDistance: Int? = 0,
    var finishedFloorAsc: Int? = 0,
    var finishedFloorDesc: Int? = 0,
    var createdAt: Date? = null,
    var owner: Boolean = false
): Parcelable {
    val isStarted: Boolean get() = (startedAt != null)
    val isFinished: Boolean get() = (finishedAt != null)
}