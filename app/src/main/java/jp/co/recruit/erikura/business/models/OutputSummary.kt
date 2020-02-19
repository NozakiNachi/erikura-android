package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class OutputSummary(
    var id: Int? = null,
    var place: String? = null,
    var evaluation: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var photoTakedAt: Double? = null,
    var comment: String? = null,
    var beforeCleaningPhotoToken: String? = null,
    var beforeCleaningPhotoUrl: String? = null,
    var operatorLikes: Boolean = false,
    var operatorComments: List<@RawValue OperatorComment> = listOf(),
    var willDelete: Boolean = false,
    var photoAsset: MediaItem? = null
) : Parcelable {
    // photoAsset
    // isUploadCompleted
    // validate
}
