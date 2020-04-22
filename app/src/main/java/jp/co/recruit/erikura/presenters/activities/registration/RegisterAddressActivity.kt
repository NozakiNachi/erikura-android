package jp.co.recruit.erikura.presenters.activities.registration

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Spinner
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
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityRegisterAddressBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel
import java.util.regex.Pattern

class RegisterAddressActivity : BaseActivity(),
    RegisterAddressEventHandlers {
    private val viewModel: RegisterAddressViewModel by lazy {
        ViewModelProvider(this).get(RegisterAddressViewModel::class.java)
    }

    var user: User = User()
    val prefectureList = ErikuraApplication.instance.resources.obtainTypedArray(R.array.prefecture_list)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        // ユーザ情報を受け取る
        user = intent.getParcelableExtra("user")

        val binding: ActivityRegisterAddressBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_address)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        val prefectureSpinner = findViewById<Spinner>(R.id.registerAddress_prefecture)
        prefectureSpinner.isFocusable = true
        prefectureSpinner.isFocusableInTouchMode = true
        viewModel.postalCodeError.message.value = null
        viewModel.cityError.message.value = null
        viewModel.streetError.message.value = null
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_register_address", params= bundleOf())
        Tracking.view(name= "/user/register/address", title= "本登録画面（住所）")
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val view = this.currentFocus
        if (view != null) {
            val constraintLayout = findViewById<ConstraintLayout>(R.id.register_address_constraintLayout)
            constraintLayout.requestFocus()

            val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(constraintLayout.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onClickNext(view: View) {
        Log.v("POSTCODE", viewModel.postalCode.value ?: "")
        user.postcode = viewModel.postalCode.value
        Log.v("PREFECTURE", prefectureList.getString(viewModel.prefectureId.value ?: 0))
        user.prefecture = prefectureList.getString(viewModel.prefectureId.value ?: 0)
        Log.v("CITY", viewModel.city.value ?: "")
        user.city = viewModel.city.value
        Log.v("STREET", viewModel.street.value ?: "")
        user.street = viewModel.street.value

        val intent: Intent = Intent(this@RegisterAddressActivity, RegisterPhoneActivity::class.java)
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
}

class RegisterAddressViewModel: ViewModel() {
    val postalCode: MutableLiveData<String> = MutableLiveData()
    val postalCodeError: ErrorMessageViewModel = ErrorMessageViewModel()
    val prefectureId: MutableLiveData<Int> = MutableLiveData()
    val city: MutableLiveData<String> = MutableLiveData()
    val cityError: ErrorMessageViewModel = ErrorMessageViewModel()
    val street: MutableLiveData<String> = MutableLiveData()
    val streetError: ErrorMessageViewModel = ErrorMessageViewModel()

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(postalCode) { result.value = isValid() }
        result.addSource(prefectureId) { result.value = isValid() }
        result.addSource(city) { result.value = isValid() }
        result.addSource(street) { result.value = isValid() }
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
            postalCodeError.message.value = null
        } else if (valid && !(pattern.matcher(postalCode.value).find())) {
            valid = false
            postalCodeError.message.value = ErikuraApplication.instance.getString(R.string.postal_code_pattern_error)
        } else if (valid && !(postalCode.value?.length ?: 0 == 7)) {
            valid = false
            postalCodeError.message.value = ErikuraApplication.instance.getString(R.string.postal_code_count_error)
        } else {
            valid = true
            postalCodeError.message.value = null

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
            cityError.message.value = null
        } else if (valid && !(city.value?.length ?: 0 <= 20)) {
            valid = false
            cityError.message.value = ErikuraApplication.instance.getString(R.string.city_count_error)
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
            streetError.message.value = ErikuraApplication.instance.getString(R.string.street_count_error)
        } else {
            valid = true
            streetError.message.value = null
        }

        return valid
    }

}

interface RegisterAddressEventHandlers {
    fun onClickNext(view: View)
    fun onFocusChanged(view: View, hasFocus: Boolean)
}