package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OutputSummaryExamplesAttributes(
    var place: String? = null,
    var beforeCleaningPhotoUrl: String? = null,
    var evaluation: String? = null,
    var comment: String? = null
): Parcelable