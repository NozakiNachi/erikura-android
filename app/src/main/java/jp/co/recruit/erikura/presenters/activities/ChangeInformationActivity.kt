package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Gender
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.*
import jp.co.recruit.erikura.presenters.activities.registration.RegisterFinishedActivity
import jp.co.recruit.erikura.presenters.activities.registration.RegisterPhoneActivity
import java.util.*
import java.util.regex.Pattern


class ChangeInformationActivity : AppCompatActivity(), ChangeInformationEventHandlers {

    var user: User = User()
    // カレンダー設定
    val calender: Calendar = Calendar.getInstance()
    var date: DatePickerDialog.OnDateSetListener =
        DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            calender.set(Calendar.YEAR, year)
            calender.set(Calendar.MONTH, monthOfYear)
            calender.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            viewModel.birthday.value =
                String.format("%d/%02d/%02d", year, monthOfYear + 1, dayOfMonth)
        }

    private val viewModel: ChangeInformationViewModel by lazy {
        ViewModelProvider(this).get(ChangeInformationViewModel::class.java)
    }

    val prefectureList = ErikuraApplication.instance.resources.obtainTypedArray(R.array.prefecture_list)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_information)

//        // ユーザ情報を受け取る
//        user = intent.getParcelableExtra("user")

        val binding: ActivityChangeInformationBinding = DataBindingUtil.setContentView(this, R.layout.activity_change_information)
        binding.lifecycleOwner = this
        binding.handlers = this
        binding.viewModel = viewModel

        // 所在地
        viewModel.postalCodeErrorVisibility.value = 8
        viewModel.cityErrorVisibility.value = 8
        viewModel.streetErrorVisibility.value = 8
        viewModel.lastNameErrorVisibility.value = 8
        viewModel.firstNameErrorVisibility.value = 8

        // 生年月日
        calender.set(Calendar.YEAR, 1980)
        calender.set(Calendar.MONTH, 1 - 1)
        calender.set(Calendar.DAY_OF_MONTH, 1)
        viewModel.birthday.value = String.format("%d/%02d/%02d", 1980, 1, 1)
    }

    // 郵便番号・都道府県・住所・番地
    override fun onClickNext(view: View) {
        Log.v("POSTCODE", viewModel.postalCode.value ?: "")
        user.postcode = viewModel.postalCode.value
        Log.v("PREFECTURE", prefectureList.getString(viewModel.prefectureId.value ?: 0))
        user.prefecture = prefectureList.getString(viewModel.prefectureId.value ?: 0)
        Log.v("CITY", viewModel.city.value ?: "")
        user.city = viewModel.city.value
        Log.v("STREET", viewModel.street.value ?: "")
        user.street = viewModel.street.value

        val intent: Intent = Intent(this@ChangeInformationActivity, RegisterPhoneActivity::class.java)
        intent.putExtra("user", user)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())

    }

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

    // 性別
    override fun onClickMale(view: View) {
        Log.v("GENDER", "male")
        user.gender = Gender.MALE
    }
    override fun onClickFemale(view: View) {
        Log.v("GENDER", "female")
        user.gender = Gender.FEMALE
    }

    // 生年月日
    override fun onClickEditView(view: View) {
        Log.v("EditView", "EditTextTapped!")
        val dpd = DatePickerDialog(
            this@ChangeInformationActivity, date, calender
                .get(Calendar.YEAR), calender.get(Calendar.MONTH),
            calender.get(Calendar.DAY_OF_MONTH)
        )

        val dp = dpd.datePicker
        val maxDate: Calendar = Calendar.getInstance()
        maxDate.add(Calendar.YEAR, -18)
        dp.maxDate = maxDate.timeInMillis

        dpd.show()
    }

    // 職業選択
    override fun onClickUnemployed(view: View) {
        user.jobStatus = "unemployed"
    }
    override fun onClickHomemaker(view: View) {
        user.jobStatus= "homemaker"
    }
    override fun onClickFreelancer(view: View) {
        user.jobStatus = "freelancer"
    }
    override fun onClickStudent(view: View) {
        user.jobStatus = "student"
    }
    override fun onClickPartTime(view: View) {
        user.jobStatus = "part_time"
    }
    override fun onClickEmployee(view: View) {
        user.jobStatus = "employee"
    }
    override fun onClickSelfEmployed(view: View) {
        user.jobStatus = "self_employed"
    }
    override fun onClickOtherJob(view: View) {
        user.jobStatus = "other_job"
    }

    override fun onClickRegister(view: View) {
        val list: MutableList<String> = mutableListOf()
        if(viewModel.interestedSmartPhone.value ?: false){ list.add("smart_phone") }
        if(viewModel.interestedCleaning.value ?: false){ list.add("cleaning") }
        if(viewModel.interestedWalk.value ?: false){ list.add("walk") }
        if(viewModel.interestedBicycle.value ?: false){ list.add("bicycle") }
        if(viewModel.interestedCar.value ?: false){ list.add("car") }
        user.wishWorks = list
        Log.v("WISHWORK", list.toString())
        // ユーザ登録Apiの呼び出し
        Api(this).updateUser(user) {
            Log.v("DEBUG", "ユーザ登録： userId=${it}")
            // 登録完了画面へ遷移
            val intent: Intent = Intent(this@ChangeInformationActivity, RegisterFinishedActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    override fun onClickTermsOfService(view: View) {
        val termsOfServiceURLString = BuildConfig.SERVER_BASE_URL + "/pdf/terms_of_service.pdf"
        val intent = Intent(this, WebViewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(termsOfServiceURLString)
        }
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onClickPrivacyPolicy(view: View) {
        val privacyPolicyURLString = BuildConfig.SERVER_BASE_URL + "/pdf/privacy_policy.pdf"
        val intent = Intent(this, WebViewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(privacyPolicyURLString)
        }
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

}

class ChangeInformationViewModel: ViewModel() {
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
    val birthday: MutableLiveData<String> = MutableLiveData()

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(postalCode) { result.value = isValid() }
        result.addSource(prefectureId) { result.value = isValid() }
        result.addSource(city) { result.value = isValid() }
        result.addSource(street) { result.value = isValid() }
    }

    val interestedSmartPhone: MutableLiveData<Boolean> = MutableLiveData()
    val interestedCleaning: MutableLiveData<Boolean> = MutableLiveData()
    val interestedWalk: MutableLiveData<Boolean> = MutableLiveData()
    val interestedBicycle: MutableLiveData<Boolean> = MutableLiveData()
    val interestedCar: MutableLiveData<Boolean> = MutableLiveData()

    val isRegisterButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(interestedSmartPhone) {result.value = isValid()}
        result.addSource(interestedCleaning) {result.value = isValid()}
        result.addSource(interestedWalk) {result.value = isValid()}
        result.addSource(interestedBicycle) {result.value = isValid()}
        result.addSource(interestedCar) {result.value = isValid()}
    }

    val lastName: MutableLiveData<String> = MutableLiveData()
    val lastNameErrorMsg: MutableLiveData<String> = MutableLiveData()
    val lastNameErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    val firstName: MutableLiveData<String> = MutableLiveData()
    val firstNameErrorMsg: MutableLiveData<String> = MutableLiveData()
    val firstNameErrorVisibility: MutableLiveData<Int> = MutableLiveData()

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

    private fun isValid(): Boolean {
        var valid = true
        valid = isValidPostalCode() && valid
        valid = isValidPrefecture() && valid
        valid = isValidCity() && valid
        valid = isValidStreet() && valid

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

    private fun isValidPrefecture(): Boolean {
        return !(prefectureId.value == 0)
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

}

interface ChangeInformationEventHandlers {
    fun onClickMale(view: View)
    fun onClickFemale(view: View)
    fun onClickNext(view: View)
    fun onFocusChanged(view: View, hasFocus: Boolean)
    fun onClickEditView(view: View)
    fun onClickUnemployed(view: View)
    fun onClickHomemaker(view: View)
    fun onClickFreelancer(view: View)
    fun onClickStudent(view: View)
    fun onClickPartTime(view: View)
    fun onClickEmployee(view: View)
    fun onClickSelfEmployed(view: View)
    fun onClickOtherJob(view: View)
    fun onClickRegister(view: View)
    fun onClickTermsOfService(view: View)
    fun onClickPrivacyPolicy(view: View)
}