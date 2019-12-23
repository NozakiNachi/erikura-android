package jp.co.recruit.erikura.business.models

data class OutputSummary(
    var id: Int?,
    var place: String?,
    var evaluation: String?,
    var latitude: Double?,
    var longitude: Double?,
    var photoTakedAt: Double?,
    var comment: String?,
    var beforeCleaningPhotoToken: String?,
    var beforeCleaningPhotoUrl: String?,
    var operatorLikes: Boolean = false,
    var operatorComments: List<OperatorComment> = listOf(),
    var willDelete: Boolean = false
) {
    // photoAsset
    // isUploadCompleted
    // validate
}
