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
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.*
import jp.co.recruit.erikura.presenters.activities.errors.LoginRequiredActivity
import java.util.*
import java.util.regex.Pattern


class ChangeUserInformationActivity : AppCompatActivity(), ChangeUserInformationEventHandlers {

    var user: User = User()

    private val viewModel: ChangeUserInformationViewModel by lazy {
        ViewModelProvider(this).get(ChangeUserInformationViewModel::class.java)
    }

    // 都道府県のリスト
    val prefectureList = ErikuraApplication.instance.resources.obtainTypedArray(R.array.prefecture_list)
    // 職業のリスト
    val jobStatusIdList = ErikuraApplication.instance.resources.obtainTypedArray(R.array.job_status_id_list)

    // 生年月日入力のカレンダー設定
    val calender: Calendar = Calendar.getInstance()
    var date: DatePickerDialog.OnDateSetListener =
        DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            calender.set(Calendar.YEAR, year)
            calender.set(Calendar.MONTH, monthOfYear)
            calender.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            viewModel.dateOfBirth.value =
                String.format("%d/%02d/%02d", year, monthOfYear + 1, dayOfMonth)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_user_information)

        val binding: ActivityChangeUserInformationBinding = DataBindingUtil.setContentView(this, R.layout.activity_change_user_information)
        binding.lifecycleOwner = this
        binding.handlers = this
        binding.viewModel = viewModel

        // エラーメッセージ
        viewModel.postalCodeErrorVisibility.value = 8
        viewModel.cityErrorVisibility.value = 8
        viewModel.streetErrorVisibility.value = 8
        viewModel.lastNameErrorVisibility.value = 8
        viewModel.firstNameErrorVisibility.value = 8
        viewModel.passwordErrorVisibility.value = 8
        viewModel.verificationPasswordErrorVisibility.value = 8

        // 生年月日入力のカレンダー設定
        calender.set(Calendar.YEAR, 1980)
        calender.set(Calendar.MONTH, 1 - 1)
        calender.set(Calendar.DAY_OF_MONTH, 1)
        viewModel.dateOfBirth.value = String.format("%d/%02d/%02d", 1980, 1, 1)

        // 変更するユーザーの現在の登録値を取得
        Api(this).user(){
            user = it
            viewModel.email.value = user.email
            viewModel.lastName.value = user.lastName
            viewModel.firstName.value = user.firstName
            viewModel.dateOfBirth.value = user.dateOfBirth
            viewModel.gender.value = user.gender?.value
            viewModel.postalCode.value = user.postcode
            viewModel.city.value = user.city
            viewModel.street.value = user.street
            viewModel.phone.value = user.phoneNumber
            viewModel.wishWalk.value = user.wishWorks.size

            // FIXME: 数回に1回初期値がプルダウンに表示されない不具合あり。
            // 都道府県のプルダウン初期表示
            viewModel.prefectureId.value = getPrefectureId(user.prefecture ?: "")
            // 職業のプルダウン初期表示
            viewModel.jobStatusId.value = getJobStatusId(user.jobStatus ?: "")

            // やりたい仕事のチェックボタン初期表示
            val wishWorkList: MutableList<String> = mutableListOf()
            wishWorkList.add(user.wishWorks.getOrNull(0).toString())
            wishWorkList.add(user.wishWorks.getOrNull(1).toString())
            wishWorkList.add(user.wishWorks.getOrNull(2).toString())
            wishWorkList.add(user.wishWorks.getOrNull(3).toString())
            wishWorkList.add(user.wishWorks.getOrNull(4).toString())

            if(wishWorkList.contains("smart_phone")){
                binding.wishWorkSmartPhone.isChecked = true
            }
            if(wishWorkList.contains("cleaning")) {
                binding.wishWorkCleaning.isChecked = true
            }
            if(wishWorkList.contains("walk")) {
                binding.wishWorkWalk.isChecked = true
            }
            if(wishWorkList.contains("bicycle"))
            {
                binding.wishWorkBicycle.isChecked = true
            }
            if(wishWorkList.contains("car")) {
                binding.wishWorkCar.isChecked = true
            }

            // 性別のラジオボタン初期表示
            if(viewModel.gender.value == "male"){
                binding.maleButton.isChecked = true
            }else{
                binding.femaleButton.isChecked = true
            }
        }

        // 再認証が必要かどうか確認
        recerfitication()
    }

    // 所在地
    override fun onFocusChanged(view: View, hasFocus: Boolean) {
        if(!hasFocus && viewModel.postalCode.value?.length ?: 0 == 7) {
            Api(this).postalCode(viewModel.postalCode.value ?: "") { prefecture, city, street ->
                viewModel.prefectureId.value = getPrefectureId(prefecture ?: "")
                viewModel.city.value = city
                viewModel.street.value = street

                val streetEditText = findViewById<EditText>(R.id.registerAddress_street)
                streetEditText.requestFocus()
            }
        }
    }
    private fun getPrefectureId(prefecture: String): Int {
        for (i in 0..47) {
            if(prefectureList.getString(i).equals(prefecture)) {
                return i
            }
        }
        return 0
    }

    // 生年月日
    override fun onClickEditView(view: View) {
        Log.v("EditView", "EditTextTapped!")
        val dpd = DatePickerDialog(
            this@ChangeUserInformationActivity, date, calender
                .get(Calendar.YEAR), calender.get(Calendar.MONTH),
            calender.get(Calendar.DAY_OF_MONTH)
        )

        val dp = dpd.datePicker
        val maxDate: Calendar = Calendar.getInstance()
        maxDate.add(Calendar.YEAR, -18)
        dp.maxDate = maxDate.timeInMillis

        dpd.show()
    }

    // 性別
    override fun onClickMale(view: View) {
        user.gender = Gender.MALE
    }
    override fun onClickFemale(view: View) {
        user.gender = Gender.FEMALE
    }

    // 職業
    private fun getJobStatusId(jobStatus: String): Int {
        for (i in 0..8) {
            if(jobStatusIdList.getString(i).equals(jobStatus)) {
                return i
            }
        }
        return 0
    }

    override fun onClickRegister(view: View) {
        // パスワード
        user.password = viewModel.password.value
        // 氏名
        user.lastName = viewModel.lastName.value
        user.firstName = viewModel.firstName.value
        // 生年月日
        user.dateOfBirth = viewModel.dateOfBirth.value
        // 所在地
        user.postcode = viewModel.postalCode.value
        user.prefecture = prefectureList.getString(viewModel.prefectureId.value ?: 0)
        user.city = viewModel.city.value
        user.street = viewModel.street.value
        // 電話番号
        user.phoneNumber = viewModel.phone.value
        // 職業
        user.jobStatus = jobStatusIdList.getString(viewModel.jobStatusId.value ?: 0)
        // やりたいこと
        val wishWorks: MutableList<String> = mutableListOf()
        if(viewModel.interestedSmartPhone.value ?: false){ wishWorks.add("smart_phone") }
        if(viewModel.interestedCleaning.value ?: false){ wishWorks.add("cleaning") }
        if(viewModel.interestedWalk.value ?: false){ wishWorks.add("walk") }
        if(viewModel.interestedBicycle.value ?: false){ wishWorks.add("bicycle") }
        if(viewModel.interestedCar.value ?: false){ wishWorks.add("car") }
        user.wishWorks = wishWorks

        // 会員情報変更Apiの呼び出し
        Api(this).updateUser(user) {
            val intent = Intent(this, ConfigurationActivity::class.java)
            intent.putExtra("onClickChangeUserInformationFragment", true)
            startActivity(intent)
            finish()
        }
    }

    // 再認証画面へ遷移
    override fun recerfitication(){
        val now = Date()
        val reSignDate = Api.userSession?.resignInExpiredAt

        if(Api.userSession?.resignInExpiredAt != null){
            val diff = now.time - (reSignDate?.time ?: 0)
            if (diff >= 0) {
                val diffMinutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
                if (diffMinutes > 10) {
                    // 過去の再認証から10分以上経っていたら再認証画面へ
                    Intent(this, RecertificationActivity::class.java).let {
                        it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        this.startActivity(it)
                    }
                }
            }
        } else {
            // 一度も再認証していなければ、再認証画面へ
            Intent(this, RecertificationActivity::class.java).let {
                it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                this.startActivity(it)
            }
        }
    }
}

