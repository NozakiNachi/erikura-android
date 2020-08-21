package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class IdDocument (
    var type: String? = null,
    var format: String = "image",
    var data: Data? = null,
    var comparingData: List<ComparingData> = listOf()
): Parcelable {

}