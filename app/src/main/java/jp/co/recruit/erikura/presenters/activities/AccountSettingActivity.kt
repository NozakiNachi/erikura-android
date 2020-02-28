package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Gender
import jp.co.recruit.erikura.business.models.Payment
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.*
import java.util.*
import java.util.regex.Pattern


class AccountSettingActivity : AppCompatActivity(), AccountSettingEventHandlers {

    var payment: Payment = Payment()

    private val viewModel: AccountSettingViewModel by lazy {
        ViewModelProvider(this).get(AccountSettingViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_setting)

        val binding: ActivityAccountSettingBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_account_setting)
        binding.lifecycleOwner = this
        binding.handlers = this
        binding.viewModel = viewModel

//        // エラーメッセージ
//        viewModel.postalCodeErrorVisibility.value = 8
//        viewModel.cityErrorVisibility.value = 8
//        viewModel.streetErrorVisibility.value = 8
//        viewModel.lastNameErrorVisibility.value = 8
//        viewModel.firstNameErrorVisibility.value = 8
//
        // 変更するユーザーの現在の登録値を取得
        Api(this).payment() {
            payment = it

            viewModel.bankName.value = payment.bankName
            viewModel.bankNumber.value = payment.bankNumber
            viewModel.branchOfficeName.value = payment.branchOfficeName
            viewModel.branchOfficeNumber.value = payment.branchOfficeNumber
            viewModel.accountType.value = payment.accountType
            viewModel.accountNumber.value = payment.accountNumber
            viewModel.accountHolderFamily.value = payment.accountHolderFamily
            viewModel.accountHolder.value = payment.accountHolder
        }

        // 口座タイプのラジオボタン初期表示
        if (viewModel.accountType.value == "normal") {
            binding.normalButton.isChecked = true
        } else if (viewModel.accountType.value == "current") {
            binding.currentButton.isChecked = true
        } else if (viewModel.accountType.value == "savings") {
            binding.savingsButton.isChecked = true
        }
    }

    // FIXME: 口座タイプの値の名称は正しいか
    // 口座タイプ
    override fun onClickNormal(view: View) {
        payment.accountType = "normal"
    }
    override fun onClickCurrent(view: View) {
        payment.accountType = "current"
    }
    override fun onClickSavings(view: View) {
        payment.accountType = "savings"
    }

    override fun onClickRegister(view: View) {
        // 口座情報登録Apiの呼び出し
//        Api(this).payment(payment) {
//            // FIXME: ダイアログの表示時間を調整
//            val dialog = AlertDialog.Builder(this).apply {
//                val binding: DialogChangeUserInformationSuccessBinding = DataBindingUtil.inflate(
//                    LayoutInflater.from(context),
//                    R.layout.dialog_change_user_information_success,
//                    null,
//                    false
//                )
//                setView(binding.root)
//            }.create()
//            dialog.show()
        //}
//        finish()
    }
}



    class AccountSettingViewModel: ViewModel() {
        // 銀行名
        val bankName: MutableLiveData<String> = MutableLiveData()
        val verificationBankNameErrorVisibility: MutableLiveData<String> = MutableLiveData()
        val verificationBankNameErrorMsg: MutableLiveData<String> = MutableLiveData()

        // 銀行コード
        val bankNumber: MutableLiveData<String> = MutableLiveData()
        val verificationBankNumberErrorVisibility: MutableLiveData<String> = MutableLiveData()
        val verificationBankNumberErrorMsg: MutableLiveData<String> = MutableLiveData()

        // 支店名
        val branchOfficeName: MutableLiveData<String> = MutableLiveData()
        val verificationBranchOfficeNameErrorVisibility: MutableLiveData<String> = MutableLiveData()
        val verificationBranchOfficeNameErrorMsg: MutableLiveData<String> = MutableLiveData()

        // 支店コード
        val branchOfficeNumber: MutableLiveData<String> = MutableLiveData()
        val verificationBranchOfficeNumberErrorVisibility: MutableLiveData<String> = MutableLiveData()
        val verificationBranchOfficeNumberErrorMsg: MutableLiveData<String> = MutableLiveData()

        // 口座番号
        val accountNumber: MutableLiveData<String> = MutableLiveData()
        val verificationAccountNumberErrorVisibility: MutableLiveData<String> = MutableLiveData()
        val verificationAccountNumberErrorMsg: MutableLiveData<String> = MutableLiveData()

        // 口座タイプ
        val accountType: MutableLiveData<String> = MutableLiveData()
        val verificationAccountTypeErrorVisibility: MutableLiveData<String> = MutableLiveData()
        val verificationAccountTypeErrorMsg: MutableLiveData<String> = MutableLiveData()

        // 銀行名
        val accountHolderFamily: MutableLiveData<String> = MutableLiveData()
        val verificationAccountHolderFamilyErrorVisibility: MutableLiveData<String> = MutableLiveData()
        val verificationAccountHolderFamilyErrorMsg: MutableLiveData<String> = MutableLiveData()
        val accountHolder: MutableLiveData<String> = MutableLiveData()
        val verificationAccountHolderErrorVisibility: MutableLiveData<String> = MutableLiveData()
        val verificationAccountHolderErrorMsg: MutableLiveData<String> = MutableLiveData()

        // 登録ボタン押下
        val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
            //        result.addSource(lastName) { result.value = isValid() }
//        result.addSource(firstName) { result.value = isValid() }
//        result.addSource(dateOfBirth) { result.value = isValid() }
//        result.addSource(gender) { result.value = isValid() }
//        result.addSource(phone) { result.value = isValid() }
//        result.addSource(jobStatusId) { result.value = isValid() }
//        // result.addSource(WishWorks) { result.value = isValid() }
//        result.addSource(postalCode) { result.value = isValid() }
//        result.addSource(prefectureId) { result.value = isValid() }
//        result.addSource(city) { result.value = isValid() }
//        result.addSource(street) { result.value = isValid() }
        }