class ChangeUserInformationViewModel: ViewModel() {
    // メールアドレス
    val email: MutableLiveData<String> = MutableLiveData()
    // パスワード
    val password: MutableLiveData<String> = MutableLiveData()
    val passwordErrorMsg: MutableLiveData<String> = MutableLiveData()
    val passwordErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    val verificationPassword: MutableLiveData<String> = MutableLiveData()
    val verificationPasswordErrorMsg: MutableLiveData<String> = MutableLiveData()
    val verificationPasswordErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    // 氏名
    val lastName: MutableLiveData<String> = MutableLiveData()
    val lastNameErrorMsg: MutableLiveData<String> = MutableLiveData()
    val lastNameErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    val firstName: MutableLiveData<String> = MutableLiveData()
    val firstNameErrorMsg: MutableLiveData<String> = MutableLiveData()
    val firstNameErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    // 生年月日
    val dateOfBirth: MutableLiveData<String> = MutableLiveData()
    // 性別
    val gender: MutableLiveData<String> = MutableLiveData()
    // 所在地
    val postalCode: MutableLiveData<String> = MutableLiveData()
    val postalCodeErrorMsg: MutableLiveData<String> = MutableLiveData()
    val postalCodeErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    val prefectureId: MutableLiveData<Int> = MutableLiveData()
    val city: MutableLiveData<String> = MutableLiveData()
    val cityErrorMsg: MutableLiveData<String> = MutableLiveData()
    val cityErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    val street: MutableLiveData<String> = MutableLiveData()
    val streetErrorMsg: MutableLiveData<String> = MutableLiveData()
    val streetErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    // 電話番号
    val phone: MutableLiveData<String> = MutableLiveData()
    val phoneErrorMsg: MutableLiveData<String> = MutableLiveData()
    val phoneErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    // 職業
    val jobStatusId: MutableLiveData<Int> = MutableLiveData()
    // やりたい仕事
    val interestedSmartPhone: MutableLiveData<Boolean> = MutableLiveData()
    val interestedCleaning: MutableLiveData<Boolean> = MutableLiveData()
    val interestedWalk: MutableLiveData<Boolean> = MutableLiveData()
    val interestedBicycle: MutableLiveData<Boolean> = MutableLiveData()
    val interestedCar: MutableLiveData<Boolean> = MutableLiveData()
    val wishWalk: MutableLiveData<Int> = MutableLiveData()

