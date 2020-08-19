package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


// FIXME 写真データの配列の内容が未確定
@Parcelize
data class IdDocument (
    var type: String? = null,
    var format: String? = null,
//    var data: List<>,
//    var front: List<>,
//    var back: List<>,
    var comparingData: List<ComparingData> = listOf()
): Parcelable {

}