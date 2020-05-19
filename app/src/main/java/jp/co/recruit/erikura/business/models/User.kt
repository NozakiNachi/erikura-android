package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import jp.co.recruit.erikura.business.util.DateUtils
import kotlinx.android.parcel.Parcelize
import java.text.ParseException
import java.util.*

@Parcelize
data class User(
    var id: Int? = 0,
    var email: String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var password: String? = null,
    var dateOfBirth: String? = null,
    var gender: Gender? = null,
    var postcode: String? = null,
    var prefecture: String? = null,
    var city: String? = null,
    var street: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var phoneNumber: String? = null,
    var jobStatus: String? = null,
    var wishWorks: List<String> = listOf(),
    var holdingJobs: Int = 0,
    var maxJobs: Int = 0,
    var confirmationToken: String? = null
): Parcelable {
    val parsedDateOfBirth: Date? get() {
        try {
            return DateUtils.parseDate(dateOfBirth, arrayOf("yyyy/MM/dd", "yyyy-MM-dd"))
        } catch(e: ParseException) {
            return null
        }
    }
}
