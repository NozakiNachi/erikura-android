package jp.co.recruit.erikura.presenters.activities.mypage

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
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
import jp.co.recruit.erikura.presenters.activities.job.JobTitleDialogFragment
import jp.co.recruit.erikura.presenters.activities.registration.SmsVerifyActivity
import kotlinx.android.synthetic.main.activity_change_user_information.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class ChangeUserInformationActivity :
    BaseReSignInRequiredActivity(fromActivity = BaseReSignInRequiredActivity.ACTIVITY_CHANGE_USER_INFORMATION),
    ChangeUserInformationEventHandlers {
    var user: User = User()
    var previousPostalCode: String? = null
    var requestCode: Int? = null
    var fromSms: Boolean = false
    var identifyStatus: Int? = null
    var userName: String? = null
    var birthDay: String? = null
    var prefectureName: String? = null
    var cityName: String? = null
    var streetName: String? = null
    var fromWhere: Int? = null

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
        requestCode = intent.getIntExtra("requestCode", ErikuraApplication.REQUEST_DEFAULT_CODE)
        fromSms = intent.getBooleanExtra("fromSms", false)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateImpl(savedInstanceState: Bundle?) {
        val binding: ActivityChangeUserInformationBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_change_user_information)
        binding.lifecycleOwner = this
        binding.handlers = this
        binding.viewModel = viewModel

        // エラーメッセージを受け取る
        val errorMessages = intent.getStringArrayExtra("errorMessages")
        if (errorMessages != null) {
            Api(this).displayErrorAlert(errorMessages.asList())
        }
        requestCode = intent.getIntExtra("requestCode", ErikuraApplication.REQUEST_DEFAULT_CODE)
        fromSms = intent.getBooleanExtra("fromSms", false)
        fromWhere =
            intent.getIntExtra(ErikuraApplication.FROM_WHERE, ErikuraApplication.FROM_NOT_FOUND)

        // 郵便番号が変更された場合に、住所を取り直すように修正します
        viewModel.postalCode.observe(this, androidx.lifecycle.Observer {
            if (viewModel.isValidPostalCode() && previousPostalCode != viewModel.postalCode.value) {
                previousPostalCode = viewModel.postalCode.value
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

        //　スピナーがdisabledのとき文字色を他のdisabledされたフィールドと同色にする
        viewModel.inputIdentityInfoEnabled.observe(this, androidx.lifecycle.Observer{
            if (it == false) {
                val adapter = ArrayAdapter<String>(this@ChangeUserInformationActivity, R.layout.custom_dropdown_disabled_item, ErikuraApplication.instance.resources.getStringArray(R.array.prefecture_list))
                adapter.setDropDownViewResource(R.layout.custom_dropdown_disabled_item)
                registerAddress_prefecture.adapter = adapter
            }
        })

        // ページ参照のトラッキングの送出
        Tracking.logEvent(event = "view_edit_profile", params = bundleOf())
        Tracking.view(name = "/mypage/users/edit", title = "会員情報変更画面")

        // 変更するユーザーの現在の登録値を取得
        val api = Api(this)
        api.user() {
            user = it
            user.id?.let { userId ->
                api.showIdVerifyStatus(
                    userId,
                    ErikuraApplication.GET_COMPARING_DATA
                ) { status, identifyComparingData ->
                    identifyStatus = status
                    val sdf = SimpleDateFormat("yyyy/MM/dd")
                    // 身分確認状況を取得
                    if (identifyStatus == ErikuraApplication.ID_CONFIRMING_CODE || identifyStatus == ErikuraApplication.FAILED_NEVER_APPROVED || identifyStatus == ErikuraApplication.FAILED_ONCE_APPROVED) {
                        userName =
                            identifyComparingData?.lastName + identifyComparingData?.firstName
                        birthDay = sdf.format(identifyComparingData?.dateOfBirth)
                        prefectureName = identifyComparingData?.prefecture
                        cityName = identifyComparingData?.city
                        streetName = identifyComparingData?.street
                    }
                    loadData()
                }
            }
        }
        // 身分証送信経由の場合、完了ダイアログを表示
        if (fromWhere == ErikuraApplication.FROM_CHANGE_USER || fromWhere == ErikuraApplication.FROM_CHANGE_USER_FOR_CHANGE_INFO) {
            val dialog = AlertDialog.Builder(this)
                .setView(R.layout.dialog_uploaded_id_image)
                .setCancelable(true)
                .create()
            dialog.show()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val view = this.currentFocus
        if (view != null) {
            val constraintLayout =
                findViewById<ConstraintLayout>(R.id.change_user_information_constraintLayout)
            constraintLayout.requestFocus()

            val imm: InputMethodManager =
                getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(constraintLayout.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
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
        val dateOfBirth =
            DateUtils.parseDate(viewModel.dateOfBirth.value, arrayOf("yyyy/MM/dd", "yyyy-MM-dd"))
        calendar.time = dateOfBirth
        val dpd = DatePickerDialog(
            this@ChangeUserInformationActivity, onDateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        dpd.setButton(DatePickerDialog.BUTTON_POSITIVE, getString(R.string.button_ok), dpd);
        dpd.setButton(DatePickerDialog.BUTTON_NEGATIVE, getString(R.string.button_cancel), dpd);

        val dp = dpd.datePicker
        val maxDate: Calendar = Calendar.getInstance()
        maxDate.add(Calendar.YEAR, -18)
        dp.maxDate = maxDate.timeInMillis

        dpd.show()
        // 初期状態で年選択を表示した状態にします
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dpd.datePicker.touchables[0].performClick()
        }
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
        if (!viewModel.password.value.isNullOrBlank()) {
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

        //電話番号以外のユーザー情報を変更します。
        Api(this).updateUser(user) {
            // 電話番号はSMS認証後に保存のため別で保持します。
            val newPhoneNumber = viewModel.phone.value
            if (fromSms) {
                Api(this).smsVerifyCheck(newPhoneNumber ?: "") { result ->
                    val isChangedPhoneNumber = user.phoneNumber != newPhoneNumber
                    val finishEditing: () -> Unit = {
                        val intent = Intent()
                        //SMS認証画面経由の場合、元の認証画面へ
                        intent.putExtra(SmsVerifyActivity.NewPhoneNumber, newPhoneNumber)
                        intent.putExtra(
                            SmsVerifyActivity.BeforeChangeNewPhoneNumber,
                            newPhoneNumber
                        )
                        intent.putExtra(SmsVerifyActivity.SmsVerified, result)
                        //電話番号の変更があることを元の認証画面へ
                        if (isChangedPhoneNumber) {
                            intent.putExtra(SmsVerifyActivity.IsChangePhoneNumber, true)
                        }
                        //電話番号以外の会員情報変更したモーダル表示
                        intent.putExtra("onClickChangeUserInformationOtherThanPhone", true)
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                    if (result && isChangedPhoneNumber && this.requestCode == ErikuraApplication.REQUEST_CHANGE_USER_INFORMATION) {
                        // 電話番号が変更されているので、永続化処理をした上で、SMS認証に戻ります
                        user.phoneNumber = newPhoneNumber
                        Api(this).updateUser(user) {
                            finishEditing()
                        }
                    } else {
                        // SMS認証画面に戻ります
                        finishEditing()
                    }
                }
            } else {
                Log.v("DEBUG", "SMS認証チェック： userId=${user.id}")
                if (newPhoneNumber != null) {
                    //電話番号の変更がある場合
                    if (user.phoneNumber != newPhoneNumber) {
                        userSession?.smsVerifyCheck = true
                        Api(this).smsVerifyCheck(newPhoneNumber ?: "") { result ->
                            if (!result) {
                                val intent = Intent(this, SmsVerifyActivity::class.java)
                                intent.putExtra("beforeChangeNewPhoneNumber", newPhoneNumber)
                                intent.putExtra("newPhoneNumber", newPhoneNumber)
                                intent.putExtra("phoneNumber", newPhoneNumber)
                                intent.putExtra("user", user)
                                intent.putExtra(
                                    "requestCode",
                                    ErikuraApplication.REQUEST_CHANGE_USER_INFORMATION
                                )
                                //電話番号以外の会員情報変更したモーダル表示
                                intent.putExtra("onClickChangeUserInformationOtherThanPhone", true)
                                startActivityForResult(
                                    intent,
                                    ErikuraApplication.REQUEST_CHANGE_USER_INFORMATION
                                )
                            } else {
                                // 会員情報変更の場合
                                // 以前にSMS認証済みの番号へ変更する場合があるので会員情報変更Apiの呼び出し
                                user.phoneNumber = newPhoneNumber
                                Api(this).updateUser(user) {
                                    val intent = Intent(this, ConfigurationActivity::class.java)
                                    intent.putExtra("onClickChangeUserInformationFragment", true)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                }
                            }
                        }
                    } else {
                        //電話番号の変更がない場合
                        //スキップして会員情報変更した場合は現在の番号がSMS未認証の場合でも更新し設定画面へ
                        val intent = Intent(this, ConfigurationActivity::class.java)
                        intent.putExtra("onClickChangeUserInformationFragment", true)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                    }
                }
            }
        }
    }

    override fun onClickUpdateIdentity(view: View) {
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event = "push_identity_verification_edit_profile", params = bundleOf())
        Tracking.trackUserId("push_identity_verification_edit_profile", user)
        // 本人確認情報入力画面へ遷移
        val intent = Intent(this, UpdateIdentityActivity::class.java)
        intent.putExtra("user", user)
        // 会員情報から遷移
        intent.putExtra(ErikuraApplication.FROM_WHERE, ErikuraApplication.FROM_CHANGE_USER)
        startActivity(intent)
    }

    override fun onClickUpdateIdentityForChangeInfo(view: View) {
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event = "push_identity_verification_edit_link", params = bundleOf())
        Tracking.trackUserId("push_identity_verification_edit_link", user)

        //本人確認情報入力画面へ遷移
        val intent = Intent(this, UpdateIdentityActivity::class.java)
        intent.putExtra("user", user)
        // 会員情報(変更リンク)から遷移
        intent.putExtra(
            ErikuraApplication.FROM_WHERE,
            ErikuraApplication.FROM_CHANGE_USER_FOR_CHANGE_INFO
        )
        startActivity(intent)
    }

    // データの読み込み
    private fun loadData() {
        viewModel.email.value = user.email
        viewModel.lastName.value = user.lastName
        viewModel.firstName.value = user.firstName
        viewModel.dateOfBirth.value = user.parsedDateOfBirth?.let {
            SimpleDateFormat("yyyy/MM/dd").format(it)
        }

        previousPostalCode = user.postcode
        viewModel.postalCode.value = user.postcode
        viewModel.city.value = user.city
        viewModel.street.value = user.street
        viewModel.phone.value = user.phoneNumber
        viewModel.wishWalk.value = user.wishWorks.size

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

        viewModel.identifyStatus.value = identifyStatus
        viewModel.fromSms.value = fromSms

        //確認中、否認の場合比較データを表示する
        if (identifyStatus == ErikuraApplication.ID_CONFIRMING_CODE) {
            // 確認中の場合表示する氏名、生年月日、住所を取得
            viewModel.confirmingUserName.value =
                fromHtml((getString(R.string.confirming_identification) + userName))
            viewModel.confirmingPrefecture.value =
                fromHtml((getString(R.string.confirming_identification) + prefectureName))
            viewModel.confirmingCityName.value =
                fromHtml((getString(R.string.confirming_identification) + cityName))
            viewModel.confirmingStreet.value =
                fromHtml((getString(R.string.confirming_identification) + streetName))
            viewModel.confirmingBirthDay.value =
                fromHtml((getString(R.string.confirming_identification) + birthDay))
        }
        if (identifyStatus == ErikuraApplication.FAILED_NEVER_APPROVED || identifyStatus == ErikuraApplication.FAILED_ONCE_APPROVED) {
            // 失敗した氏名、生年月日、住所を取得
            viewModel.deniedUserName.value = fromHtml((getString(R.string.denied_identification) + userName))
            viewModel.deniedPrefecture.value =
                fromHtml((getString(R.string.denied_identification) + prefectureName))
            viewModel.deniedCityName.value = fromHtml((getString(R.string.denied_identification) + cityName))
            viewModel.deniedStreet.value = fromHtml((getString(R.string.denied_identification) + streetName))
            viewModel.deniedBirthDay.value = fromHtml((getString(R.string.denied_identification) + birthDay))
        }
    }

    private fun fromHtml(text: String): Spanned {
        return Html.fromHtml("<u>${text}</u>")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var isSkip: Boolean = false
        if (resultCode == RESULT_OK) {
            data?.let {
                isSkip = it.getBooleanExtra("isSkip", false)
                if (requestCode == ErikuraApplication.REQUEST_RESIGHIN) {
                    //再認証経由の場合
                    onCreateImpl(savedInstanceState = null)
                } else if (data.getBooleanExtra("isSmsAuthenticate", false)) {
                    //会員情報経由でSMS認証した場合
                    val intent = Intent(this, ConfigurationActivity::class.java)
                    intent.putExtra("onClickChangeUserInformationFragment", true)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                } else if (isSkip) {
                    //会員情報経由でスキップした場合
                    finish()
                }
            }
        } else {
            finish()
        }
    }

    override fun startResignInActivity() {
        Intent(this, ResignInActivity::class.java).let { intent ->
            intent.putExtra("fromActivity", fromActivity)
            intent.putExtra("requestCode", requestCode)
            intent.putExtra("fromSms", fromSms)
            startActivityForResult(intent, ErikuraApplication.REQUEST_RESIGHIN)
        }
    }

    override fun onCLickName(view: View) {
        val dialog = JobTitleDialogFragment.newInstance( userName?: "")
        dialog.show(supportFragmentManager, "JobTitle")
    }

    override fun onClickBirthDay(view: View) {
        val dialog = JobTitleDialogFragment.newInstance( birthDay?: "")
        dialog.show(supportFragmentManager, "JobTitle")    }

    override fun onCLickPrefecture(view: View) {
        val dialog = JobTitleDialogFragment.newInstance( prefectureName?: "")
        dialog.show(supportFragmentManager, "JobTitle")    }

    override fun onClickCity(view: View) {
        val dialog = JobTitleDialogFragment.newInstance( cityName?: "")
        dialog.show(supportFragmentManager, "JobTitle")    }

    override fun onCLickStreet(view: View) {
        val dialog = JobTitleDialogFragment.newInstance( streetName?: "")
        dialog.show(supportFragmentManager, "JobTitle")    }
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

    // 身分確認
    val identifyStatus: MutableLiveData<Int> = MutableLiveData()
    val fromSms: MutableLiveData<Boolean> = MutableLiveData()
    val confirmingUserName: MutableLiveData<Spanned> = MutableLiveData()
    val confirmingBirthDay: MutableLiveData<Spanned> = MutableLiveData()
    val confirmingPrefecture: MutableLiveData<Spanned> = MutableLiveData()
    val confirmingCityName: MutableLiveData<Spanned> = MutableLiveData()
    val confirmingStreet: MutableLiveData<Spanned> = MutableLiveData()
    val deniedUserName: MutableLiveData<Spanned> = MutableLiveData()
    val deniedBirthDay: MutableLiveData<Spanned> = MutableLiveData()
    val deniedPrefecture: MutableLiveData<Spanned> = MutableLiveData()
    val deniedCityName: MutableLiveData<Spanned> = MutableLiveData()
    val deniedStreet: MutableLiveData<Spanned> = MutableLiveData()


    val unconfirmedExplainVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(identifyStatus) { status ->
            when (status) {
                ErikuraApplication.ID_UNCONFIRMED_CODE -> {
                    result.value = View.VISIBLE
                }
                else -> {
                    result.value = View.GONE
                }
            }
        }
    }

    val unconfirmedVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(identifyStatus) { status ->
            when (status) {
                ErikuraApplication.ID_UNCONFIRMED_CODE -> {
                    result.value = View.VISIBLE
                }
                ErikuraApplication.FAILED_NEVER_APPROVED -> {
                    result.value = View.VISIBLE
                }
                else -> {
                    result.value = View.GONE
                }
            }
        }
        result.addSource(fromSms) { sms ->
            if (sms) {
                result.value = View.GONE
            }
        }
    }

    val confirmingVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(identifyStatus) { status ->
            when (status) {
                ErikuraApplication.ID_CONFIRMING_CODE -> {
                    result.value = View.VISIBLE
                }
                else -> {
                    result.value = View.GONE
                }
            }
        }
    }

    val confirmedVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(identifyStatus) { status ->
            when (status) {
                ErikuraApplication.ID_CONFIRMED_CODE -> {
                    result.value = View.VISIBLE
                }
                else -> {
                    result.value = View.GONE
                }
            }
        }
    }

    val deniedVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(identifyStatus) { status ->
            when (status) {
                ErikuraApplication.FAILED_NEVER_APPROVED -> {
                    result.value = View.VISIBLE
                }
                ErikuraApplication.FAILED_ONCE_APPROVED -> {
                    result.value = View.VISIBLE
                }
                else -> {
                    result.value = View.GONE
                }
            }
        }
    }


    val changeVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(identifyStatus) { status ->
            if (isConfirmingOrConfirmed(status)) {
                result.value = View.VISIBLE
            } else {
                result.value = View.GONE
            }
        }
        result.addSource(fromSms) { sms ->
            if (sms) {
                result.value = View.GONE
            }
        }
    }

    val inputIdentityInfoEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(identifyStatus) { status ->
            result.value = !(isConfirmingOrConfirmed(status))
        }
    }

    //確認中と確認済と否認（確認済）の場合 true
    private fun isConfirmingOrConfirmed(status: Int): Boolean {
        var isConfirmingOrConfirmed = false
        when (status) {
            ErikuraApplication.ID_CONFIRMING_CODE -> {
                isConfirmingOrConfirmed = true
            }
            ErikuraApplication.ID_CONFIRMED_CODE -> {
                isConfirmingOrConfirmed = true
            }
            ErikuraApplication.FAILED_ONCE_APPROVED -> {
                isConfirmingOrConfirmed = true
            }
        }
        return isConfirmingOrConfirmed
    }

    // バリデーションルール
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

    fun isValidPostalCode(): Boolean {
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

        if (valid && password.value.isNullOrBlank()) {
            passwordError.message.value = null
        } else {
            password.value?.let { pwd ->
                if (valid && !(pattern.matcher(pwd).find())) {
                    valid = false
                    passwordError.message.value =
                        ErikuraApplication.instance.getString(R.string.password_count_error)
                } else if (valid && !(hasAlphabet(pwd) && hasNumeric(pwd))) {
                    valid = false
                    passwordError.message.value =
                        ErikuraApplication.instance.getString(R.string.password_pattern_error)
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

        if (valid && password.value.isNullOrBlank() && verificationPassword.value.isNullOrBlank()) {
            verificationPasswordError.message.value = null
        } else {
            if (valid && !(password.value.equals(verificationPassword.value))) {
                valid = false
                verificationPasswordError.message.value =
                    ErikuraApplication.instance.getString(R.string.password_verificationPassword_match_error)
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
        val pattern2 = Pattern.compile("^(070|080|090)")

        if (valid && phone.value?.isBlank() ?: true) {
            valid = false
            phoneError.message.value = null
        } else if (valid && !(pattern.matcher(phone.value).find())) {
            valid = false
            phoneError.message.value =
                ErikuraApplication.instance.getString(R.string.phone_pattern_error)
        } else if (valid && !(phone.value?.length ?: 0 == 11)) {
            valid = false
            phoneError.message.value =
                ErikuraApplication.instance.getString(R.string.phone_count_error)
        } else if (valid && !(pattern2.matcher(phone.value).find())) {
            valid = false
            phoneError.message.value =
                ErikuraApplication.instance.getString(R.string.phone_pattern2_error)
        } else {
            valid = true
            phoneError.message.value = null
        }

        return valid
    }
}

interface ChangeUserInformationEventHandlers {
    fun onClickBirthdayEditView(view: View)
    fun onClickRegister(view: View)
    fun onClickMale(view: View)
    fun onClickFemale(view: View)
    fun onClickUpdateIdentity(view: View)
    fun onClickUpdateIdentityForChangeInfo(view: View)
    fun onCLickName(view: View)
    fun onClickBirthDay(view: View)
    fun onCLickPrefecture(view: View)
    fun onClickCity(view: View)
    fun onCLickStreet(view: View)
}