package jp.co.recruit.erikura.business.models

import com.google.gson.annotations.SerializedName

enum class Gender(val value: String) {
    @SerializedName("male")
    MALE("male"),
    @SerializedName("female")
    FEMALE("female")
}
