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
import java.util.*
import java.util.regex.Pattern


class AccountSettingActivity : AppCompatActivity(), AccountSettingEventHandlers {

    var user: User = User()

    private val viewModel: AccountSettingViewModel by lazy {
        ViewModelProvider(this).get(AccountSettingViewModel::class.java)
    }

//    // 都道府県のリスト
//    val prefectureList = ErikuraApplication.instance.resources.obtainTypedArray(R.array.prefecture_list)
//    // 職業のリスト
//    val jobStatusIdList = ErikuraApplication.instance.resources.obtainTypedArray(R.array.job_status_id_list)
//
//    // 生年月日入力のカレンダー設定
//    val calender: Calendar = Calendar.getInstance()
//    var date: DatePickerDialog.OnDateSetListener =
//        DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
//            calender.set(Calendar.YEAR, year)
//            calender.set(Calendar.MONTH, monthOfYear)
//            calender.set(Calendar.DAY_OF_MONTH, dayOfMonth)
//
//            viewModel.dateOfBirth.value =
//                String.format("%d/%02d/%02d", year, monthOfYear + 1, dayOfMonth)
//        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_setting)

        val binding: ActivityAccountSettingBinding = DataBindingUtil.setContentView(this, R.layout.activity_account_setting)
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
//        // 生年月日入力のカレンダー設定
//        calender.set(Calendar.YEAR, 1980)
//        calender.set(Calendar.MONTH, 1 - 1)
//        calender.set(Calendar.DAY_OF_MONTH, 1)
//        viewModel.dateOfBirth.value = String.format("%d/%02d/%02d", 1980, 1, 1)
//
//        // 変更するユーザーの現在の登録値を取得
//        Api(this).user(){
//            user = it
//            viewModel.CurrentEmail.value = user.email
//            viewModel.CurrentLastName.value = user.lastName
//            viewModel.CurrentFirstName.value = user.firstName
//            viewModel.CurrentdateOfBirth.value = user.dateOfBirth
//            viewModel.CurrentGender.value = user.gender?.value
//            viewModel.CurrentPostalCode.value = user.postcode
////            viewModel.CurrentPrefectureId.value = user.prefecture
//            viewModel.CurrentCity.value = user.city
//            viewModel.CurrentStreet.value = user.street
//            viewModel.CurrentPhoneNumber.value = user.phoneNumber
// //           viewModel.CurrentJobStatusId.value = user.jobStatus
//
//            // 都道府県のプルダウン初期表示
//            viewModel.CurrentPrefectureId.value = getPrefectureId(user.prefecture ?: "")
//            // 職業のプルダウン初期表示
//            viewModel.CurrentJobStatusId.value = getJobStatusId(user.jobStatus ?: "")
//
//            // やりたい仕事のチェックボタン初期表示
//            val wishWorkList: MutableList<String> = mutableListOf()
//            wishWorkList.add(user.wishWorks.getOrNull(0).toString())
//            wishWorkList.add(user.wishWorks.getOrNull(1).toString())
//            wishWorkList.add(user.wishWorks.getOrNull(2).toString())
//            wishWorkList.add(user.wishWorks.getOrNull(3).toString())
//            wishWorkList.add(user.wishWorks.getOrNull(4).toString())

//            if(wishWorkList.contains("smart_phone")){
//                binding.wishWorkSmartPhone.isChecked = true
//            }
//            if(wishWorkList.contains("cleaning")) {
//                binding.wishWorkCleaning.isChecked = true
//            }
//            if(wishWorkList.contains("walk")) {
//                binding.wishWorkWalk.isChecked = true
//            }
//            if(wishWorkList.contains("bicycle"))
//            {
//                binding.wishWorkBicycle.isChecked = true
//            }
//            if(wishWorkList.contains("car")) {
//                binding.wishWorkCar.isChecked = true
//            }
//
//            // 性別のラジオボタン初期表示
//            if(viewModel.CurrentGender.value == "male"){
//                binding.maleButton.isChecked = true
//            }else{
//                binding.femaleButton.isChecked = true
//            }
//        }
//    }


//override fun onClickRegister(view: View) {

//        // 会員情報変更Apiの呼び出し
//        Api(this).updateUser(user) {
//            // FIXME: ダイアログの表示時間を調整
//            val dialog = AlertDialog.Builder(this).apply {
//                val binding: DialogAccountSettingBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.dialog_change_user_information_success, null, false)
//                setView(binding.root)
//            }.create()
//            dialog.show()
//        }
//        finish()

    }
}


