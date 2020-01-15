package jp.co.recruit.erikura.presenters.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityRegisterAddressBinding
import java.util.regex.Pattern





class RegisterAddressActivity : AppCompatActivity(), RegisterAddressEventHandlers {
    private val viewModel: RegisterAddressViewModel by lazy {
        ViewModelProvider(this).get(RegisterAddressViewModel::class.java)
    }

    var user: User = User()
    val prefectureList = ErikuraApplication.instance.resources.obtainTypedArray(R.array.prefecture_list)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_register_address)

        // ユーザ情報を受け取る
        user = intent.getParcelableExtra("user")

        val binding: ActivityRegisterAddressBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_address)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
        viewModel.postalCodeErrorVisibility.value = 8
        viewModel.cityErrorVisibility.value = 8
        viewModel.streetErrorVisibility.value = 8
    }

    override fun onClickNext(view: View) {
        Log.v("POSTCODE", viewModel.postalCode.value ?: "")
        user.postCode = viewModel.postalCode.value
        Log.v("PREFECTURE", prefectureList.getString(viewModel.prefectureId.value ?: 0))
        user.prefecture = prefectureList.getString(viewModel.prefectureId.value ?: 0)
        Log.v("CITY", viewModel.city.value ?: "")
        user.city = viewModel.city.value
        Log.v("STREET", viewModel.street.value ?: "")
        user.street = viewModel.street.value

        // FIXME: 電話番号登録画面へ遷移

    }
}

class RegisterAddressViewModel: ViewModel() {
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

    fun addressAutoComplement() {
        /*Api(this).postalCode(postalCode.value ?: "") { prefecture, city, street ->

        }*/
    }

}

interface RegisterAddressEventHandlers {
    fun onClickNext(view: View)
}