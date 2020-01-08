package jp.co.recruit.erikura.business.models

import java.util.*

data class Entry(
    var id: Int,
    var userId: Int,
    var jobId: Int,
    var comment: Int,
    var limitAt: Date,
    var startedAt: Date?,
    var startedLatitude: Double?,
    var startedLongitude: Double?,
    var startedSteps: Int?,
    var startedDistance: Int?,
    var startedFloorAsc: Int?,
    var startedFloorDesc: Int?,
    var finishedAt: Date?,
    var finishedLatitude: Double?,
    var finishedLongitude: Double?,
    var finishedSteps: Int?,
    var finishedDistance: Int?,
    var finishedFloorAsc: Int?,
    var finishedFloorDesc: Int?,
    var createdAt: Date,
    var owner: Boolean = false
) {
    val isStarted: Boolean get() = (startedAt != null)
    val isFinished: Boolean get() = (finishedAt != null)
}