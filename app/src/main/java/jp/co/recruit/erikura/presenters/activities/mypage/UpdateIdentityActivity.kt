package jp.co.recruit.erikura.presenters.activities.mypage

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityUpdateIdentityBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import java.util.regex.Pattern

class UpdateIdentityActivity : BaseActivity(), UpdateIdentityEventHandlers {
    var user = User()
    var from: Int? = null
    private val viewModel: UpdateIdentityViewModel by lazy {
        ViewModelProvider(this).get(UpdateIdentityViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        user = intent.getParcelableExtra("user")
        from = intent.getIntExtra(ErikuraApplication.FROM, ErikuraApplication.FROM_NOT_FOUND)

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


    }

    // 表示する画面の初期化
    private fun loadData() {
        //FIXME viewModelの初期化
        viewModel.from.value = from
    }

}

class UpdateIdentityViewModel: ViewModel() {
    // 遷移元
    val from: MutableLiveData<Int> = MutableLiveData()
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
        result.addSource(from) { from ->
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
    // FIXME 遷移もとによってvisibilityを制御

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

}