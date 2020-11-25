package jp.co.recruit.erikura.presenters.activities.registration

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
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
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityRegisterPasswordBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.StartActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel
import java.util.regex.Pattern


class RegisterPasswordActivity : BaseActivity(),
    RegisterPasswordEventHandlers {
    private val viewModel: RegisterPasswordViewModel by lazy {
        ViewModelProvider(this).get(RegisterPasswordViewModel::class.java)
    }

    val user: User = User()
    var confirmationToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_register_password)

        val binding: ActivityRegisterPasswordBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_password)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
        viewModel.error.message.value = null



        // 仮登録トークン取得
        var uri: Uri? = intent.data
        if (uri?.path == "${BuildConfig.ERIKURA_RELATIVE_URL_ROOT}/api/v1/utils/open_android_app") {
            val path = uri?.getQueryParameter("path")
            uri = Uri.parse("erikura://${path}")
        }
        else if (uri?.path == "/api/v1/utils/open_android_app") {
            val path = uri?.getQueryParameter("path")
            uri = Uri.parse("erikura://${path}")
        }
        confirmationToken = uri?.getQueryParameter("confirmation_token")

        //FDLの場合
        if (intent.data != null && confirmationToken == null){
            // 仮登録トークン取得
            confirmationToken = handleIntent(intent)
        }

        // ワーカ仮登録の確認
        Api(this).registerConfirm(confirmationToken ?:"", onError = {
            Log.v("DEBUG", "ユーザ仮登録確認失敗")
            // 仮登録に失敗しているので、セッション情報をクリアします
            Api.userSession = null
            UserSession.clear()
            // スタート画面へ遷移する
            val intent = Intent(this, StartActivity::class.java)
            if(it != null) {
                val array = it.toTypedArray()
                intent.putExtra("errorMessages", array)
            }
            startActivity(intent)
            finish()
        }) {
            Log.v("DEBUG", "仮登録確認： userId=${it}")
            user.confirmationToken = confirmationToken
        }
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event = "view_register_password", params = bundleOf())
        Tracking.view(name = "/user/register/password", title = "本登録画面（パスワード）")
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val view = this.currentFocus
        if (view != null) {
            val constraintLayout = findViewById<ConstraintLayout>(R.id.register_password_constraintLayout)
            constraintLayout.requestFocus()

            val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(constraintLayout.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onClickNext(view: View) {
        Log.v("PASSWORD", viewModel.password.value ?: "")
        user.password = viewModel.password.value
        val intent: Intent = Intent(this@RegisterPasswordActivity, RegisterNameActivity::class.java)
        intent.putExtra("user", user)
        startActivity(intent)
    }

    override fun backToDefaultActivity() {
        // 会員登録中なので、スタート画面に遷移させます
        Intent(this, StartActivity::class.java)?.let {
            it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(it)
        }
    }

    private fun handleIntent(intent: Intent): String {
        val appLinkData: Uri? = intent.data
        return appLinkData!!.lastPathSegment!!.toString()
    }
}

class RegisterPasswordViewModel: ViewModel() {
    val password: MutableLiveData<String> = MutableLiveData()
    val error: ErrorMessageViewModel = ErrorMessageViewModel()

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(password) {result.value = isValid(it) }
    }

    private fun isValid(password: String?): Boolean {
        var valid = true
        //　パスワードのバリデーション
        val passwordValidAndErrorMessage = User.isValidFirstRegisterPassword(password)
        // パスワードのエラーメッセージを取得
        error.message.value = passwordValidAndErrorMessage.second
        valid = passwordValidAndErrorMessage.first && valid
        return valid
    }
}

interface RegisterPasswordEventHandlers {
    fun onClickNext(view: View)
}