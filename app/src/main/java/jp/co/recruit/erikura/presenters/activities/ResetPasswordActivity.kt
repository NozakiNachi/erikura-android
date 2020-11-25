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
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityResetPasswordBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel
import org.apache.commons.lang.StringUtils
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

        // エラーメッセージは画面生成時は存在しないので表示しない
        viewModel.passwordErrorVisibility.value = View.GONE
        viewModel.verificationPasswordErrorVisibility.value = View.GONE

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
        Tracking.logEvent(event = "view_password_update", params = bundleOf())
        Tracking.view(name = "/user/password/update", title = "パスワード再設定画面")

    }


    override fun onClickResetPassword(view: View) {
        // パスワード再設定API
        Tracking.logEvent(event = "push_password_update", params = bundleOf())
        Tracking.trackUserId(
            name = "push_password_update",
            user = user
        )
        Api(this).updateResetPassword(resetPasswordToken?: "",
            viewModel.password.value?: "",
            viewModel.verificationPassword.value?:""){ userSession ->
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
    val passwordErrorVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(passwordError.message) { message ->
            result.value = if (message == null || StringUtils.isBlank(message)) {
                View.GONE
            }
            else {
                View.VISIBLE
            }
        }
    }
    val verificationPassword: MutableLiveData<String> = MutableLiveData()
    val verificationPasswordError: ErrorMessageViewModel = ErrorMessageViewModel()
    val verificationPasswordErrorVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(verificationPasswordError.message) { message ->
            result.value = if (message == null || StringUtils.isBlank(message)) {
                View.GONE
            }
            else {
                View.VISIBLE
            }
        }
    }

    // 登録ボタン押下
    val isChangeButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(password) { result.value = isValid() }
        result.addSource(verificationPassword) { result.value = isValid() }
    }

    // バリデーションルール
    private fun isValid(): Boolean {
        var valid = true
        //　パスワードと確認用パスワードのバリデーション
        val passwordValidAndErrorMessage = User.isValidPassword(password.value)
        val verificationPassValidAndErrorMessage = User.isValidVerificationPassword(password.value, verificationPassword.value)
        // パスワードと確認用パスワードのエラーメッセージを取得
        passwordError.message.value = passwordValidAndErrorMessage.second
        verificationPasswordError.message.value = verificationPassValidAndErrorMessage.second
        // バリデーション
        valid = passwordValidAndErrorMessage.first && valid
        valid = verificationPassValidAndErrorMessage.first && valid

        return valid
    }
}

interface ResetPasswordEventHandlers {
    fun onClickResetPassword(view: View)
}

