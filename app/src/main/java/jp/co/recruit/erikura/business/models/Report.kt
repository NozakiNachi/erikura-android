package jp.co.recruit.erikura.business.models

import java.util.*

data class Report (
    var id: Int? = null,
    var jobId: Int,
    var workingMinute: Int? = null,
    var additionalComment: String? = null,
    var additionalReportPhotoUrl: String? = null,
    var additionalReportPhotoWillDelete: Boolean = false,
    var additionalReportPhotoToken: String?,
    var additionalOperatorLikes: Boolean = false,
    var additionalOPeratorComments: List<OperatorComment> = listOf(),
    var evaluation: String? = null,
    var comment: String? = null,
    var owner: Boolean = false,
    var operatorLikeCount: Int = 0,
    var operatorCommentsCount: Int = 0,
    var acceptedAt: Date? = null,
    var rejectedAt: Date? = null,
    var rejectComment: String? = null,
    var createdAt: Date? = null,
    var outputSummaries: List<OutputSummary> = listOf()
) {
    // photoAsset
    // isUploadCompleted
    // stattus
    // isAccepted
    // isRejected
    // activeOutputSummaryCount
    // activeOUputSummary
    // activeIndexOf
    // validate
}