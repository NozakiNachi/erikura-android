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
import jp.co.recruit.erikura.presenters.activities.SendChangeEmailActivity
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

    private val viewModel: ChangeUserInformationViewModel by lazy {
        ViewModelProvider(this).get(ChangeUserInformationViewModel::class.java)
    }

    // ????????????????????????
    val prefectureList =
        ErikuraApplication.instance.resources.obtainTypedArray(R.array.prefecture_list)

    // ??????????????????
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

        // ???????????????????????????????????????
        val errorMessages = intent.getStringArrayExtra("errorMessages")
        if (errorMessages != null) {
            Api(this).displayErrorAlert(errorMessages.asList())
        }
        requestCode = intent.getIntExtra("requestCode", ErikuraApplication.REQUEST_DEFAULT_CODE)
        fromSms = intent.getBooleanExtra("fromSms", false)
        fromWhere =
            intent.getIntExtra(ErikuraApplication.FROM_WHERE, ErikuraApplication.FROM_NOT_FOUND)

        // ???????????????????????????????????????????????????????????????????????????????????????
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

        //??????????????????disabled???????????????????????????disabled??????????????????????????????????????????
        viewModel.inputIdentityInfoEnabled.observe(this, androidx.lifecycle.Observer{
            if (it == false) {
                val adapter = ArrayAdapter<String>(this@ChangeUserInformationActivity, R.layout.custom_dropdown_disabled_item, ErikuraApplication.instance.resources.getStringArray(R.array.prefecture_list))
                adapter.setDropDownViewResource(R.layout.custom_dropdown_disabled_item)
                registerAddress_prefecture.adapter = adapter
            }
        })

        // ?????????????????????????????????????????????
        Tracking.logEvent(event = "view_edit_profile", params = bundleOf())
        Tracking.view(name = "/mypage/users/edit", title = "????????????????????????")

        // ??????????????????????????????????????????????????????
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
                    // ???????????????????????????
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
        // ???????????????????????????????????????????????????????????????
        if (fromWhere == ErikuraApplication.FROM_CHANGE_USER || fromWhere == ErikuraApplication.FROM_CHANGE_USER_FOR_CHANGE_INFO) {
            val dialog = AlertDialog.Builder(this)
                .setView(R.layout.dialog_uploaded_id_image)
                .setCancelable(true)
                .create()
            dialog.show()
        }

        // FDL???????????????
        ErikuraApplication.instance.removePushUriFromFDL(intent, "/app/link/mypage/user/edit")
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

    // ????????????
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
        // ?????????????????????????????????????????????????????????
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dpd.datePicker.touchables[0].performClick()
        }
    }

    // ??????
    override fun onClickMale(view: View) {
        user.gender = Gender.MALE
    }

    override fun onClickFemale(view: View) {
        user.gender = Gender.FEMALE
    }

    // ??????
    private fun getJobStatusId(jobStatus: String): Int {
        for (i in 0..8) {
            if (jobStatusIdList.getString(i).equals(jobStatus)) {
                return i
            }
        }
        return 0
    }

    override fun onClickRegister(view: View) {
        // ???????????????
        if (!viewModel.password.value.isNullOrBlank()) {
            // ????????????????????????????????????????????????????????????????????????????????????
            user.password = viewModel.password.value
        }
        // ??????
        user.lastName = viewModel.lastName.value
        user.firstName = viewModel.firstName.value
        // ????????????
        user.dateOfBirth = viewModel.dateOfBirth.value
        // ?????????
        user.postcode = viewModel.postalCode.value
        user.prefecture = prefectureList.getString(viewModel.prefectureId.value ?: 0)
        user.city = viewModel.city.value
        user.street = viewModel.street.value
        // ??????
        user.jobStatus = jobStatusIdList.getString(viewModel.jobStatusId.value ?: 0)
        // ??????????????????
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

        //????????????????????????????????????????????????????????????
        Api(this).updateUser(user) {
            // ???????????????SMS???????????????????????????????????????????????????
            val newPhoneNumber = viewModel.phone.value
            if (fromSms) {
                Api(this).smsVerifyCheck(newPhoneNumber ?: "") { result ->
                    val isChangedPhoneNumber = user.phoneNumber != newPhoneNumber
                    val finishEditing: () -> Unit = {
                        val intent = Intent()
                        //SMS???????????????????????????????????????????????????
                        intent.putExtra(SmsVerifyActivity.NewPhoneNumber, newPhoneNumber)
                        intent.putExtra(
                            SmsVerifyActivity.BeforeChangeNewPhoneNumber,
                            newPhoneNumber
                        )
                        intent.putExtra(SmsVerifyActivity.SmsVerified, result)
                        //????????????????????????????????????????????????????????????
                        if (isChangedPhoneNumber) {
                            intent.putExtra(SmsVerifyActivity.IsChangePhoneNumber, true)
                        }
                        //???????????????????????????????????????????????????????????????
                        intent.putExtra("onClickChangeUserInformationOtherThanPhone", true)
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                    if (result && isChangedPhoneNumber && this.requestCode == ErikuraApplication.REQUEST_CHANGE_USER_INFORMATION) {
                        // ??????????????????????????????????????????????????????????????????????????????SMS?????????????????????
                        user.phoneNumber = newPhoneNumber
                        Api(this).updateUser(user) {
                            finishEditing()
                        }
                    } else {
                        // SMS???????????????????????????
                        finishEditing()
                    }
                }
            } else {
                Log.v("DEBUG", "SMS????????????????????? userId=${user.id}")
                if (newPhoneNumber != null) {
                    //????????????????????????????????????
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
                                //???????????????????????????????????????????????????????????????
                                intent.putExtra("onClickChangeUserInformationOtherThanPhone", true)
                                startActivityForResult(
                                    intent,
                                    ErikuraApplication.REQUEST_CHANGE_USER_INFORMATION
                                )
                            } else {
                                // ???????????????????????????
                                // ?????????SMS???????????????????????????????????????????????????????????????????????????Api???????????????
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
                        //????????????????????????????????????
                        //?????????????????????????????????????????????????????????????????????SMS????????????????????????????????????????????????
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
        // ?????????????????????????????????????????????
        Tracking.logEvent(event = "push_identity_verification_edit_profile", params = bundleOf())
        Tracking.trackUserId("push_identity_verification_edit_profile", user)
        // ???????????????????????????????????????
        val intent = Intent(this, UpdateIdentityActivity::class.java)
        intent.putExtra("user", user)
        // ????????????????????????
        intent.putExtra(ErikuraApplication.FROM_WHERE, ErikuraApplication.FROM_CHANGE_USER)
        startActivity(intent)
    }

    override fun onClickUpdateIdentityForChangeInfo(view: View) {
        // ?????????????????????????????????????????????
        Tracking.logEvent(event = "push_identity_verification_edit_link", params = bundleOf())
        Tracking.trackUserId("push_identity_verification_edit_link", user)

        //???????????????????????????????????????
        val intent = Intent(this, UpdateIdentityActivity::class.java)
        intent.putExtra("user", user)
        // ????????????(???????????????)????????????
        intent.putExtra(
            ErikuraApplication.FROM_WHERE,
            ErikuraApplication.FROM_CHANGE_USER_FOR_CHANGE_INFO
        )
        startActivity(intent)
    }

    override fun onClickChangeEmail(view: View) {
        // ????????????????????????????????????????????????
        val intent = Intent(this, SendChangeEmailActivity::class.java)
        intent.putExtra("user", user)
        startActivity(intent)
    }

    // ????????????????????????
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

        // ??????????????????????????????????????????
        val id = getPrefectureId(user.prefecture ?: "")
        viewModel.prefectureId.value = id
        // ????????????????????????????????????
        viewModel.jobStatusId.value = getJobStatusId(user.jobStatus ?: "")

        // ??????????????????????????????????????????????????????
        viewModel.interestedSmartPhone.value = user.wishWorks.contains("smart_phone")
        viewModel.interestedCleaning.value = user.wishWorks.contains("cleaning")
        viewModel.interestedWalk.value = user.wishWorks.contains("walk")
        viewModel.interestedBicycle.value = user.wishWorks.contains("bicycle")
        viewModel.interestedCar.value = user.wishWorks.contains("car")

        // ???????????????????????????????????????
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

        //?????????????????????????????????????????????????????????
        if (identifyStatus == ErikuraApplication.ID_CONFIRMING_CODE) {
            // ?????????????????????????????????????????????????????????????????????
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
            // ???????????????????????????????????????????????????
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
                    //????????????????????????
                    onCreateImpl(savedInstanceState = null)
                } else if (data.getBooleanExtra("isSmsAuthenticate", false)) {
                    //?????????????????????SMS??????????????????
                    val intent = Intent(this, ConfigurationActivity::class.java)
                    intent.putExtra("onClickSmsVerifiedFragment", true)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                } else if (isSkip) {
                    //?????????????????????????????????????????????
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
    // ?????????????????????
    val email: MutableLiveData<String> = MutableLiveData()

    // ???????????????
    val password: MutableLiveData<String> = MutableLiveData()
    val passwordError: ErrorMessageViewModel = ErrorMessageViewModel()
    val verificationPassword: MutableLiveData<String> = MutableLiveData()
    val verificationPasswordError: ErrorMessageViewModel = ErrorMessageViewModel()

    // ??????
    val lastName: MutableLiveData<String> = MutableLiveData()
    val lastNameError: ErrorMessageViewModel = ErrorMessageViewModel()
    val firstName: MutableLiveData<String> = MutableLiveData()
    val firstNameError: ErrorMessageViewModel = ErrorMessageViewModel()

    // ????????????
    val dateOfBirth: MutableLiveData<String> = MutableLiveData()

    // ??????
    val gender: MutableLiveData<String> = MutableLiveData()
    val male: MutableLiveData<Boolean> = MutableLiveData()
    val female: MutableLiveData<Boolean> = MutableLiveData()

    // ?????????
    val postalCode: MutableLiveData<String> = MutableLiveData()
    val postalCodeError: ErrorMessageViewModel = ErrorMessageViewModel()
    val prefectureId: MutableLiveData<Int> = MutableLiveData()
    val city: MutableLiveData<String> = MutableLiveData()
    val cityError: ErrorMessageViewModel = ErrorMessageViewModel()
    val street: MutableLiveData<String> = MutableLiveData()
    val streetError: ErrorMessageViewModel = ErrorMessageViewModel()

    // ????????????
    val phone: MutableLiveData<String> = MutableLiveData()
    val phoneError: ErrorMessageViewModel = ErrorMessageViewModel()

    // ??????
    val jobStatusId: MutableLiveData<Int> = MutableLiveData()

    // ??????????????????
    val interestedSmartPhone: MutableLiveData<Boolean> = MutableLiveData()
    val interestedCleaning: MutableLiveData<Boolean> = MutableLiveData()
    val interestedWalk: MutableLiveData<Boolean> = MutableLiveData()
    val interestedBicycle: MutableLiveData<Boolean> = MutableLiveData()
    val interestedCar: MutableLiveData<Boolean> = MutableLiveData()
    val wishWalk: MutableLiveData<Int> = MutableLiveData()

    // ?????????????????????
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

    // ????????????
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

    //?????????????????????????????????????????????????????? true
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

    // ??????????????????????????????
    private fun isValid(): Boolean {
        var valid = true
        //?????????????????????????????????????????????????????????????????????
        val passwordValidAndErrorMessage = User.isValidPasswordForChangeUser(password.value)
        val verificationPassValidAndErrorMessage = User.isValidVerificationPasswordForChangeUser(password.value, verificationPassword.value)
        // ??????????????????????????????????????????????????????????????????????????????
        passwordError.message.value = passwordValidAndErrorMessage.second
        verificationPasswordError.message.value = verificationPassValidAndErrorMessage.second
        valid = passwordValidAndErrorMessage.first && valid
        valid = verificationPassValidAndErrorMessage.first && valid
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

    // FIXME: ????????????????????????????????????????????????????????????
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

    // FIXME: ????????????????????????????????????????????????????????????????????????
    private fun isValidPrefecture(): Boolean {
        return !(prefectureId.value == 0 || prefectureId.value == null)
    }

    // FIXME: ????????????????????????????????????????????????????????????????????????
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
    fun onClickChangeEmail(view: View)
}