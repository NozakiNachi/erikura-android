package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
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
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityLoginBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity

class LoginActivity : AppCompatActivity(), LoginEventHandlers {
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

    override fun onClickLogin(view: View) {
        // FIXME: spinner 表示を行う
        Log.v("EMAIL", viewModel.email.value ?: "")
        Log.v("PASS", viewModel.password.value ?: "")
        Api(this).login(viewModel.email.value ?: "", viewModel.password.value ?: "") {
            Log.v("DEBUG", "ログイン成功: userId=${it.userId}")
            // 自動ログインが有効になっている場合はセッション情報を永続化します
            if (viewModel.enableAutoLogin.value ?: false) {
                it.store()
            }
            // 地図画面へ遷移します
            // FIXME: チュートリアルの表示
            val intent = Intent(this, MapViewActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
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
