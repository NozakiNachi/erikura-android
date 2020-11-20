package jp.co.recruit.erikura.presenters.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
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
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityResetPasswordBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel
import java.util.regex.Pattern

class ResetPasswordActivity : BaseActivity(),
    ResetPasswordEventHandlers {
    var user: User = User()
    private var resetPasswordToken: String? = null

    private val viewModel: ResetPasswordViewModel by lazy {
        ViewModelProvider(this).get(ResetPasswordViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityResetPasswordBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_reset_password)
        binding.lifecycleOwner = this
        binding.handlers = this
        binding.viewModel = viewModel

        // FIXME FDLは12/11リリース
        // FDLの場合
//        resetPasswordToken = handleIntent(intent)

        // FIXME FDLは12/11削除予定
        // パスワード再設定トークン取得
        var uri: Uri? = intent.data
        if (uri?.path == "${BuildConfig.ERIKURA_RELATIVE_URL_ROOT}/api/v1/utils/open_android_app") {
            val path = uri?.getQueryParameter("path")
            uri = Uri.parse("erikura://${path}")
        }
        else if (uri?.path == "/api/v1/utils/open_android_app") {
            val path = uri?.getQueryParameter("path")
            uri = Uri.parse("erikura://${path}")
        }
        resetPasswordToken = uri?.getQueryParameter("reset_password_token")
        // FIXME FDLは12/11削除予定ここまで

        // エラーメッセージを受け取る
        val errorMessages = intent.getStringArrayExtra("errorMessages")
        if (errorMessages != null) {
            Api(this).displayErrorAlert(errorMessages.asList())
        }

        // ページ参照のトラッキングの送出
        Tracking.logEvent(event = "view_edit_password", params = bundleOf())
        Tracking.view(name = "/users/password/", title = "パスワード再設定")

    }


    override fun onClickResetPassword(view: View) {
        // パスワード再設定API
        Api(this).updateResetPassword(resetPasswordToken?: "",
            viewModel.password.value?: "",
            viewModel.verificationPassword.value?:""){ userId, accessToken ->
                var userSession = UserSession(userId = userId, token = accessToken)
                userSession.store()
                var intent = Intent(this, MapViewActivity::class.java)
                startActivity(intent)
                finish()
        }
    }

    private fun handleIntent(intent: Intent): String {
        val appLinkData: Uri? = intent.data
        return appLinkData!!.lastPathSegment!!.toString()
    }
}

class ResetPasswordViewModel : ViewModel() {
    // パスワード
    val password: MutableLiveData<String> = MutableLiveData()
    val passwordError: ErrorMessageViewModel = ErrorMessageViewModel()
    val verificationPassword: MutableLiveData<String> = MutableLiveData()
    val verificationPasswordError: ErrorMessageViewModel = ErrorMessageViewModel()

    // 登録ボタン押下
    val isChangeButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(password) { result.value = isValid() }
        result.addSource(verificationPassword) { result.value = isValid() }
    }

    // バリデーションルール
    private fun isValid(): Boolean {
        var valid = true
        valid = isValidPassword() && valid
        valid = isValidVerificationPassword() && valid
        return valid
    }



    private fun isValidPassword(): Boolean {
        // URLが有効か判定する
        var valid = true
        val pattern = Pattern.compile("^([a-zA-Z0-9]{6,})\$")
        val alPattern = Pattern.compile("^(.*[A-z]+.*)")
        val numPattern = Pattern.compile("^(.*[0-9]+.*)")

        val hasAlphabet: (str: String) -> Boolean = { str -> alPattern.matcher(str).find() }
        val hasNumeric: (str: String) -> Boolean = { str -> numPattern.matcher(str).find() }

        if (valid && password.value.isNullOrBlank()) {
            passwordError.message.value = null
        } else {
            password.value?.let { pwd ->
                if (valid && !(pattern.matcher(pwd).find())) {
                    valid = false
                    passwordError.message.value =
                        ErikuraApplication.instance.getString(R.string.password_count_error)
                } else if (valid && !(hasAlphabet(pwd) && hasNumeric(pwd))) {
                    valid = false
                    passwordError.message.value =
                        ErikuraApplication.instance.getString(R.string.password_pattern_error)
                } else {
                    valid = true
                    passwordError.message.value = null
                }
            }
        }
        return valid
    }

    private fun isValidVerificationPassword(): Boolean {
        var valid = true

        if (valid && password.value.isNullOrBlank() && verificationPassword.value.isNullOrBlank()) {
            verificationPasswordError.message.value = null
        } else {
            if (valid && !(password.value.equals(verificationPassword.value))) {
                valid = false
                verificationPasswordError.message.value =
                    ErikuraApplication.instance.getString(R.string.password_verificationPassword_match_error)
            } else {
                valid = true
                verificationPasswordError.message.value = null
            }
        }
        return valid
    }
}

interface ResetPasswordEventHandlers {
    fun onClickResetPassword(view: View)
}

