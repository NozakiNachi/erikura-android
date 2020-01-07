package jp.co.recruit.erikura.presenters.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityRegisterEmailBinding

class RegisterEmailActivity : AppCompatActivity(), SendEmailEventHandlers, TextWatcher {
    private val viewModel: RegisterEmailViewModel by lazy {
        ViewModelProvider(this).get(RegisterEmailViewModel::class.java)
    }

    lateinit var button: Button
    lateinit var editText: EditText
    lateinit var errorText: TextView
    lateinit var mlp: ViewGroup.MarginLayoutParams

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        val binding: ActivityRegisterEmailBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_email)
        binding.viewModel = viewModel
        binding.handlers = this

        button = findViewById(R.id.registerEmail_button)
        button.isEnabled = false

        editText = findViewById(R.id.registerEmail_editText)
        editText.addTextChangedListener(this)

        errorText = findViewById(R.id.registerEmail_errorTextView)
        errorText.isInvisible = true
        mlp = errorText.layoutParams as ViewGroup.MarginLayoutParams
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

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        validate()
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        validate()
    }

    override fun afterTextChanged(s: Editable?) {
        validate()
    }

    private fun validate(){
        var valid = true
        // バリデーションの設定
        if (valid && TextUtils.isEmpty(viewModel.email.value)) {
            valid = false
            errorText.isInvisible = true
            errorText.text = ""
            errorText.textSize = 0f
            mlp.topMargin = 0
        }else if(valid && !(android.util.Patterns.EMAIL_ADDRESS.matcher(viewModel.email.value ?:"").matches())) {
            valid = false
            errorText.isInvisible = false
            errorText.text = getString(R.string.email_format_error)
            errorText.textSize = 14.0f
            mlp.topMargin = 10
        }else {
            valid = true
            errorText.isInvisible = true
            errorText.text = ""
            errorText.textSize = 0f
            mlp.topMargin = 0
        }
        errorText.layoutParams = mlp
        button.isEnabled = valid
    }
}

class RegisterEmailViewModel: ViewModel() {
    val email: MutableLiveData<String> = MutableLiveData()
}

interface SendEmailEventHandlers {
    fun onClickSendEmail(view: View)
    fun onClickTermsOfService(view: View)
    fun onClickPrivacyPolicy(view: View)
}