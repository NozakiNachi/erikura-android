package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OutputSummaryExample(
    var place: String? = null,
    var beforeCleaningPhotoUrl: String? = null,
    var evaluation: String? = null,
    var comment: String? = null,
    var clientComment: String? = null
): Parcelable