    // 登録ボタン押下
    val isChangeButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(password) { result.value = isValid() }
        result.addSource(verificationPassword) { result.value = isValid() }
        result.addSource(lastName) { result.value = isValid() }
        result.addSource(firstName) { result.value = isValid() }
        result.addSource(dateOfBirth) { result.value = isValid() }
        result.addSource(gender) { result.value = isValid() }
        result.addSource(postalCode) { result.value = isValid() }
        result.addSource(prefectureId) { result.value = isValid() }
        result.addSource(city) { result.value = isValid() }
        result.addSource(street) { result.value = isValid() }
        result.addSource(phone) { result.value = isValid() }
        result.addSource(jobStatusId) { result.value = isValid() }
        result.addSource(wishWalk) { result.value = isValid() }
        result.addSource(interestedSmartPhone) {result.value = isValid()}
        result.addSource(interestedCleaning) {result.value = isValid()}
        result.addSource(interestedWalk) {result.value = isValid()}
        result.addSource(interestedBicycle) {result.value = isValid()}
        result.addSource(interestedCar) {result.value = isValid()}
    }

    //     バリデーションルール
    private fun isValid(): Boolean {
        var valid = true
        valid = isValidPassword() && valid
        valid = isValidVerificationPassword() && valid
        valid = isValidFirstName() && valid
        valid = isValidLastName() && valid
        valid = isValidPostalCode() && valid
        valid = isValidPrefecture() && valid
        valid = isValidCity() && valid
        valid = isValidStreet() && valid
        valid = isValidPhoneNumber() && valid
        valid = isValidJobStatus() && valid
        valid = isValidWishWorks() && valid
        return valid
    }

    // FIXME: 足りないバリデーションルールがないか確認
    private fun isValidLastName(): Boolean {
        var valid = true

        if (valid && lastName.value?.isBlank() ?:true) {
            valid = false
            lastNameErrorMsg.value = ""
            lastNameErrorVisibility.value = 8
        } else if (valid && !(lastName.value?.length ?: 0 <= 30)) {
            valid = false
            lastNameErrorMsg.value = ErikuraApplication.instance.getString(R.string.last_name_count_error)
            lastNameErrorVisibility.value = 0
        } else {
            valid = true
            lastNameErrorMsg.value = ""
            lastNameErrorVisibility.value = 8
        }

        return valid
    }

    private fun isValidFirstName(): Boolean {
        var valid = true

        if (valid && firstName.value?.isBlank() ?:true) {
            valid = false
            firstNameErrorMsg.value = ""
            firstNameErrorVisibility.value = 8
        } else if (valid && !(firstName.value?.length ?: 0 <= 30)) {
            valid = false
            firstNameErrorMsg.value = ErikuraApplication.instance.getString(R.string.first_name_count_error)
            firstNameErrorVisibility.value = 0
        } else {
            valid = true
            firstNameErrorMsg.value = ""
            firstNameErrorVisibility.value = 8
        }

        return valid
    }

    private fun isValidPostalCode(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([0-9])")

        if (valid && postalCode.value?.isBlank() ?: true) {
            valid = false
            postalCodeErrorMsg.value = ""
            postalCodeErrorVisibility.value = 8
        } else if (valid && !(pattern.matcher(postalCode.value).find())) {
            valid = false
            postalCodeErrorMsg.value = ErikuraApplication.instance.getString(R.string.postal_code_pattern_error)
            postalCodeErrorVisibility.value = 0
        } else if (valid && !(postalCode.value?.length ?: 0 == 7)) {
            valid = false
            postalCodeErrorMsg.value = ErikuraApplication.instance.getString(R.string.postal_code_count_error)
            postalCodeErrorVisibility.value = 0
        } else {
            valid = true
            postalCodeErrorMsg.value = ""
            postalCodeErrorVisibility.value = 8

        }
        return valid
    }

    // FIXME: プルダウン初期値取得の不具合が解決したら動作確認
    private fun isValidPrefecture(): Boolean {
        return !(prefectureId.value == 0 || prefectureId.value == null)
    }

    // FIXME: プルダウン初期値取得の不具合が解決したら動作確認
    private fun isValidJobStatus() : Boolean {
        return !(jobStatusId.value == 0 || jobStatusId.value == null)
    }

    private fun isValidCity(): Boolean {
        var valid = true

        if (valid && city.value?.isBlank() ?: true) {
            valid = false
            cityErrorMsg.value = ""
            cityErrorVisibility.value = 8
        } else if (valid && !(city.value?.length ?: 0 <= 20)) {
            valid = false
            cityErrorMsg.value = ErikuraApplication.instance.getString(R.string.city_count_error)
            cityErrorVisibility.value = 0
        } else {
            valid = true
            cityErrorMsg.value = ""
            cityErrorVisibility.value = 8
        }

        return valid
    }

    private fun isValidStreet(): Boolean {
        var valid = true

        if (valid && street.value?.isBlank() ?: true) {
            valid = false
            streetErrorMsg.value = ""
            streetErrorVisibility.value = 8
        } else if (valid && !(street.value?.length ?: 0 <= 100)) {
            valid = false
            streetErrorMsg.value = ErikuraApplication.instance.getString(R.string.street_count_error)
            streetErrorVisibility.value = 0
        } else {
            valid = true
            streetErrorMsg.value = ""
            streetErrorVisibility.value = 8
        }

        return valid
    }

    private fun isValidPassword(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([a-zA-Z0-9]{6,})\$")
        val alPattern = Pattern.compile("^(.*[A-z]+.*)")
        val numPattern = Pattern.compile("^(.*[0-9]+.*)")

        if(valid && password.value !== null && !(pattern.matcher(password.value).find())) {
            valid = false
            passwordErrorMsg.value = ErikuraApplication.instance.getString(R.string.password_count_error)
            passwordErrorVisibility.value = 0
        }else if(valid && password.value !== null && (!(alPattern.matcher(password.value).find()) || !(numPattern.matcher(password.value).find()))) {
            valid = false
            passwordErrorMsg.value = ErikuraApplication.instance.getString(R.string.password_pattern_error)
            passwordErrorVisibility.value = 0
        }else if(valid && password.value == null || password.value == "") {
            valid = true
            verificationPasswordErrorMsg.value = ""
            verificationPasswordErrorVisibility.value = 8
        } else {
            valid = true
            passwordErrorMsg.value = ""
            passwordErrorVisibility.value = 8
        }
        return valid
    }

    private fun isValidVerificationPassword(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([a-zA-Z0-9]{6,})\$")
        val alPattern = Pattern.compile("^(.*[A-z]+.*)")
        val numPattern = Pattern.compile("^(.*[0-9]+.*)")

        if (valid && password.value !== null && verificationPassword.value == null) {
            valid = false
            verificationPasswordErrorMsg.value = ""
            verificationPasswordErrorVisibility.value = 8
        } else if(valid && verificationPassword.value !== null && !(pattern.matcher(password.value).find())) {
            valid = false
            verificationPasswordErrorMsg.value = ErikuraApplication.instance.getString(R.string.password_count_error)
            verificationPasswordErrorVisibility.value = 0
        }else if(valid && verificationPassword.value !== null && (!(alPattern.matcher(verificationPassword.value).find()) || !(numPattern.matcher(verificationPassword.value).find()))) {
            valid = false
            verificationPasswordErrorMsg.value = ErikuraApplication.instance.getString(R.string.password_pattern_error)
            verificationPasswordErrorVisibility.value = 0
        }else if(valid && password.value !== null && verificationPassword.value !== null && !(password.value.equals(verificationPassword.value))) {
            valid = false
            verificationPasswordErrorMsg.value = ErikuraApplication.instance.getString(R.string.password_verificationPassword_match_error)
            verificationPasswordErrorVisibility.value = 0
        }else if(valid && verificationPassword.value == null || verificationPassword.value == "" ) {
            valid = true
            verificationPasswordErrorMsg.value = ""
            verificationPasswordErrorVisibility.value = 8
        } else {
            valid = true
            verificationPasswordErrorMsg.value = ""
            verificationPasswordErrorVisibility.value = 8
        }
        return valid
    }

    private fun isValidWishWorks(): Boolean {
        return interestedSmartPhone.value ?:false || interestedCleaning.value ?:false || interestedWalk.value ?:false || interestedBicycle.value ?:false || interestedCar.value ?:false
    }

    private fun isValidPhoneNumber(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([0-9])")

        if (valid && phone.value?.isBlank() ?:true) {
            valid = false
            phoneErrorMsg.value = ""
            phoneErrorVisibility.value = 8
        }else if(valid && !(pattern.matcher(phone.value).find())) {
            valid = false
            phoneErrorMsg.value = ErikuraApplication.instance.getString(R.string.phone_pattern_error)
            phoneErrorVisibility.value = 0
        }else if(valid && !(phone.value?.length ?: 0 == 10 || phone.value?.length ?: 0 == 11)) {
            valid = false
            phoneErrorMsg.value = ErikuraApplication.instance.getString(R.string.phone_count_error)
            phoneErrorVisibility.value = 0
        } else {
            valid = true
            phoneErrorMsg.value = ""
            phoneErrorVisibility.value = 8
        }

        return valid
    }
}

interface ChangeUserInformationEventHandlers {
    fun onFocusChanged(view: View, hasFocus: Boolean)
    fun onClickEditView(view: View)
    fun onClickRegister(view: View)
    fun onClickMale(view: View)
    fun onClickFemale(view: View)
    fun recerfitication()
}