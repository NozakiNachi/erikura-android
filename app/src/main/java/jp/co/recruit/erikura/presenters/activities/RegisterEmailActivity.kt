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
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityRegisterEmailBinding

class RegisterEmailActivity : AppCompatActivity(), SendEmailEventHandlers {
    private val viewModel: RegisterEmailViewModel by lazy {
        ViewModelProvider(this).get(RegisterEmailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_email)

        val binding: ActivityRegisterEmailBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_email)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
        viewModel.errorVisibility.value = 8
    }

    override fun onClickSendEmail(view: View) {
        Log.v("EMAIL", viewModel.email.value ?: "")
        // 仮登録APIの実行
        Api(this).registerEmail(viewModel.email.value ?:"") {
            Log.v("DEBUG", "仮登録メール送信： userId=${it}")
            val intent: Intent = Intent(this@RegisterEmailActivity, RegisterEmailFinishedActivity::class.java)
            startActivity(intent)
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
}

class RegisterEmailViewModel: ViewModel() {
    val email: MutableLiveData<String> = MutableLiveData()
    val errorMsg: MutableLiveData<String> = MutableLiveData()
    val errorVisibility: MutableLiveData<Int> = MutableLiveData()

    val isRegisterEmailButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(email) { result.value = isValid() }
    }

    private fun isValid(): Boolean {
        var valid = true

        if (valid && email.value?.isBlank() ?: true) {
            valid = false
            errorMsg.value = ""
            errorVisibility.value = 8
        }else if (valid && !(android.util.Patterns.EMAIL_ADDRESS.matcher(email.value ?:"").matches())) {
            valid = false
            errorMsg.value = ErikuraApplication.instance.getString(R.string.email_format_error)
            errorVisibility.value = 0
        } else {
            valid = true
            errorMsg.value = ""
            errorVisibility.value = 8
        }

        return valid
    }
}

interface SendEmailEventHandlers {
    fun onClickSendEmail(view: View)
    fun onClickTermsOfService(view: View)
    fun onClickPrivacyPolicy(view: View)
}