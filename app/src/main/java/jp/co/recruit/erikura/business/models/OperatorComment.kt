package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class OperatorComment(
    var id: Int = 0,
    var body: String = "",
    var createdAt: Date = Date()
) : Parcelable
