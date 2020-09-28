package jp.co.recruit.erikura.presenters.activities.mypage

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.IdentifyComparingData
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.util.DateUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityUpdateIdentityBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.tutorial.PermitLocationActivity
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class UpdateIdentityActivity : BaseActivity(), UpdateIdentityEventHandlers {
    var user = User()
    var job = Job()
    var identifyComparingData = IdentifyComparingData()
    var fromWhere: Int? = null
    private val viewModel: UpdateIdentityViewModel by lazy {
        ViewModelProvider(this).get(UpdateIdentityViewModel::class.java)
    }

    var previousPostalCode: String? = null
    // 都道府県のリスト
    val prefectureList =
        ErikuraApplication.instance.resources.obtainTypedArray(R.array.prefecture_list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = intent.getParcelableExtra("user")
        fromWhere = intent.getIntExtra(ErikuraApplication.FROM_WHERE, ErikuraApplication.FROM_NOT_FOUND)
        // 応募経由の場合、スキップ、または認証後にjobが必要
        if (fromWhere == ErikuraApplication.FROM_ENTRY) {
            job = intent.getParcelableExtra("job")
        }

        val binding: ActivityUpdateIdentityBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_update_identity)
        binding.lifecycleOwner = this
        binding.handlers = this
        binding.viewModel = viewModel

        // エラーメッセージを受け取る
        val errorMessages = intent.getStringArrayExtra("errorMessages")
        if(errorMessages != null){
            Api(this).displayErrorAlert(errorMessages.asList())
        }

        viewModel.postalCode.observe(this, androidx.lifecycle.Observer {
            if (viewModel.isValidPostalCode() && previousPostalCode != viewModel.postalCode.value) {
                Api(this).postalCode(viewModel.postalCode.value ?: "") { prefecture, city, street ->
                    viewModel.prefectureId.value = getPrefectureId(prefecture ?: "")
                    viewModel.city.value = city
                    viewModel.street.value = street

                    if (user.postcode != viewModel.postalCode.value) {
                        val streetEditText = findViewById<EditText>(R.id.registerAddress_street)
                        streetEditText.requestFocus()
                    }
                    user.postcode = viewModel.postalCode.value
                }
            }
        })

        loadData()

    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_user_comparing_data", params= bundleOf())
        Tracking.view( "/user/verifications/comparing_data",  "本人確認情報入力画面")
    }

    // 表示する画面の初期化
    private fun loadData() {
        viewModel.fromWhere.value = fromWhere
        viewModel.lastName.value = user.lastName
        viewModel.firstName.value = user.firstName
        viewModel.dateOfBirth.value = user.parsedDateOfBirth?.let {
            SimpleDateFormat("yyyy/MM/dd").format(it)
        }

        viewModel.postalCode.value = user.postcode
        viewModel.city.value = user.city
        viewModel.street.value = user.street
        // 都道府県のプルダウン初期表示
        val id = getPrefectureId(user.prefecture ?: "")
        viewModel.prefectureId.value = id
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

    private fun getPrefectureId(prefecture: String): Int {
        for (i in 0..47) {
            if (prefectureList.getString(i).equals(prefecture)) {
                return i
            }
        }
        return 0
    }

    // 戻るボタンの制御
    override fun onBackPressed() {
        super.onBackPressed()
        when (fromWhere) {
            ErikuraApplication.FROM_CHANGE_USER, ErikuraApplication.FROM_CHANGE_USER_FOR_CHANGE_INFO -> {
                // 元の画面へ iOSでは乗っかってる画面を消して
                // 会員情報変更画面に戻る場合画面を更新するのでAndroidも更新するために画面を再生成
                val intent = Intent(this, ChangeUserInformationActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            else -> {
                finish()
            }
        }
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
            this@UpdateIdentityActivity, onDateSetListener,
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

    override fun onClickTermsOfService(view: View) {
        val termsOfServiceURLString = BuildConfig.SERVER_BASE_URL + "/pdf/terms_of_service.pdf"
        val intent = Intent(this, WebViewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(termsOfServiceURLString)
        }
        startActivity(intent)
    }

    override fun onClickPrivacyPolicy(view: View) {
        val privacyPolicyURLString = BuildConfig.SERVER_BASE_URL + "/pdf/privacy_policy.pdf"
        val intent = Intent(this, WebViewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(privacyPolicyURLString)
        }
        startActivity(intent)
    }

    override fun onClickRegister(view: View) {
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "send_comparing_data", params= bundleOf())
        Tracking.trackUserId( "send_comparing_data",  user)
        // 身元確認画面へ遷移する
        val intent = Intent(this, UploadIdImageActivity::class.java)
        // 入力された本人確認情報
        identifyComparingData.lastName = viewModel.lastName.value
        identifyComparingData.firstName = viewModel.firstName.value
        identifyComparingData.dateOfBirth = DateUtils.parseDate(viewModel.dateOfBirth.value, arrayOf("yyyy/MM/dd", "yyyy-MM-dd"))
        identifyComparingData.postcode = viewModel.postalCode.value
        identifyComparingData.prefecture = prefectureList.getString(viewModel.prefectureId.value ?: 0)
        identifyComparingData.city = viewModel.city.value
        identifyComparingData.street = viewModel.street.value
        intent.putExtra("identifyComparingData", identifyComparingData)
        intent.putExtra("user", user)
        if (fromWhere == ErikuraApplication.FROM_ENTRY) {
            intent.putExtra("job", job)
        }
        intent.putExtra(ErikuraApplication.FROM_WHERE, fromWhere)
        startActivity(intent)
        finish()
    }

    override fun onClickSkip(view: View) {
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "skip_user_verifications_comparing_data", params= bundleOf())
        Tracking.trackUserId( "skip_user_verifications_comparing_data",  user)
        //遷移元によって遷移先を切り分ける
        when(fromWhere) {
            ErikuraApplication.FROM_REGISTER -> {
                // 地図画面へ
                if (ErikuraApplication.instance.isOnboardingDisplayed()) {
                    // 地図画面へ遷移
                    val intent = Intent(this, MapViewActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
                else {
                    // 位置情報の許諾、オンボーディングを表示します
                    Intent(this, PermitLocationActivity::class.java).let { intent ->
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
            ErikuraApplication.FROM_CHANGE_USER, ErikuraApplication.FROM_CHANGE_USER_FOR_CHANGE_INFO -> {
                // 元の画面へ iOSでは乗っかってる画面を消して
                // 会員情報変更画面に戻る場合画面を更新するのでAndroidも更新するために画面を再生成
                val intent = Intent(this, ChangeUserInformationActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            ErikuraApplication.FROM_ENTRY -> {
                // 仕事詳細へ遷移し応募確認ダイアログへ
                val intent= Intent(this, JobDetailsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("fromIdentify", true)
                intent.putExtra("job", job)
                startActivity(intent)
                finish()
            }
        }
    }

}

class UpdateIdentityViewModel: ViewModel() {
    // 遷移元
    val fromWhere: MutableLiveData<Int> = MutableLiveData()
    // 氏名
    val lastName: MutableLiveData<String> = MutableLiveData()
    val lastNameError: ErrorMessageViewModel = ErrorMessageViewModel()
    val firstName: MutableLiveData<String> = MutableLiveData()
    val firstNameError: ErrorMessageViewModel = ErrorMessageViewModel()
    // 生年月日
    val dateOfBirth: MutableLiveData<String> = MutableLiveData()
    // 所在地
    val postalCode: MutableLiveData<String> = MutableLiveData()
    val postalCodeError: ErrorMessageViewModel = ErrorMessageViewModel()
    val prefectureId: MutableLiveData<Int> = MutableLiveData()
    val city: MutableLiveData<String> = MutableLiveData()
    val cityError: ErrorMessageViewModel = ErrorMessageViewModel()
    val street: MutableLiveData<String> = MutableLiveData()
    val streetError: ErrorMessageViewModel = ErrorMessageViewModel()
    // 登録ボタン押下
    val isChangeButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(lastName) { result.value = isValid() }
        result.addSource(firstName) { result.value = isValid() }
        result.addSource(dateOfBirth) { result.value = isValid() }
        result.addSource(postalCode) { result.value = isValid() }
        result.addSource(prefectureId) { result.value = isValid() }
        result.addSource(city) { result.value = isValid() }
        result.addSource(street) { result.value = isValid() }
    }
    // テキスト
    var updateIdentityCaption1 = MediatorLiveData<String>().also { result ->
        result.addSource(fromWhere) { from ->
            //応募経由の場合、会員情報変更から入力値の変更経由の場合
            when(from) {
                ErikuraApplication.FROM_CHANGE_USER_FOR_CHANGE_INFO -> {
                    result.value = ErikuraApplication.instance.getString(R.string.update_identity_caption1_from_change)
                }
                ErikuraApplication.FROM_ENTRY -> {
                    result.value = ErikuraApplication.instance.getString(R.string.update_identity_caption1_from_entry)
                }
            }
        }
    }
    // 各visibility
    var captionBlockVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(fromWhere) { from ->
            if (from == ErikuraApplication.FROM_REGISTER) {
                result.value = View.GONE
            } else {
                result.value = View.VISIBLE
            }
        }
    }

    var caption1Visibility = MediatorLiveData<Int>().also { result ->
        result.addSource(fromWhere) { from ->
            if (from == ErikuraApplication.FROM_CHANGE_USER || from == ErikuraApplication.FROM_REGISTER) {
                result.value = View.GONE
            } else {
                result.value = View.VISIBLE
            }
        }
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


    // バリデーションルール
    private fun isValid(): Boolean {
        var valid = true
        valid = isValidFirstName() && valid
        valid = isValidLastName() && valid
        valid = isValidPostalCode() && valid
        valid = isValidPrefecture() && valid
        valid = isValidCity() && valid
        valid = isValidStreet() && valid
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

    private fun isValidPrefecture(): Boolean {
        return !(prefectureId.value == 0 || prefectureId.value == null)
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

}

interface UpdateIdentityEventHandlers {
    fun onClickBirthdayEditView(view: View)
    fun onClickRegister(view: View)
    fun onClickSkip(view: View)
    fun onClickTermsOfService(view: View)
    fun onClickPrivacyPolicy(view: View)
}