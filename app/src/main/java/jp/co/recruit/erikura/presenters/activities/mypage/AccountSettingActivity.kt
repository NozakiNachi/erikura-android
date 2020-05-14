package jp.co.recruit.erikura.presenters.activities.mypage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Bank
import jp.co.recruit.erikura.business.models.BankBranch
import jp.co.recruit.erikura.business.models.Payment
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.network.Api.Companion.userSession
import jp.co.recruit.erikura.databinding.ActivityAccountSettingBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import kotlinx.android.synthetic.main.activity_account_setting.*
import org.apache.commons.lang.StringUtils
import java.util.*
import java.util.regex.Pattern

class AccountSettingActivity : BaseReSignInRequiredActivity(fromActivity = BaseReSignInRequiredActivity.ACTIVITY_ACCOUNT_SETTINGS), AccountSettingEventHandlers {
    val api = Api(this)
    var payment: Payment = Payment()

    private val viewModel: AccountSettingViewModel by lazy {
        ViewModelProvider(this).get(AccountSettingViewModel::class.java)
    }

    override fun onCreateImpl(savedInstanceState: Bundle?) {

        val binding: ActivityAccountSettingBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_account_setting)
        binding.lifecycleOwner = this
        binding.handlers = this
        binding.viewModel = viewModel

        setupBankNameAdapter()
        setupBranchNameAdapter()

        api.payment() {
            payment = it

            viewModel.accountType.value = payment.accountType
            // 口座タイプのラジオボタン初期表示
            if (viewModel.accountType.value == "ordinary_account") {
                binding.ordinaryButton.isChecked = true
            } else if (viewModel.accountType.value == "current_account") {
                binding.currentButton.isChecked = true
            } else if (viewModel.accountType.value == "savings") {
                binding.savingsButton.isChecked = true
            }
        }

        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_edit_bank", params= bundleOf())
        Tracking.view(name= "/mypage/bank/edit", title= "口座情報変更画面")

