package jp.co.recruit.erikura.business.models

data class User(
    var id: Int?,
    var email: String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var password: String? = null,
    var dateOfBirth: String? = null,
    var gender: Gender? = null,
    var postCode: String? = null,
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
) {
    // FIXME: 都道府県の文字列定数を定義するのか？
    // FIXME: double は問題なくパースできるか？
}
