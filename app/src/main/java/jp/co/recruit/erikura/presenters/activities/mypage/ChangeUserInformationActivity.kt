package jp.co.recruit.erikura.presenters.activities.mypage

import android.app.ActivityOptions
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Gender
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.util.DateUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.network.Api.Companion.userSession
import jp.co.recruit.erikura.databinding.ActivityChangeUserInformationBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern


class ChangeUserInformationActivity : BaseActivity(), ChangeUserInformationEventHandlers {

    var user: User = User()

    private val viewModel: ChangeUserInformationViewModel by lazy {
        ViewModelProvider(this).get(ChangeUserInformationViewModel::class.java)
    }

    // 都道府県のリスト
    val prefectureList =
        ErikuraApplication.instance.resources.obtainTypedArray(R.array.prefecture_list)
    // 職業のリスト
    val jobStatusIdList =
        ErikuraApplication.instance.resources.obtainTypedArray(R.array.job_status_id_list)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        val binding: ActivityChangeUserInformationBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_change_user_information)
        binding.lifecycleOwner = this
        binding.handlers = this
        binding.viewModel = viewModel
    }

    override fun onStart() {
        super.onStart()

        // 再認証が必要かどうか確認
        checkResignIn() { isResignIn ->
            if (isResignIn) {
                // ページ参照のトラッキングの送出
                Tracking.logEvent(event= "view_edit_profile", params= bundleOf())
                Tracking.view(name= "/mypage/users/edit", title= "会員情報変更画面")

                // 変更するユーザーの現在の登録値を取得
                Api(this).user() {
                    user = it
                    loadData()
                }
            } else {
                finish()
                Intent(this, ResignInActivity::class.java).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    intent.putExtra("fromChangeUserInformation", true)
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val view = this.currentFocus
        if (view != null) {
            val constraintLayout = findViewById<ConstraintLayout>(R.id.change_user_information_constraintLayout)
            constraintLayout.requestFocus()

            val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(constraintLayout.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    // 所在地
    override fun onFocusChanged(view: View, hasFocus: Boolean) {
        viewModel.postalCode.observe(this, androidx.lifecycle.Observer {
            if (user.postcode != viewModel.postalCode.value && viewModel.postalCode.value?.length ?: 0 == 7) {
                Api(this).postalCode(viewModel.postalCode.value ?: "") { prefecture, city, street ->
                    user.postcode = viewModel.postalCode.value
                    viewModel.prefectureId.value = getPrefectureId(prefecture ?: "")
                    viewModel.city.value = city
                    viewModel.street.value = street

                    val streetEditText = findViewById<EditText>(R.id.registerAddress_street)
                    streetEditText.requestFocus()
                }
            }
        })
    }

    private fun getPrefectureId(prefecture: String): Int {
        for (i in 0..47) {
            if (prefectureList.getString(i).equals(prefecture)) {
                return i
            }
        }
        return 0
    }

    // 生年月日
    override fun onClickBirthdayEditView(view: View) {
        Log.v("EditView", "EditTextTapped!")

        var onDateSetListener: DatePickerDialog.OnDateSetListener =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                val calendar = Calendar.getInstance()

                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                var birthday = Date(arrayOf(calendar.timeInMillis, view.maxDate).min()!!)

                val sdf = SimpleDateFormat("yyyy/MM/dd")
                viewModel.dateOfBirth.value = sdf.format(birthday)
            }

        val calendar = Calendar.getInstance()
        val dateOfBirth = DateUtils.parseDate(viewModel.dateOfBirth.value, arrayOf("yyyy/MM/dd", "yyyy-MM-dd"))
        calendar.time = dateOfBirth
        val dpd = DatePickerDialog(
            this@ChangeUserInformationActivity, onDateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
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
            if (jobStatusIdList.getString(i).equals(jobStatus)) {
                return i
            }
        }
        return 0
    }

    override fun onClickRegister(view: View) {
        // パスワード
        if(!viewModel.password.value.isNullOrBlank()) {
            // パスワードが設定されている場合のみ、更新するようにします
            user.password = viewModel.password.value
        }
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
        if (viewModel.interestedSmartPhone.value ?: false) {
            wishWorks.add("smart_phone")
        }
        if (viewModel.interestedCleaning.value ?: false) {
            wishWorks.add("cleaning")
        }
        if (viewModel.interestedWalk.value ?: false) {
            wishWorks.add("walk")
        }
        if (viewModel.interestedBicycle.value ?: false) {
            wishWorks.add("bicycle")
        }
        if (viewModel.interestedCar.value ?: false) {
            wishWorks.add("car")
        }
        user.wishWorks = wishWorks

        // 会員情報変更Apiの呼び出し
        Api(this).updateUser(user) {
            val intent = Intent(this, ConfigurationActivity::class.java)
            intent.putExtra("onClickChangeUserInformationFragment", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }

    // 再認証画面へ遷移
    private fun checkResignIn(onComplete: (isResignIn: Boolean) -> Unit) {
        val nowTime = Date()
        val reSignTime = userSession?.resignInExpiredAt

        if (userSession?.resignInExpiredAt !== null) {
            // 過去の再認証から10分以上経っていたら再認証画面へ
            if (reSignTime!! < nowTime) {
                onComplete(false)
            } else {
                onComplete(true)
            }
        } else {
            // 一度も再認証していなければ、再認証画面へ
            onComplete(false)
        }
    }

    // データの読み込み
    private fun loadData() {
        viewModel.email.value = user.email
        viewModel.lastName.value = user.lastName
        viewModel.firstName.value = user.firstName
        viewModel.dateOfBirth.value = user.parsedDateOfBirth?.let {
            SimpleDateFormat("yyyy/MM/dd").format(it)
        }
//        viewModel.gender.value = user.gender?.value
        viewModel.postalCode.value = user.postcode
        viewModel.city.value = user.city
        viewModel.street.value = user.street
        viewModel.phone.value = user.phoneNumber
        viewModel.wishWalk.value = user.wishWorks.size

        // FIXME: 数回に1回初期値がプルダウンに表示されない不具合あり。
        // 都道府県のプルダウン初期表示
        val id = getPrefectureId(user.prefecture ?: "")
        viewModel.prefectureId.value = id
        // 職業のプルダウン初期表示
        viewModel.jobStatusId.value = getJobStatusId(user.jobStatus ?: "")

        // やりたい仕事のチェックボタン初期表示
        viewModel.interestedSmartPhone.value = user.wishWorks.contains("smart_phone")
        viewModel.interestedCleaning.value = user.wishWorks.contains("cleaning")
        viewModel.interestedWalk.value = user.wishWorks.contains("walk")
        viewModel.interestedBicycle.value = user.wishWorks.contains("bicycle")
        viewModel.interestedCar.value = user.wishWorks.contains("car")

        // 性別のラジオボタン初期表示
        when (user.gender) {
            Gender.MALE -> {
                viewModel.male.value = true
            }
            else -> {
                viewModel.female.value = true
            }
        }

    }
}

class ChangeUserInformationViewModel : ViewModel() {
    // メールアドレス
    val email: MutableLiveData<String> = MutableLiveData()
    // パスワード
    val password: MutableLiveData<String> = MutableLiveData()
    val passwordError: ErrorMessageViewModel = ErrorMessageViewModel()
    val verificationPassword: MutableLiveData<String> = MutableLiveData()
    val verificationPasswordError: ErrorMessageViewModel = ErrorMessageViewModel()
    // 氏名
    val lastName: MutableLiveData<String> = MutableLiveData()
    val lastNameError: ErrorMessageViewModel = ErrorMessageViewModel()
    val firstName: MutableLiveData<String> = MutableLiveData()
    val firstNameError: ErrorMessageViewModel = ErrorMessageViewModel()
    // 生年月日
    val dateOfBirth: MutableLiveData<String> = MutableLiveData()
    // 性別
    val gender: MutableLiveData<String> = MutableLiveData()
    val male: MutableLiveData<Boolean> = MutableLiveData()
    val female: MutableLiveData<Boolean> = MutableLiveData()
    // 所在地
    val postalCode: MutableLiveData<String> = MutableLiveData()
    val postalCodeError: ErrorMessageViewModel = ErrorMessageViewModel()
    val prefectureId: MutableLiveData<Int> = MutableLiveData()
    val city: MutableLiveData<String> = MutableLiveData()
    val cityError: ErrorMessageViewModel = ErrorMessageViewModel()
    val street: MutableLiveData<String> = MutableLiveData()
    val streetError: ErrorMessageViewModel = ErrorMessageViewModel()
    // 電話番号
    val phone: MutableLiveData<String> = MutableLiveData()
    val phoneError: ErrorMessageViewModel = ErrorMessageViewModel()
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
        result.addSource(interestedSmartPhone) { result.value = isValid() }
        result.addSource(interestedCleaning) { result.value = isValid() }
        result.addSource(interestedWalk) { result.value = isValid() }
        result.addSource(interestedBicycle) { result.value = isValid() }
        result.addSource(interestedCar) { result.value = isValid() }
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

        if (valid && lastName.value?.isBlank() ?: true) {
            valid = false
            lastNameError.message.value = null
        } else if (valid && !(lastName.value?.length ?: 0 <= 30)) {
            valid = false
            lastNameError.message.value =
                ErikuraApplication.instance.getString(R.string.last_name_count_error)
        } else {
            valid = true
            lastNameError.message.value = null
        }

        return valid
    }

    private fun isValidFirstName(): Boolean {
        var valid = true

        if (valid && firstName.value?.isBlank() ?: true) {
            valid = false
            firstNameError.message.value = null
        } else if (valid && !(firstName.value?.length ?: 0 <= 30)) {
            valid = false
            firstNameError.message.value =
                ErikuraApplication.instance.getString(R.string.first_name_count_error)
        } else {
            valid = true
            firstNameError.message.value = null
        }

        return valid
    }

    private fun isValidPostalCode(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([0-9])")

        if (valid && postalCode.value?.isBlank() ?: true) {
            valid = false
            postalCodeError.message.value = null
        } else if (valid && !(pattern.matcher(postalCode.value).find())) {
            valid = false
            postalCodeError.message.value =
                ErikuraApplication.instance.getString(R.string.postal_code_pattern_error)
        } else if (valid && !(postalCode.value?.length ?: 0 == 7)) {
            valid = false
            postalCodeError.message.value =
                ErikuraApplication.instance.getString(R.string.postal_code_count_error)
        } else {
            valid = true
            postalCodeError.message.value = null
        }
        return valid
    }

    // FIXME: プルダウン初期値取得の不具合が解決したら動作確認
    private fun isValidPrefecture(): Boolean {
        return !(prefectureId.value == 0 || prefectureId.value == null)
    }

    // FIXME: プルダウン初期値取得の不具合が解決したら動作確認
    private fun isValidJobStatus(): Boolean {
        return !(jobStatusId.value == 0 || jobStatusId.value == null)
    }

    private fun isValidCity(): Boolean {
        var valid = true

        if (valid && city.value?.isBlank() ?: true) {
            valid = false
            cityError.message.value = null
        } else if (valid && !(city.value?.length ?: 0 <= 20)) {
            valid = false
            cityError.message.value =
                ErikuraApplication.instance.getString(R.string.city_count_error)
        } else {
            valid = true
            cityError.message.value = null
        }

        return valid
    }

    private fun isValidStreet(): Boolean {
        var valid = true

        if (valid && street.value?.isBlank() ?: true) {
            valid = false
            streetError.message.value = null
        } else if (valid && !(street.value?.length ?: 0 <= 100)) {
            valid = false
            streetError.message.value =
                ErikuraApplication.instance.getString(R.string.street_count_error)
        } else {
            valid = true
            streetError.message.value = null
        }

        return valid
    }

    private fun isValidPassword(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([a-zA-Z0-9]{6,})\$")
        val alPattern = Pattern.compile("^(.*[A-z]+.*)")
        val numPattern = Pattern.compile("^(.*[0-9]+.*)")

        val hasAlphabet: (str: String) -> Boolean = { str -> alPattern.matcher(str).find() }
        val hasNumeric: (str: String) -> Boolean = { str -> numPattern.matcher(str).find() }

        if(valid && password.value.isNullOrBlank()) {
            passwordError.message.value = null
        }else{
            password.value?.let { pwd ->
                if(valid && !(pattern.matcher(pwd).find())) {
                    valid = false
                    passwordError.message.value = ErikuraApplication.instance.getString(R.string.password_count_error)
                } else if (valid && !(hasAlphabet(pwd) && hasNumeric(pwd))) {
                    valid = false
                    passwordError.message.value = ErikuraApplication.instance.getString(R.string.password_pattern_error)
                } else {
                    valid = true
                    passwordError.message.value = null
                }
            }
        }
        return valid
    }

    private fun isValidVerificationPassword(): Boolean {
        var valid = true

        if(valid && password.value.isNullOrBlank() && verificationPassword.value.isNullOrBlank()) {
            verificationPasswordError.message.value = null
        } else {
            if (valid && !(password.value.equals(verificationPassword.value))) {
                valid = false
                verificationPasswordError.message.value = ErikuraApplication.instance.getString(R.string.password_verificationPassword_match_error)
            } else {
                valid = true
                verificationPasswordError.message.value = null
            }
        }
        return valid
    }

    private fun isValidWishWorks(): Boolean {
        return interestedSmartPhone.value ?: false || interestedCleaning.value ?: false || interestedWalk.value ?: false || interestedBicycle.value ?: false || interestedCar.value ?: false
    }

    private fun isValidPhoneNumber(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([0-9])")

        if (valid && phone.value?.isBlank() ?: true) {
            valid = false
            phoneError.message.value = null
        } else if (valid && !(pattern.matcher(phone.value).find())) {
            valid = false
            phoneError.message.value =
                ErikuraApplication.instance.getString(R.string.phone_pattern_error)
        } else if (valid && !(phone.value?.length ?: 0 == 10 || phone.value?.length ?: 0 == 11)) {
            valid = false
            phoneError.message.value =
                ErikuraApplication.instance.getString(R.string.phone_count_error)
        } else {
            valid = true
            phoneError.message.value = null
        }

        return valid
    }
}

interface ChangeUserInformationEventHandlers {
    fun onFocusChanged(view: View, hasFocus: Boolean)
    fun onClickBirthdayEditView(view: View)
    fun onClickRegister(view: View)
    fun onClickMale(view: View)
    fun onClickFemale(view: View)
}