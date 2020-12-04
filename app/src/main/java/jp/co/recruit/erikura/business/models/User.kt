package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.util.DateUtils
import kotlinx.android.parcel.Parcelize
import java.text.ParseException
import java.util.*
import java.util.regex.Pattern

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
) : Parcelable {
    companion object {
        val pattern = Pattern.compile("^([a-zA-Z0-9]{6,})\$")
        val alPattern = Pattern.compile("^(.*[A-z]+.*)")
        val numPattern = Pattern.compile("^(.*[0-9]+.*)")
        val hasAlphabet: (str: String) -> Boolean = { str -> alPattern.matcher(str).find() }
        val hasNumeric: (str: String) -> Boolean = { str -> numPattern.matcher(str).find() }

        private fun commonValidPassword(password: String?, callerValid: Boolean): Pair<Boolean, String?> {
            var valid = callerValid
            var passwordErrorMessage: String? = null

            password?.let { pwd ->
                if (valid && !(pattern.matcher(pwd).find())) {
                    valid = false
                    passwordErrorMessage =
                        ErikuraApplication.instance.getString(R.string.password_count_error)
                }
                else if(valid && !(pwd.length <= 128))) {
                    valid = false
                    passwordErrorMessage =
                        ErikuraApplication.instance.getString(R.string.password_count_over_error)
                }
                else if (valid && !(hasAlphabet(pwd) && hasNumeric(pwd))) {
                    valid = false
                    passwordErrorMessage =
                        ErikuraApplication.instance.getString(R.string.password_pattern_error)
                } else {
                    valid = true
                    passwordErrorMessage = null
                }
            }
            return Pair(first = valid, second = passwordErrorMessage)
        }

        // 新規登録パスワードの場合
        fun isValidFirstRegisterPassword(password: String?): Pair<Boolean, String?> {
            // URLが有効か判定する
            var valid = true
            var passwordErrorMessage: String? = null

            if (valid && password?.isBlank() != false) {
                valid = false
                passwordErrorMessage = null
            } else {
                var validAndErrorMessage =
                    commonValidPassword(password, valid)
                valid = validAndErrorMessage.first
                passwordErrorMessage = validAndErrorMessage.second
            }
            return Pair(first = valid, second = passwordErrorMessage)
        }

        // パスワード再設定の場合 会員情報変更の場合　未入力OK
        fun isValidPasswordForChangeUser(password: String?): Pair<Boolean, String?> {
            // URLが有効か判定する
            var valid = true
            var passwordErrorMessage: String? = null

            if (valid && password.isNullOrBlank()) {
                passwordErrorMessage = null
            } else {
                var validAndErrorMessage =
                    commonValidPassword(password, valid)
                valid = validAndErrorMessage.first
                passwordErrorMessage = validAndErrorMessage.second
            }
            return Pair(first = valid, second = passwordErrorMessage)
        }

        // パスワード再設定の場合 パスワード再設定の場合　未入力NG
        fun isValidPasswordForReset(password: String?): Pair<Boolean, String?> {
            // URLが有効か判定する
            var valid = true
            var passwordErrorMessage: String? = null

            if (valid && password.isNullOrBlank()) {
                valid = false
                passwordErrorMessage = null
            } else {
                var validAndErrorMessage =
                    commonValidPassword(password, valid)
                valid = validAndErrorMessage.first
                passwordErrorMessage = validAndErrorMessage.second
            }
            return Pair(first = valid, second = passwordErrorMessage)
        }

        // パスワード再設定確認の場合 会員情報変更の場合　未入力OK
        fun isValidVerificationPasswordForChangeUser(
            password: String?,
            verificationPassword: String?
        ): Pair<Boolean, String?> {
            var valid = true
            var verificationPasswordErrorMessage: String? = null

            if (valid && password.isNullOrBlank() && verificationPassword.isNullOrBlank()) {
                verificationPasswordErrorMessage = null
            } else {
                if (valid && !(password.equals(verificationPassword))) {
                    valid = false
                    verificationPasswordErrorMessage =
                        ErikuraApplication.instance.getString(R.string.password_verificationPassword_match_error)
                } else {
                    valid = true
                    verificationPasswordErrorMessage = null
                }
            }
            return Pair(first = valid, second = verificationPasswordErrorMessage)
        }

        // パスワード再設定確認の場合 パスワード再設定の場合　未入力NG
        fun isValidVerificationPasswordForReset(
            password: String?,
            verificationPassword: String?
        ): Pair<Boolean, String?> {
            var valid = true
            var verificationPasswordErrorMessage: String? = null

            if (valid && password.isNullOrBlank() && verificationPassword.isNullOrBlank()) {
                valid = false
                verificationPasswordErrorMessage = null
            } else {
                if (valid && !(password.equals(verificationPassword))) {
                    valid = false
                    verificationPasswordErrorMessage =
                        ErikuraApplication.instance.getString(R.string.password_verificationPassword_match_error)
                } else {
                    valid = true
                    verificationPasswordErrorMessage = null
                }
            }
            return Pair(first = valid, second = verificationPasswordErrorMessage)
        }
    }

    val parsedDateOfBirth: Date?
        get() {
            try {
                return DateUtils.parseDate(dateOfBirth, arrayOf("yyyy/MM/dd", "yyyy-MM-dd"))
            } catch (e: ParseException) {
                return null
            }
        }
}