        // 変更するユーザーの現在の登録値を取得
        Api(this).payment() {
            payment = it
            loadData()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val view = this.currentFocus
        if (view != null) {
            val constraintLayout = findViewById<ConstraintLayout>(R.id.change_account_setting_constraintLayout)
            constraintLayout.requestFocus()

            val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(constraintLayout.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun setupBankNameAdapter() {
        val adapter = BankNameAdapter(this)
        val bankNameField: AutoCompleteTextView = findViewById(R.id.account_setting_bank_name)
        bankNameField.setAdapter(adapter)
        bankNameField.threshold = 1
        bankNameField.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val bank = adapter.getItem(position)
                viewModel.bankNumber.value = bank?.code
                // 次のフィールドにフォーカスを移します
                val nextField: AutoCompleteTextView =
                    findViewById(R.id.account_setting_branch_office_name)
                nextField.requestFocus()
            }
        }
        var prevBankName: String = ""
        viewModel.bankName.observe(this, object : Observer<String> {
            override fun onChanged(t: String?) {
                val bankName = t ?: ""
                if (bankName != prevBankName) {
                    api.cancelAllRequests()
                    api.bank(bankName, showProgress = false) { banks ->
                        adapter.clear()
                        adapter.addAll(banks)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    private fun setupBranchNameAdapter() {
        val adapter = BranchNameAdapter(this)
        val branchNameField: AutoCompleteTextView =
            findViewById(R.id.account_setting_branch_office_name)
        branchNameField.setAdapter(adapter)
        branchNameField.threshold = 1
        branchNameField.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val branch = adapter.getItem(position)
                viewModel.branchOfficeNumber.value = branch?.code
                // 次のフィールドにフォーカスを移します
                val nextField: EditText = findViewById(R.id.account_name)
                nextField.requestFocus()
            }
        }
        var prevBranchName: String = ""
        viewModel.branchOfficeName.observe(this, object : Observer<String> {
            override fun onChanged(t: String?) {
                val branchOfficeName = t ?: ""
                if (branchOfficeName != prevBranchName) {
                    api.cancelAllRequests()
                    api.branch(
                        branchOfficeName,
                        viewModel.bankNumber.value ?: "",
                        showProgress = false
                    ) { branches ->
                        adapter.clear()
                        adapter.addAll(branches)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    override fun onBankNameFocusChanged(view: View, hasFocus: Boolean) {
        if (!hasFocus && StringUtils.isNotBlank(viewModel.bankName.value)) {
            val bankName = viewModel.bankName.value ?: ""
            api.bank(bankName, showProgress = true) { banks ->
                if (banks.isNotEmpty()) {
                    val bank = banks.find { it.name == bankName }
                    bank?.let {
                        if (StringUtils.isBlank(viewModel.bankNumber.value)) {
                            viewModel.bankNumber.value = bank.code
                        }
                    }
                }
                // MEMO: ユーザが意図的にフォーカスを移しているため、このタイミングではフォーカス変更は行いません
            }
        }
    }

    override fun onBranchOfficeNameFocusChanged(view: View, hasFocus: Boolean) {
        if (!hasFocus && StringUtils.isNotBlank(viewModel.branchOfficeName.value)) {
            val branchOfficeName = viewModel.branchOfficeName.value ?: ""
            val bankNumber = viewModel.bankNumber.value ?: ""
            api.branch(branchOfficeName, bankNumber, showProgress = true) { branches ->
                if (branches.isNotEmpty()) {
                    val branch = branches.find { it.name == branchOfficeName }
                    branch?.let {
                        if (StringUtils.isBlank(viewModel.branchOfficeNumber.value)) {
                            viewModel.branchOfficeNumber.value = branch.code
                        }
                    }
                }
                // MEMO: ユーザが意図的にフォーカスを移しているため、このタイミングではフォーカス変更は行いません
            }
        }
    }

    // 口座種別
    override fun onOrdinaryButton(view: View) {
        if (payment.accountType == "ordinary_account") {
            (view as? RadioButton)?.isChecked = false
            this.table.clearCheck()
            viewModel.accountType.value = null
            payment.accountType = null
        }
        else {
            viewModel.accountType.value = "ordinary_account"
            payment.accountType = "ordinary_account"
        }
    }

    override fun onCurrentButton(view: View) {
        if (payment.accountType == "current_account") {
            (view as? RadioButton)?.isChecked = false
            this.table.clearCheck()
            viewModel.accountType.value = null
            payment.accountType = null
        }
        else {
            viewModel.accountType.value = "current_account"
            payment.accountType = "current_account"
        }
    }

    override fun onSavingsButton(view: View) {
        if (payment.accountType == "savings") {
            (view as? RadioButton)?.isChecked = false
            this.table.clearCheck()
            viewModel.accountType.value = null
            payment.accountType = null
        }
        else {
            viewModel.accountType.value = "savings"
            payment.accountType = "savings"
        }
    }

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
            val intent = Intent(this, ConfigurationActivity::class.java)

            if (viewModel.settingFragment.value == "register") {
                intent.putExtra("onClickRegisterAccountFragment", true)
            } else if (viewModel.settingFragment.value == null) {
                intent.putExtra("onClickChangeAccountFragment", true)
            }

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    // データの読み込み
    private fun loadData() {
        viewModel.bankName.value = payment.bankName
        viewModel.bankNumber.value = payment.bankNumber
        viewModel.branchOfficeName.value = payment.branchOfficeName
        viewModel.branchOfficeNumber.value = payment.branchOfficeNumber
        viewModel.accountType.value = payment.accountType
        viewModel.accountNumber.value = payment.accountNumber
        viewModel.accountHolderFamily.value = payment.accountHolderFamily
        viewModel.accountHolder.value = payment.accountHolder

        // 登録・更新を見分けるフラグ
        if (payment.bankName == null) {
            viewModel.settingFragment.value = "register"
        }
    }
}


class AccountSettingViewModel: ViewModel() {
    // 登録か更新かのフラグ
    val settingFragment: MutableLiveData<String> = MutableLiveData()

    // 銀行名
    val bankName: MutableLiveData<String> = MutableLiveData()
    val bankNameError: ErrorMessageViewModel =
        ErrorMessageViewModel()

    // 銀行コード
    val bankNumber: MutableLiveData<String> = MutableLiveData()
    val bankNumberError: ErrorMessageViewModel =
        ErrorMessageViewModel()

    // 支店名
    val branchOfficeName: MutableLiveData<String> = MutableLiveData()
    val branchOfficeNameError: ErrorMessageViewModel =
        ErrorMessageViewModel()

    // 支店コード
    val branchOfficeNumber: MutableLiveData<String> = MutableLiveData()
    val branchOfficeNumberError: ErrorMessageViewModel =
        ErrorMessageViewModel()

    // 口座番号
    val accountNumber: MutableLiveData<String> = MutableLiveData()
    val accountNumberError: ErrorMessageViewModel =
        ErrorMessageViewModel()

    // 口座タイプ
    val accountType: MutableLiveData<String> = MutableLiveData()

    // 銀行名
    val accountHolderFamily: MutableLiveData<String> = MutableLiveData()
    val accountHolderFamilyError: ErrorMessageViewModel =
        ErrorMessageViewModel()
    val accountHolder: MutableLiveData<String> = MutableLiveData()
    val accountHolderError: ErrorMessageViewModel =
        ErrorMessageViewModel()

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
        valid = isValidAccountType() && valid
        valid = isValidAccountHolderFamily() && valid
        valid = isValidAccountHolder() && valid

        return valid
    }

    private fun isValidBankName(): Boolean {
        var valid = true

        if (valid && bankName.value?.isBlank() ?:true) {
            valid = false
            bankNameError.message.value = null
        } else if (valid && !(bankName.value?.length ?: 0 <= 100)) {
            valid = false
            bankNameError.message.value = ErikuraApplication.instance.getString(R.string.bank_name_count_error)
        } else {
            valid = true
            bankNameError.message.value = null
        }

        return valid
    }

    private fun isValidBankNumber(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([0-9]*)$")

        if (valid && bankNumber.value?.isBlank() ?: true) {
            valid = false
            bankNumberError.message.value = null
        } else if (valid && !(pattern.matcher(bankNumber.value).find())) {
            valid = false
            bankNumberError.message.value = ErikuraApplication.instance.getString(R.string.bank_number_format_error)
        } else if (valid && !(bankNumber.value?.length ?: 4 == 4)) {
            valid = false
            bankNumberError.message.value = ErikuraApplication.instance.getString(R.string.bank_number_count_error)
        } else {
            valid = true
            bankNumberError.message.value = null
        }
        return valid
    }

    private fun isValidBranchOfficeName(): Boolean {
        var valid = true

        if (valid && branchOfficeName.value?.isBlank() ?:true) {
            valid = false
            branchOfficeNameError.message.value = null
        } else if (valid && !(bankName.value?.length ?: 0 <= 100)) {
            valid = false
            branchOfficeNameError.message.value = ErikuraApplication.instance.getString(R.string.branch_official_name_count_error)
        } else {
            valid = true
            branchOfficeNameError.message.value = null
        }
        return valid
    }

    private fun isValidBranchOfficeNumber(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([0-9]*)$")

        if (valid && bankNumber.value?.isBlank() ?: true) {
            valid = false
            branchOfficeNumberError.message.value = null
        } else if (valid && !(pattern.matcher(bankNumber.value).find())) {
            valid = false
            branchOfficeNumberError.message.value = ErikuraApplication.instance.getString(R.string.branch_official_number_format_error)
        } else if (valid && !(branchOfficeNumber.value?.length ?: 3 == 3)) {
            valid = false
            branchOfficeNumberError.message.value = ErikuraApplication.instance.getString(R.string.branch_official_number_count_error)
        } else {
            valid = true
            branchOfficeNumberError.message.value = null
        }
        return valid
    }

    private fun isValidAccountNumber(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([0-9]*)$")

        if (valid && accountNumber.value?.isBlank() ?: true) {
            valid = false
            accountNumberError.message.value = null
        } else if (valid && !(pattern.matcher(accountNumber.value).find())) {
            valid = false
            accountNumberError.message.value = ErikuraApplication.instance.getString(R.string.account_number_format_error)
        } else if (valid && !(accountNumber.value?.length ?: 7 == 7)) {
            valid = false
            accountNumberError.message.value = ErikuraApplication.instance.getString(R.string.account_number_count_error)
        } else {
            valid = true
            accountNumberError.message.value = null
        }
        return valid
    }

    private fun isValidAccountType(): Boolean {
        return !(accountType.value.isNullOrBlank() || accountType.value.equals("unanswered"))
    }

    private fun isValidAccountHolder(): Boolean {
        var valid = true

        if (valid && accountHolder.value?.isBlank() ?:true) {
            valid = false
            accountHolderError.message.value = null
        } else if (valid && !(accountHolder.value?.length ?: 0 <= 20)) {
            valid = false
            accountHolderError.message.value = ErikuraApplication.instance.getString(R.string.account_holder_count_error)
        }else if (!(accountHolder.value?.matches("^[\\u30A0-\\u30FF]+$".toRegex()) ?:true )){
            valid = false
            accountHolderError.message.value = ErikuraApplication.instance.getString(R.string.account_holder_format_error)
        } else {
            valid = true
            accountHolderError.message.value = null
        }
        return valid
    }

    private fun isValidAccountHolderFamily(): Boolean {
        var valid = true
        if (valid && accountHolderFamily.value?.isBlank() ?:true) {
            valid = false
            accountHolderFamilyError.message.value = null
        } else if (valid && !(accountHolderFamily.value?.length ?: 0 <= 20)) {
            valid = false
            accountHolderFamilyError.message.value = ErikuraApplication.instance.getString(R.string.account_holder_family_count_error)
        } else if (!(accountHolderFamily.value?.matches("^[\\u30A0-\\u30FF]+$".toRegex()) ?:true )){
            valid = false
            accountHolderFamilyError.message.value = ErikuraApplication.instance.getString(R.string.account_holder_family_format_error)
        } else {
            valid = true
            accountHolderFamilyError.message.value = null
        }
        return valid
    }
}

interface AccountSettingEventHandlers {
    fun onClickSetting(view: View)
    fun onOrdinaryButton(view: View)
    fun onCurrentButton(view: View)
    fun onSavingsButton(view: View)
    fun onBankNameFocusChanged(view: View, hasFocus: Boolean)
    fun onBranchOfficeNameFocusChanged(view: View, hasFocus: Boolean)
}

class ErrorMessageViewModel {
    val message: MutableLiveData<String> = MutableLiveData()
    val visibility = MediatorLiveData<Int>().also { result ->
        result.addSource(message) {
            result.value = if (message.value == null || StringUtils.isBlank(message.value)) {
                View.GONE
            }
            else {
                View.VISIBLE
            }
        }
    }
}

class BankNameAdapter(context: Context): ArrayAdapter<Bank>(context, android.R.layout.simple_dropdown_item_1line, mutableListOf()) {
    val filter =
        BankNameFilter(this)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val convertView = convertView ?: run {
            val inflator = LayoutInflater.from(context)
            inflator.inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
        }

        getItem(position)?.let {
            (convertView as? TextView)?.text = it.name
        }
        return convertView
    }

    override fun getFilter(): Filter {
        return filter
    }

    class BankNameFilter(val adapter: BankNameAdapter): Filter() {
        override fun convertResultToString(resultValue: Any?): CharSequence {
            return (resultValue as? Bank)?.let {
                it.name
            } ?: run {
                super.convertResultToString(resultValue)
            }
        }

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            return FilterResults()
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            if (results != null && results.count > 0) {
                adapter.notifyDataSetChanged()
            } else {
                adapter.notifyDataSetInvalidated()
            }
        }
    }
}

class BranchNameAdapter(context: Context): ArrayAdapter<BankBranch>(context, android.R.layout.simple_dropdown_item_1line, mutableListOf()) {
    val filter = BranchNameFilter(this)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val convertView = convertView ?: run {
            val inflator = LayoutInflater.from(context)
            inflator.inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
        }

        getItem(position)?.let {
            (convertView as? TextView)?.text = it.name
        }
        return convertView
    }

    override fun getFilter(): Filter {
        return filter
    }

    class BranchNameFilter(val adapter: BranchNameAdapter): Filter() {
        override fun convertResultToString(resultValue: Any?): CharSequence {
            return (resultValue as? BankBranch)?.let {
                it.name
            } ?: run {
                super.convertResultToString(resultValue)
            }
        }

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            return FilterResults()
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            if (results != null && results.count > 0) {
                adapter.notifyDataSetChanged()
            } else {
                adapter.notifyDataSetInvalidated()
            }
        }
    }
}
