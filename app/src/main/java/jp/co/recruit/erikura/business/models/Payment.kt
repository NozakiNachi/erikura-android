package jp.co.recruit.erikura.business.models

data class Payment(
    var bankName: String?,
    var bankNumber: String?,
    var branchOfficeName: String?,
    var branchOfficeNumber: String?,
    var accountType: String?,
    var accountNumber: String?,
    var accountHolderFamily: String?,
    var accountHolder: String?
) {
    // validate?
}