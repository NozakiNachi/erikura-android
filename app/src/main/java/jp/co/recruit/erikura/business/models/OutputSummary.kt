package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import java.util.*

@Parcelize
data class OutputSummary(
    var id: Int? = null,
    var place: String? = null,
    var evaluation: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var photoTakedAt: Date? = null,
    var comment: String? = null,
    var beforeCleaningPhotoToken: String? = null,
    var beforeCleaningPhotoUrl: String? = null,
    var operatorLikes: Boolean = false,
    var operatorComments: List<@RawValue OperatorComment> = listOf(),
    var willDelete: Boolean = false,
    var photoAsset: MediaItem? = null
) : Parcelable {
    // photoAsset
    // isUploadCompleted
    // validate

    val isUploadCompleted: Boolean get() {
        if (photoAsset?.contentUri == null) {
            return true
        }else {
            return !beforeCleaningPhotoToken.isNullOrBlank()
        }
    }

    fun evaluationMap(): String {
        when(evaluation) {
            "異常あり、未対応" -> {
                return "bad"
            }
            "異常あり、対応済み" -> {
                return "ordinary"
            }
            "異常なし" -> {
                return "good"
            }
        }
        return "bad"
    }
}
