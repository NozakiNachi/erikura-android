package jp.co.recruit.erikura.presenters.activities.registration

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
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
import jp.co.recruit.erikura.business.models.ErikuraConfig
import jp.co.recruit.erikura.business.models.ErikuraConst
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityRegisterEmailBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel

class RegisterEmailActivity : BaseActivity(),
    RegisterEmailEventHandlers {
    private val viewModel: RegisterEmailViewModel by lazy {
        ViewModelProvider(this).get(RegisterEmailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        val binding: ActivityRegisterEmailBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_email)
        binding.lifecycleOwner = this
        viewModel.setupHandler(this)
        binding.viewModel = viewModel
        binding.handlers = this
        viewModel.error.message.value = null
    }

    override fun onStart() {
        super.onStart()
        this.findViewById<TextView>(R.id.agreementLink)?.movementMethod = LinkMovementMethod.getInstance()

        // ?????????????????????????????????????????????
        Tracking.logEvent(event = "view_temp_register", params = bundleOf())
        Tracking.view(name = "/user/register/pre", title = "???????????????")
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val view = this.currentFocus
        if (view != null) {
            val constraintLayout = findViewById<ConstraintLayout>(R.id.register_email_constraintLayout)
            constraintLayout.requestFocus()

            val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(constraintLayout.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onClickSendEmail(view: View) {
        Log.v("EMAIL", viewModel.email.value ?: "")
        // ?????????API?????????
        Api(this).registerEmail(viewModel.email.value ?:"") {
            Log.v("DEBUG", "??????????????????????????? userId=${it}")
            val intent: Intent = Intent(this@RegisterEmailActivity, RegisterEmailFinishedActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onClickTermsOfService(view: View) {
        try {
            val termsOfServiceURLString =
                BuildConfig.SERVER_BASE_URL + BuildConfig.TERMS_OF_SERVICE_PATH
            val intent = Intent(this, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(termsOfServiceURLString)
            }
            startActivity(intent)
        }
        catch (e: ActivityNotFoundException) {
            Api(this).displayErrorAlert(listOf("PDF??????????????????????????????????????????\nPDF??????????????????????????????????????????????????????????????????"))
        }
    }

    override fun onClickPrivacyPolicy(view: View) {
        try {
            val privacyPolicyURLString =
                BuildConfig.SERVER_BASE_URL + BuildConfig.PRIVACY_POLICY_PATH
            val intent = Intent(this, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(privacyPolicyURLString)
            }
            startActivity(intent)
        }
        catch (e: ActivityNotFoundException) {
            Api(this).displayErrorAlert(listOf("PDF??????????????????????????????????????????\nPDF??????????????????????????????????????????????????????????????????"))
        }
    }
}

class RegisterEmailViewModel: ViewModel() {
    var handler: RegisterEmailEventHandlers? = null
    val agreementText = MutableLiveData<SpannableStringBuilder>(
        SpannableStringBuilder().also { str ->
            JobUtil.appendLinkSpan(str, ErikuraApplication.instance.getString(R.string.registerEmail_terms_of_service), R.style.linkText) {
                handler?.onClickTermsOfService(it)
            }
            str.append(ErikuraApplication.instance.getString(R.string.registerEmail_comma))
            JobUtil.appendLinkSpan(str, ErikuraApplication.instance.getString(R.string.registerEmail_privacy_policy, ErikuraConfig.ppTermsTitle), R.style.linkText) {
                handler?.onClickPrivacyPolicy(it)
            }
            str.append(ErikuraApplication.instance.getString(R.string.registerEmail_agree))
        }
    )
    private val emailPattern = ErikuraConst.emailPattern

    val email: MutableLiveData<String> = MutableLiveData()
    val error: ErrorMessageViewModel = ErrorMessageViewModel()

    val isRegisterEmailButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(email) { result.value = isValid() }
    }

    private fun isValid(): Boolean {
        var valid = true

        if (valid && email.value?.isBlank() ?: true) {
            valid = false
            error.message.value = null
        }else if (valid && !(emailPattern.matches(email.value ?: ""))) {
            valid = false
            error.message.value = ErikuraApplication.instance.getString(R.string.email_format_error)
        } else {
            valid = true
            error.message.value = null
        }

        return valid
    }

    fun setupHandler(handler: RegisterEmailEventHandlers?) {
        this.handler = handler
    }
}

interface RegisterEmailEventHandlers {
    fun onClickSendEmail(view: View)
    fun onClickTermsOfService(view: View)
    fun onClickPrivacyPolicy(view: View)
}