package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize


@Parcelize
data class IdDocument (
    var type: String? = null,
    var format: String = "image",
    @SerializedName("data")
    var identifyImageData: IdentifyImageData? = null,
    @SerializedName("comparing_data")
    var identifyComparingData: IdentifyComparingData? = null
): Parcelable {

}