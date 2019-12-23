package jp.co.recruit.erikura.business.models

import java.util.*

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
    // isActive
    // isFuture
    // isPastOrInactive
    // isReported
    // isReportCreatable
    // isReportEditable
    // isExpired
    // Status
    // getSummaryTitles
}