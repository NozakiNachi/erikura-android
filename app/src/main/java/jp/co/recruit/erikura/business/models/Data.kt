package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Data(
    var front: List<String>,
    var back: List<String>
) : Parcelable