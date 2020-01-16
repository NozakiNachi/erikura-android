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
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityRegisterWishWorkBinding
import java.util.regex.Pattern

class RegisterWishWorkActivity : AppCompatActivity(), RegisterWishWorkEventHandlers {
    private val viewModel: RegisterWishWorkViewModel by lazy {
        ViewModelProvider(this).get(RegisterWishWorkViewModel::class.java)
    }

    var user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_register_wish_work)

        // ユーザ情報を受け取る
        user = intent.getParcelableExtra("user")

        val binding: ActivityRegisterWishWorkBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_wish_work)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
    }

    override fun onClickRegister(view: View) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
}

class RegisterWishWorkViewModel: ViewModel() {
    val isCheckedSmartPhone: MutableLiveData<Boolean> = MutableLiveData()
    val isCheckedCleaning: MutableLiveData<Boolean> = MutableLiveData()
    val isCheckedWalk: MutableLiveData<Boolean> = MutableLiveData()
    val isCheckedBicycle: MutableLiveData<Boolean> = MutableLiveData()
    val isCheckedCar: MutableLiveData<Boolean> = MutableLiveData()

    val isRegisterButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.value = isCheckedSmartPhone.value ?:false || isCheckedCleaning.value ?:false || isCheckedWalk.value ?:false || isCheckedBicycle.value ?:false || isCheckedCar.value ?:false
    }
}

interface RegisterWishWorkEventHandlers {
    fun onClickRegister(view: View)
    fun onClickTermsOfService(view: View)
    fun onClickPrivacyPolicy(view: View)
}