//    // バリデーションルール
//    private fun isValid(): Boolean {
//        var valid = true
//        valid = isValidPostalCode() && valid
//        valid = isValidPrefecture() && valid
//        valid = isValidCity() && valid
//        valid = isValidStreet() && valid
//        valid = isValidFirstName() && valid
//        valid = isValidLastName() && valid
//        valid = isValidPassword() && valid
//        valid = isValidPhoneNumber() && valid
//
//        return valid
//    }

//    private fun isValidLastName(): Boolean {
//        var valid = true
//
//        if (valid && lastName.value?.isBlank() ?:true) {
//            valid = false
//            lastNameErrorMsg.value = ""
//            lastNameErrorVisibility.value = 8
//        } else if (valid && !(lastName.value?.length ?: 0 <= 30)) {
//            valid = false
//            lastNameErrorMsg.value = ErikuraApplication.instance.getString(R.string.last_name_count_error)
//            lastNameErrorVisibility.value = 0
//        } else {
//            valid = true
//            lastNameErrorMsg.value = ""
//            lastNameErrorVisibility.value = 8
//        }
//
//        return valid
//    }
//
//    private fun isValidFirstName(): Boolean {
//        var valid = true
//
//        if (valid && firstName.value?.isBlank() ?:true) {
//            valid = false
//            firstNameErrorMsg.value = ""
//            firstNameErrorVisibility.value = 8
//        } else if (valid && !(firstName.value?.length ?: 0 <= 30)) {
//            valid = false
//            firstNameErrorMsg.value = ErikuraApplication.instance.getString(R.string.first_name_count_error)
//            firstNameErrorVisibility.value = 0
//        } else {
//            valid = true
//            firstNameErrorMsg.value = ""
//            firstNameErrorVisibility.value = 8
//        }
//
//        return valid
//    }
//
//    private fun isValidPostalCode(): Boolean {
//        var valid = true
//        val pattern = Pattern.compile("^([0-9])")
//
//        if (valid && postalCode.value?.isBlank() ?: true) {
//            valid = false
//            postalCodeErrorMsg.value = ""
//            postalCodeErrorVisibility.value = 8
//        } else if (valid && !(pattern.matcher(postalCode.value).find())) {
//            valid = false
//            postalCodeErrorMsg.value = ErikuraApplication.instance.getString(R.string.postal_code_pattern_error)
//            postalCodeErrorVisibility.value = 0
//        } else if (valid && !(postalCode.value?.length ?: 0 == 7)) {
//            valid = false
//            postalCodeErrorMsg.value = ErikuraApplication.instance.getString(R.string.postal_code_count_error)
//            postalCodeErrorVisibility.value = 0
//        } else {
//            valid = true
//            postalCodeErrorMsg.value = ""
//            postalCodeErrorVisibility.value = 8
//
//        }
//        return valid
//    }
//
//    private fun isValidPrefecture(): Boolean {
//        return !(prefectureId.value == 0)
//    }
//
//    private fun isValidCity(): Boolean {
//        var valid = true
//
//        if (valid && city.value?.isBlank() ?: true) {
//            valid = false
//            cityErrorMsg.value = ""
//            cityErrorVisibility.value = 8
//        } else if (valid && !(city.value?.length ?: 0 <= 20)) {
//            valid = false
//            cityErrorMsg.value = ErikuraApplication.instance.getString(R.string.city_count_error)
//            cityErrorVisibility.value = 0
//        } else {
//            valid = true
//            cityErrorMsg.value = ""
//            cityErrorVisibility.value = 8
//        }
//
//        return valid
//    }
//
//    private fun isValidStreet(): Boolean {
//        var valid = true
//
//        if (valid && street.value?.isBlank() ?: true) {
//            valid = false
//            streetErrorMsg.value = ""
//            streetErrorVisibility.value = 8
//        } else if (valid && !(street.value?.length ?: 0 <= 100)) {
//            valid = false
//            streetErrorMsg.value = ErikuraApplication.instance.getString(R.string.street_count_error)
//            streetErrorVisibility.value = 0
//        } else {
//            valid = true
//            streetErrorMsg.value = ""
//            streetErrorVisibility.value = 8
//        }
//
//        return valid
//    }
//
//    private fun isValidPassword(): Boolean {
//        var valid = true
//        val pattern = Pattern.compile("^([a-zA-Z0-9]{6,})\$")
//        val alPattern = Pattern.compile("^(.*[A-z]+.*)")
//        val numPattern = Pattern.compile("^(.*[0-9]+.*)")
//
//        if (valid && password.value?.isBlank() ?:true) {
//            valid = false
//            passwordErrorMsg.value = ""
//            passwordErrorVisibility.value = 8
//        }else if(valid && !(pattern.matcher(password.value).find())) {
//            valid = false
//            passwordErrorMsg.value = ErikuraApplication.instance.getString(R.string.password_count_error)
//            passwordErrorVisibility.value = 0
//        }else if(valid && (!(alPattern.matcher(password.value).find()) || !(numPattern.matcher(password.value).find()))) {
//            valid = false
//            passwordErrorMsg.value = ErikuraApplication.instance.getString(R.string.password_pattern_error)
//            passwordErrorVisibility.value = 0
//        }else if(valid && password.value !== verificationPassword.value) {
//            passwordErrorMsg.value = ErikuraApplication.instance.getString(R.string.password_verificationPassword_match_error)
//            passwordErrorVisibility.value = 0
//        } else {
//            valid = true
//            passwordErrorMsg.value = ""
//            passwordErrorVisibility.value = 8
//        }
//        return valid
//    }
//
//    private fun isValidPhoneNumber(): Boolean {
//        var valid = true
//        val pattern = Pattern.compile("^([0-9])")
//
//        if (valid && phone.value?.isBlank() ?:true) {
//            valid = false
//            phoneErrorMsg.value = ""
//            phoneErrorVisibility.value = 8
//        }else if(valid && !(pattern.matcher(phone.value).find())) {
//            valid = false
//            phoneErrorMsg.value = ErikuraApplication.instance.getString(R.string.phone_pattern_error)
//            phoneErrorVisibility.value = 0
//        }else if(valid && !(phone.value?.length ?: 0 == 10 || phone.value?.length ?: 0 == 11)) {
//            valid = false
//            phoneErrorMsg.value = ErikuraApplication.instance.getString(R.string.phone_count_error)
//            phoneErrorVisibility.value = 0
//        } else {
//            valid = true
//            phoneErrorMsg.value = ""
//            phoneErrorVisibility.value = 8
//        }
//
//        return valid
//    }
    }

    interface AccountSettingEventHandlers {
        fun onClickNormal(view: View)
        fun onClickCurrent(view: View)
        fun onClickSavings(view: View)
        fun onClickRegister(view: View)
    }