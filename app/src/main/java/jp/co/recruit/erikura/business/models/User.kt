package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

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
    // FIXME: 利用箇所の確認
    var confirmationToken: String? = null
): Parcelable {
    // FIXME: 都道府県の文字列定数を定義するのか？
    // FIXME: double は問題なくパースできるか？
}
