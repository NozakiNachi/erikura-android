package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class IdentifyComparingData(
    var firstName: String? = null,
    var lastName: String? = null,
    var dateOfBirth: Date? = null,
    var postcode: String? = null,
    var prefecture: String? = null,
    var city: String? = null,
    var street: String? = null
    ) : Parcelable