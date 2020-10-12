package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class JobAttachment(
    var id: Int = 0,
    var displayName: String? = null,
    var filename: String? = null,
    var url: String? = null,
    var mimeType: String? = null
): Parcelable {
    val label: String
    get() {
        if (displayName.isNullOrBlank()) {
            return filename ?: ""
        } else {
            return displayName ?: ""
        }
    }
}