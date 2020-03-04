package jp.co.recruit.erikura.presenters.activities

import android.content.Intent
import android.os.Bundle
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
import jp.co.recruit.erikura.business.models.Payment
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.*
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
                viewModel.ordinary_button.value = true
            } else if (viewModel.accountType.value == "current_account") {
                viewModel.current_button.value = true
            } else if (viewModel.accountType.value == "savings") {
                viewModel.savings_button.value = true
            }
        }
    }

    // 銀行名フォーカス機能
    override fun onBankNameFocusChanged(view: View, hasFocus: Boolean) {
        if(!hasFocus && viewModel.bankName.value !== null) {
            Api(this).bank(viewModel.bankName.value ?: "") { bankNumber ->
                viewModel.bankNumber.value = bankNumber

                val streetEditText = findViewById<EditText>(R.id.branch_office_name)
                streetEditText.requestFocus()
            }
        }
    }

    // 支店名フォーカス機能
    override fun onBranchOfficeNameFocusChanged(view: View, hasFocus: Boolean) {
        if(!hasFocus && viewModel.branchOfficeName.value !== null) {
            Api(this).branch(viewModel.branchOfficeName.value ?: "",viewModel.bankNumber.value ?: "") { branchOfficeNumber ->
                viewModel.branchOfficeNumber.value = branchOfficeNumber

                val streetEditText = findViewById<EditText>(R.id.account_name)
                streetEditText.requestFocus()
            }
        }
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

        if (viewModel.ordinary_button.value == true) {
            payment.accountType = "ordinary_account"
        }else if(viewModel.current_button.value == true) {
            payment.accountType = "current_account"
        }else if(viewModel.savings_button.value == true) {
            payment.accountType = "savings"
        }

        // 口座情報登録Apiの呼び出し
        Api(this).updatePayment(payment) {
            val intent = Intent(this, ConfigurationActivity::class.java)

            if (viewModel.settingFragment.value == "register") {
                intent.putExtra("onClickRegisterAccountFragment", true)
            } else if(viewModel.settingFragment.value == null) {
                intent.putExtra("onClickChangeAccountFragment", true)
            }
            startActivity(intent)
            finish()
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
    val ordinary_button: MutableLiveData<Boolean> = MutableLiveData()
    val current_button: MutableLiveData<Boolean> = MutableLiveData()
    val savings_button: MutableLiveData<Boolean> = MutableLiveData()

    // 銀行名
    val accountHolderFamily: MutableLiveData<String> = MutableLiveData()
    val accountHolderFamilyErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    val accountHolderFamilyErrorMsg: MutableLiveData<String> = MutableLiveData()
    val accountHolder: MutableLiveData<String> = MutableLiveData()
    val accountHolderErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    val accountHolderErrorMsg: MutableLiveData<String> = MutableLiveData()

    // 登録ボタン押下
    val isSettingButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(bankName) { result.value = isValid() }
        result.addSource(bankNumber) { result.value = isValid() }
        result.addSource(branchOfficeName) { result.value = isValid() }
        result.addSource(branchOfficeNumber) { result.value = isValid() }
        result.addSource(accountNumber) { result.value = isValid() }
        result.addSource(accountType) { result.value = isValid() }
        result.addSource(accountHolder) { result.value = isValid() }
        result.addSource(accountHolderFamily) { result.value = isValid() }
    }

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

        if (valid && bankName.value?.isBlank() ?:true) {
            valid = false
            bankNameErrorMsg.value = ""
            bankNameErrorVisibility.value = 8
        } else if (valid && !(bankName.value?.length ?: 0 <= 6)) {
            valid = false
            bankNameErrorMsg.value = ErikuraApplication.instance.getString(R.string.bank_name_count_error)
            bankNameErrorVisibility.value = 0
        } else {
            valid = true
            bankNameErrorMsg.value = ""
            bankNameErrorVisibility.value = 8
        }

        return valid
    }

    private fun isValidBankNumber(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([0-9])")

        if (valid && bankNumber.value?.isBlank() ?: true) {
            valid = false
            bankNumberErrorMsg.value = ""
            bankNumberErrorVisibility.value = 8
        } else if (valid && !(pattern.matcher(bankNumber.value).find())) {
            valid = false
            bankNumberErrorMsg.value = ErikuraApplication.instance.getString(R.string.bank_number_format_error)
            bankNumberErrorVisibility.value = 0
        } else if (valid && !(bankNumber.value?.length ?: 4 == 4)) {
            valid = false
            bankNumberErrorMsg.value = ErikuraApplication.instance.getString(R.string.bank_number_count_error)
            bankNumberErrorVisibility.value = 0
        } else {
            valid = true
            bankNumberErrorMsg.value = ""
            bankNumberErrorVisibility.value = 8

        }
        return valid
    }


    private fun isValidBranchOfficeName(): Boolean {
        var valid = true

        if (valid && branchOfficeName.value?.isBlank() ?:true) {
            valid = false
            branchOfficeNameErrorMsg.value = ""
            branchOfficeNameErrorVisibility.value = 8
        } else if (valid && !(bankName.value?.length ?: 0 <= 30)) {
            valid = false
            branchOfficeNameErrorMsg.value = ErikuraApplication.instance.getString(R.string.branch_official_name_count_error)
            branchOfficeNameErrorVisibility.value = 0
        } else {
            valid = true
            branchOfficeNameErrorMsg.value = ""
            branchOfficeNameErrorVisibility.value = 8
        }
        return valid
    }


    private fun isValidBranchOfficeNumber(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([0-9])")

        if (valid && bankNumber.value?.isBlank() ?: true) {
            valid = false
            branchOfficeNumberErrorMsg.value = ""
            branchOfficeNumberErrorVisibility.value = 8
        } else if (valid && !(pattern.matcher(bankNumber.value).find())) {
            valid = false
            branchOfficeNumberErrorMsg.value = ErikuraApplication.instance.getString(R.string.branch_official_number_format_error)
            branchOfficeNumberErrorVisibility.value = 0
        } else if (valid && !(branchOfficeNumber.value?.length ?: 3 == 3)) {
            valid = false
            branchOfficeNumberErrorMsg.value = ErikuraApplication.instance.getString(R.string.branch_official_number_count_error)
            branchOfficeNumberErrorVisibility.value = 0
        } else {
            valid = true
            branchOfficeNumberErrorMsg.value = ""
            branchOfficeNumberErrorVisibility.value = 8
        }
        return valid
    }

    private fun isValidAccountNumber(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([0-9])")

        if (valid && branchOfficeNumber.value?.isBlank() ?: true) {
            valid = false
            accountNumberErrorMsg.value = ""
            accountNumberErrorVisibility.value = 8
        } else if (valid && !(pattern.matcher(bankNumber.value).find())) {
            valid = false
            accountNumberErrorMsg.value = ErikuraApplication.instance.getString(R.string.account_number_format_error)
            accountNumberErrorVisibility.value = 0
        } else if (valid && !(accountNumber.value?.length ?: 7 == 7)) {
            valid = false
            accountNumberErrorMsg.value = ErikuraApplication.instance.getString(R.string.account_number_count_error)
            accountNumberErrorVisibility.value = 0
        } else {
            valid = true
            accountNumberErrorMsg.value = ""
            accountNumberErrorVisibility.value = 8
        }
        return valid
    }

    private fun isValidAccountHolderFamily(): Boolean {
        var valid = true
        if (valid && accountHolder.value?.isBlank() ?:true) {
            valid = false
            accountHolderErrorMsg.value = ""
            accountHolderErrorVisibility.value = 8
        } else if (valid && !(accountHolder.value?.length ?: 0 <= 30)) {
            valid = false
            accountHolderErrorMsg.value = ErikuraApplication.instance.getString(R.string.account_holder_count_error)
            accountHolderErrorVisibility.value = 0
        } else {
            valid = true
            accountHolderErrorMsg.value = ""
            accountHolderErrorVisibility.value = 8
        }
        return valid
    }

    private fun isValidAccountHolder(): Boolean {
        var valid = true
        if (valid && accountHolderFamily.value?.isBlank() ?:true) {
            valid = false
            accountHolderFamilyErrorMsg.value = ""
            accountHolderFamilyErrorVisibility.value = 8
        } else if (valid && !(accountHolderFamily.value?.length ?: 0 <= 30)) {
            valid = false
            accountHolderFamilyErrorMsg.value = ErikuraApplication.instance.getString(R.string.account_holder_family_count_error)
            accountHolderFamilyErrorVisibility.value = 0
        } else {
            valid = true
            accountHolderFamilyErrorMsg.value = ""
            accountHolderFamilyErrorVisibility.value = 8
        }
        return valid
    }
}

interface AccountSettingEventHandlers {
    fun onClickSetting(view: View)
    fun onBankNameFocusChanged(view: View, hasFocus: Boolean)
    fun onBranchOfficeNameFocusChanged(view: View, hasFocus: Boolean)
}