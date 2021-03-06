package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class IdentifyImageData(
    var front: List<String>? = null,
    var back: List<String>? = null
) : Parcelable