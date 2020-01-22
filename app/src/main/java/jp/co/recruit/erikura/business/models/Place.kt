package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

enum class PlaceJobType(val value: String) {
    @SerializedName("active")
    ACTIVE("active"),
    @SerializedName("future")
    FUTURE("future"),
    @SerializedName("past")
    PAST("past")
}

@Parcelize
data class Place(
    var id: Int = 0,
    var workingBuilding: String? = null,
    var workingPlace: String?= null,
    var workingPlaceShort: String?= null,
    var latitude: Double?= null,
    var longitude: Double?= null,
    var thumbnailUrl: String?= null,
    var hasEntries: Boolean = false,
    var jobs: Map<PlaceJobType, List<Job>> = mapOf()
): Parcelable
