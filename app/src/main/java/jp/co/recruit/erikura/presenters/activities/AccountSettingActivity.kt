package jp.co.recruit.erikura.presenters.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Payment
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.*


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

        // エラーメッセージ
        viewModel.bankNameErrorVisibility.value = 8
        viewModel.bankNumberErrorVisibility.value = 8
        viewModel.branchOfficeNameErrorVisibility.value = 8
        viewModel.branchOfficeNumberErrorVisibility.value = 8
        viewModel.accountNumberErrorVisibility.value = 8
        viewModel.accountHolderErrorVisibility.value = 8
        viewModel.accountHolderFamilyErrorVisibility.value = 8

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

            // 登録・更新を見分けるフラグ
            if(payment.bankName == null){
                viewModel.settingFragment.value = "register"
            }

            // 口座タイプのラジオボタン初期表示
            if (viewModel.accountType.value == "ordinary_account") {
                binding.ordinaryButton.isChecked = true
            } else if (viewModel.accountType.value == "current_account") {
                binding.currentButton.isChecked = true
            } else if (viewModel.accountType.value == "savings") {
                binding.savingsButton.isChecked = true
            }
        }
    }

//    // 銀行名
//    override fun onFocusChanged(view: View, hasFocus: Boolean) {
////        if(!hasFocus && viewModel.bankName.value?.length ?: 0 == 7) {
////            Api(this).postalCode(viewModel.postalCode.value ?: "") { prefecture, city, street ->
////                viewModel.prefectureId.value = getPrefectureId(prefecture ?: "")
////                viewModel.city.value = city
////                viewModel.street.value = street
////
////                val streetEditText = findViewById<EditText>(R.id.registerAddress_street)
////                streetEditText.requestFocus()
////            }
////        }
//        if(!hasFocus && viewModel.bankName.value?.length ?: 0 == 3) {
//            Api(this).bank(viewModel.bankNumber.value ?: "") { ->
////                val streetEditText = findViewById<EditText>(R.id.registerAddress_street)
////                streetEditText.requestFocus()
//            }
//        }
//    }

    // 口座種別
    override fun onClickOrdinary(view: View) {
        payment.accountType = "ordinary_account"
    }

    override fun onClickCurrent(view: View) {
        payment.accountType = "current_account"
    }

    override fun onClickSavings(view: View) {
        payment.accountType = "savings"
    }

    // FIXME: HolderとFamilyの区別
    override fun onClickSetting(view: View) {
        payment.bankName = viewModel.bankName.value
        payment.bankNumber = viewModel.bankNumber.value
        payment.branchOfficeName = viewModel.branchOfficeName.value
        payment.branchOfficeNumber = viewModel.branchOfficeNumber.value
        payment.accountNumber = viewModel.accountNumber.value
        payment.accountHolder = viewModel.accountHolder.value
        payment.accountHolderFamily = viewModel.accountHolderFamily.value

        // 口座情報登録Apiの呼び出し
        Api(this).updatePayment(payment) {
            if (viewModel.settingFragment.value == "register") {
                val intent = Intent(this, ConfigurationActivity::class.java)
                intent.putExtra("onClickRegisterAccountFragment", true)
                startActivity(intent)
                finish()
            } else {
                if (viewModel.settingFragment.value == null) {
                    val intent = Intent(this, ConfigurationActivity::class.java)
                    intent.putExtra("onClickChangeAccountFragment", true)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}

class AccountSettingViewModel: ViewModel() {
    // 登録か更新かのフラグ
    val settingFragment: MutableLiveData<String> = MutableLiveData()

    // 銀行名
    val bankName: MutableLiveData<String> = MutableLiveData()
    val bankNameErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    val bankNameErrorMsg: MutableLiveData<String> = MutableLiveData()

    // 銀行コード
    val bankNumber: MutableLiveData<String> = MutableLiveData()
    val bankNumberErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    val bankNumberErrorMsg: MutableLiveData<String> = MutableLiveData()

    // 支店名
    val branchOfficeName: MutableLiveData<String> = MutableLiveData()
    val branchOfficeNameErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    val branchOfficeNameErrorMsg: MutableLiveData<String> = MutableLiveData()

    // 支店コード
    val branchOfficeNumber: MutableLiveData<String> = MutableLiveData()
    val branchOfficeNumberErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    val branchOfficeNumberErrorMsg: MutableLiveData<String> = MutableLiveData()

    // 口座番号
    val accountNumber: MutableLiveData<String> = MutableLiveData()
    val accountNumberErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    val accountNumberErrorMsg: MutableLiveData<String> = MutableLiveData()

    // 口座タイプ
    val accountType: MutableLiveData<String> = MutableLiveData()

    // 銀行名
    val accountHolderFamily: MutableLiveData<String> = MutableLiveData()
    val accountHolderFamilyErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    val accountHolderFamilyErrorMsg: MutableLiveData<String> = MutableLiveData()
    val accountHolder: MutableLiveData<String> = MutableLiveData()
    val accountHolderErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    val accountHolderErrorMsg: MutableLiveData<String> = MutableLiveData()

    // 登録ボタン押下
    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(bankName) { result.value = isValid() }
        result.addSource(bankNumber) { result.value = isValid() }
        result.addSource(branchOfficeName) { result.value = isValid() }
        result.addSource(branchOfficeNumber) { result.value = isValid() }
        result.addSource(accountNumber) { result.value = isValid() }
        result.addSource(accountHolder) { result.value = isValid() }
        result.addSource(accountHolderFamily) { result.value = isValid() }
    }
//
    // バリデーションルール
    private fun isValid(): Boolean {
        var valid = true
        valid = isValidBankName() && valid
        valid = isValidBankNumber() && valid
        valid = isValidBranchOfficeName() && valid
        valid = isValidBranchOfficeNumber() && valid
        valid = isValidAccountNumber() && valid
        valid = isValidAccountHolderFamily() && valid
        valid = isValidAccountHolder() && valid

        return valid
    }

    private fun isValidBankName(): Boolean {
        var valid = true
        return valid
    }
    private fun isValidBankNumber(): Boolean {
        var valid = true
        return valid
    }
    private fun isValidBranchOfficeName(): Boolean {
        var valid = true
        return valid
    }
    private fun isValidBranchOfficeNumber(): Boolean {
        var valid = true
        return valid
    }
    private fun isValidAccountNumber(): Boolean {
        var valid = true
        return valid
    }
    private fun isValidAccountHolderFamily(): Boolean {
        var valid = true
        return valid
    }
    private fun isValidAccountHolder(): Boolean {
        var valid = true
        return valid
    }

//
//    private fun isValidBankName(): Boolean {
//        var valid = true
//
//        if (valid && bankName.value?.isBlank() ?:true) {
//            valid = false
//            bankNameErrorMsg.value = ""
//            bankNameErrorVisibility.value = 8
//        } else if (valid && !(bankName.value?.length ?: 0 <= 30)) {
//            valid = false
//            bankNameErrorMsg.value = ErikuraApplication.instance.getString(R.string.last_name_count_error)
//            bankNameErrorVisibility.value = 0
//        } else {
//            valid = true
//            bankNameErrorMsg.value = ""
//            bankNameErrorVisibility.value = 8
//        }
//
//        return valid
//    }
//
//    private fun isValidBankNumber(): Boolean {
//        var valid = true
//        val pattern = Pattern.compile("^([0-9])")
//
//        if (valid && bankName.value?.isBlank() ?:true) {
//            valid = false
//            bankNameErrorMsg.value = ""
//            bankNameErrorVisibility.value = 8
//        }else if(valid && !(pattern.matcher(bankName.value).find())) {
//            valid = false
//            bankNameErrorMsg.value = ErikuraApplication.instance.getString(R.string.phone_pattern_error)
//            bankNameErrorVisibility.value = 0
//        }else if(valid && !(bankName.value?.length ?: 0 == 10 || bankName.value?.length ?: 0 == 11)) {
//            valid = false
//            bankNameErrorMsg.value = ErikuraApplication.instance.getString(R.string.phone_count_error)
//            bankNameErrorVisibility.value = 0
//        } else {
//            valid = true
//            bankNameErrorMsg.value = ""
//            bankNameErrorVisibility.value = 8
//        }
//
//        return valid
//    }

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


}

interface AccountSettingEventHandlers {
    fun onClickOrdinary(view: View)
    fun onClickCurrent(view: View)
    fun onClickSavings(view: View)
    fun onClickSetting(view: View)
//        fun onFocusChanged(view: View, hasFocus: Boolean)
}