class AccountSettingViewModel: ViewModel() {
    // 現在の登録値
    val CurrentEmail: MutableLiveData<String> = MutableLiveData()
    val CurrentLastName: MutableLiveData<String> = MutableLiveData()
    val CurrentFirstName: MutableLiveData<String> = MutableLiveData()
    val CurrentdateOfBirth: MutableLiveData<String> = MutableLiveData()
    val CurrentGender: MutableLiveData<String> = MutableLiveData()
    val CurrentPostalCode: MutableLiveData<String> = MutableLiveData()
    val CurrentPrefectureId: MutableLiveData<Int> = MutableLiveData()
    val CurrentCity: MutableLiveData<String> = MutableLiveData()
    val CurrentStreet: MutableLiveData<String> = MutableLiveData()
    val CurrentPhoneNumber: MutableLiveData<String> = MutableLiveData()
    val CurrentJobStatusId: MutableLiveData<Int> = MutableLiveData()
    val CurrentWishWorks: MutableLiveData<String> = MutableLiveData()

    // パスワード
    val password: MutableLiveData<String> = MutableLiveData()
    val verificationPassword: MutableLiveData<String> = MutableLiveData()
    val passwordErrorMsg: MutableLiveData<String> = MutableLiveData()
    val passwordErrorVisibility: MutableLiveData<Int> = MutableLiveData()
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
    val gender: MutableLiveData<Boolean> = MutableLiveData()
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

    // 登録ボタン押下
    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(lastName) { result.value = isValid() }
        result.addSource(firstName) { result.value = isValid() }
        result.addSource(dateOfBirth) { result.value = isValid() }
        result.addSource(gender) { result.value = isValid() }
        result.addSource(phone) { result.value = isValid() }
        result.addSource(jobStatusId) { result.value = isValid() }
        // result.addSource(WishWorks) { result.value = isValid() }
        result.addSource(postalCode) { result.value = isValid() }
        result.addSource(prefectureId) { result.value = isValid() }
        result.addSource(city) { result.value = isValid() }
        result.addSource(street) { result.value = isValid() }
    }

    // バリデーションルール
    private fun isValid(): Boolean {
        var valid = true
        valid = isValidPostalCode() && valid
        valid = isValidPrefecture() && valid
        valid = isValidCity() && valid
        valid = isValidStreet() && valid
        valid = isValidFirstName() && valid
        valid = isValidLastName() && valid
        valid = isValidPassword() && valid
        valid = isValidPhoneNumber() && valid

        return valid
    }

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

    private fun isValidPassword(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([a-zA-Z0-9]{6,})\$")
        val alPattern = Pattern.compile("^(.*[A-z]+.*)")
        val numPattern = Pattern.compile("^(.*[0-9]+.*)")

        if (valid && password.value?.isBlank() ?:true) {
            valid = false
            passwordErrorMsg.value = ""
            passwordErrorVisibility.value = 8
        }else if(valid && !(pattern.matcher(password.value).find())) {
            valid = false
            passwordErrorMsg.value = ErikuraApplication.instance.getString(R.string.password_count_error)
            passwordErrorVisibility.value = 0
        }else if(valid && (!(alPattern.matcher(password.value).find()) || !(numPattern.matcher(password.value).find()))) {
            valid = false
            passwordErrorMsg.value = ErikuraApplication.instance.getString(R.string.password_pattern_error)
            passwordErrorVisibility.value = 0
        }else if(valid && password.value !== verificationPassword.value) {
            passwordErrorMsg.value = ErikuraApplication.instance.getString(R.string.password_verificationPassword_match_error)
            passwordErrorVisibility.value = 0
        } else {
            valid = true
            passwordErrorMsg.value = ""
            passwordErrorVisibility.value = 8
        }
        return valid
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

interface AccountSettingEventHandlers {

}