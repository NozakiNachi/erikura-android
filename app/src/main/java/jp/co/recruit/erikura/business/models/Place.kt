package jp.co.recruit.erikura.business.models

import com.google.gson.annotations.SerializedName

enum class PlaceJobType(val value: String) {
    @SerializedName("active")
    ACTIVE("active"),
    @SerializedName("future")
    FUTURE("future"),
    @SerializedName("past")
    PAST("past")
}

data class Place(
    var id: Int,
    var workingBuilding: String?,
    var workingPlace: String?,
    var workingPlaceShort: String?,
    var latitude: Double?,
    var longitude: Double?,
    var thumbnailUrl: String?,
    var hasEntries: Boolean = false,
    var jobs: Map<PlaceJobType, List<Job>>
)
