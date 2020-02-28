package jp.co.recruit.erikura.business.models

data class Payment(
    var bankName: String? = null,
    var bankNumber: String? = null,
    var branchOfficeName: String? = null,
    var branchOfficeNumber: String? = null,
    var accountType: String? = null,
    var accountNumber: String? = null,
    var accountHolderFamily: String? = null,
    var accountHolder: String? = null
) {
    // validate?
}