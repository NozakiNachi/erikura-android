package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
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
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityLoginBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.registration.SmsVerifyActivity
import jp.co.recruit.erikura.presenters.activities.tutorial.PermitLocationActivity

class LoginActivity : BaseActivity(), LoginEventHandlers {
    private val viewModel: LoginViewModel by lazy {
        ViewModelProvider(this).get(LoginViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        val binding: ActivityLoginBinding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        // FDLで遷移した場合
        ErikuraApplication.instance.removePushUriFromFDL(intent, "/app/link/user/login/")

        viewModel.email.value = ""
        viewModel.password.value = ""
        viewModel.enableAutoLogin.value = true
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_login", params= bundleOf())
        Tracking.view(name= "/user/login", title= "ログイン画面")
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val view = this.currentFocus
        if (view != null) {
            val layout = findViewById<LinearLayout>(R.id.login_layout)
            layout.requestFocus()

            val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(layout.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onClickLogin(view: View) {
        Log.v("EMAIL", viewModel.email.value ?: "")
        Log.v("PASS", viewModel.password.value ?: "")
        Api(this).login(viewModel.email.value ?: "", viewModel.password.value ?: "") { userSession ->
            Log.v("DEBUG", "ログイン成功: userId=${userSession.userId}")
            // 自動ログインが有効になっている場合はセッション情報を永続化します
            if (viewModel.enableAutoLogin.value ?: false) {
                userSession.store()
            }
            Log.v("DEBUG", "SMS認証チェック： userId=${userSession.userId}")
            Api(this).user(){user ->
                userSession.smsVerifyCheck = true
                Log.v("INFO", "SMS認証チェック")
                Api(this).smsVerifyCheck(user?.phoneNumber ?:"") { result->
                    if (result) {
                        //SMS認証済みの場合
                        // 地図画面へ遷移します
                        if (ErikuraApplication.instance.isOnboardingDisplayed()) {
                            var intent = Intent(this, MapViewActivity::class.java)
                            // プッシュ通知、FDLのURLがあるならそちらへ遷移
                            ErikuraApplication.instance.pushUri?.let {
                                intent = Intent(Intent.ACTION_VIEW, it)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            ErikuraApplication.instance.pushUri = null
                        }
                        else {
                            // 位置情報の許諾、オンボーディングを表示します
                            Intent(this, PermitLocationActivity::class.java).let { intent ->
                                startActivity(intent)
                            }
                        }
                    }
                    else {
                        //SMS未認証の場合、認証画面へ遷移します。
                        val intent = Intent(this, SmsVerifyActivity::class.java)
                        intent.putExtra("requestCode",ErikuraApplication.REQUEST_LOGIN_CODE)
                        startActivityForResult(intent, ErikuraApplication.REQUEST_LOGIN_CODE)
                    }
                }
            }
        }
    }

    override fun onClickReminderLink(view: View) {
        Tracking.logEvent(event = "view_password_new", params = bundleOf())
        Tracking.view(
            name = "/user/password/new",
            title = "パスワード再設定メール画面"
        )
        val intent = Intent(this, SendResetPasswordActivity::class.java)
        startActivity(intent)
    }

    override fun onClickUnreachLink(view: View) {
        val unreachURLString = BuildConfig.SERVER_BASE_URL + "users/confirmation/new"
        val intent = Intent(this, WebViewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(unreachURLString)
        }
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val it = Api.userSession
        Log.v("DEBUG", "ログイン成功: userId=${it?.userId}")
        var isSkip: Boolean = false
        var isChangePhoneNumber: Boolean = false
        data?.let {
            isSkip = it.getBooleanExtra("isSkip", false)
            if (!isSkip) {
                isChangePhoneNumber = it.getBooleanExtra("isChangePhoneNumber", false)
            }
        }
        if (requestCode == ErikuraApplication.REQUEST_LOGIN_CODE && resultCode == RESULT_OK) {
            //skipの判定をいれる
            // 地図画面へ遷移します
            if (ErikuraApplication.instance.isOnboardingDisplayed()) {
                var intent = Intent(this, MapViewActivity::class.java)
                // プッシュ通知、FDLのURLがあるならそちらへ遷移
                ErikuraApplication.instance.pushUri?.let {
                    intent = Intent(Intent.ACTION_VIEW, it)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                if (isChangePhoneNumber && !isSkip) {
                    intent.putExtra("onClickChangeUserInformationOnlyPhone", true)
                }
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                finish()
                ErikuraApplication.instance.pushUri = null
            } else {
                // 位置情報の許諾、オンボーディングを表示します
                Intent(this, PermitLocationActivity::class.java).let { intent ->
                    if (isChangePhoneNumber && !isSkip) {
                        intent.putExtra("onClickChangeUserInformationOnlyPhone", true)
                    }
                    startActivity(
                        intent,
                        ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
                    )
                    finish()
                }
            }
        }
    }
}

class LoginViewModel: ViewModel() {
    val email: MutableLiveData<String> = MutableLiveData()
    val password: MutableLiveData<String> = MutableLiveData()
    val enableAutoLogin: MutableLiveData<Boolean> = MutableLiveData()

    val isLoginButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(email) { result.value = isValid() }
        result.addSource(password) { result.value = isValid()  }
    }

    fun isValid(): Boolean {
        return (email.value?.isNotBlank() ?: false) && (password.value?.isNotBlank() ?: false)
    }
}

interface LoginEventHandlers {
    fun onClickLogin(view: View)
    fun onClickReminderLink(view: View)
    fun onClickUnreachLink(view: View)
}
