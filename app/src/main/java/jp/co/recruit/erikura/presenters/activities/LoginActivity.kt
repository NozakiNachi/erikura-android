package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityLoginBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.registration.RegisterPhoneActivity
import jp.co.recruit.erikura.presenters.activities.registration.RegisterSmsVerifyActivity
import jp.co.recruit.erikura.presenters.activities.tutorial.PermitLocationActivity
import kotlinx.android.synthetic.main.activity_start.*

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
                Log.v("DEBUG", "SMS認証チェック電話番号： phoneNumber=${user?.phoneNumber}")
                Api(this).smsVerifyCheck(user?.phoneNumber ?:"") { result->
                    if (result) {
                        //SMS認証済みの場合
                        // 地図画面へ遷移します
                        if (ErikuraApplication.instance.isOnboardingDisplayed()) {
                            val intent = Intent(this, MapViewActivity::class.java)
                            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                        }
                        else {
                            // 位置情報の許諾、オンボーディングを表示します
                            Intent(this, PermitLocationActivity::class.java).let { intent ->
                                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                            }
                        }
                    }
                    else {
                        //SMS未認証の場合、認証画面へ遷移します。
                        val intent = Intent(this, RegisterSmsVerifyActivity::class.java)
                        intent.putExtra("requestCode",2)
                        startActivityForResult(intent, 2)
                    }
                }
            }
        }
    }

    override fun onClickReminderLink(view: View) {
        val reminderURLString = BuildConfig.SERVER_BASE_URL + "users/password/new"
        val intent = Intent(this, WebViewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(reminderURLString)
        }
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onClickUnreachLink(view: View) {
        val unreachURLString = BuildConfig.SERVER_BASE_URL + "users/confirmation/new"
        val intent = Intent(this, WebViewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(unreachURLString)
        }
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2 && resultCode == RESULT_OK) {
            // 地図画面へ遷移します
            if (ErikuraApplication.instance.isOnboardingDisplayed()) {
                val intent = Intent(this, MapViewActivity::class.java)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                finish()
            }
            else {
                // 位置情報の許諾、オンボーディングを表示します
                Intent(this, PermitLocationActivity::class.java).let { intent ->
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
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
