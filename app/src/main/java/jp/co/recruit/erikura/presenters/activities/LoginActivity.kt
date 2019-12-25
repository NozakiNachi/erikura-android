package jp.co.recruit.erikura.presenters.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.Bindable
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity(), LoginEventHandlers {
    private val viewModel: LoginViewModel by lazy {
        ViewModelProvider(this).get(LoginViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val binding: ActivityLoginBinding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
    }

    override fun onClickLogin(view: View) {
        // FIXME: spinner 表示を行う
        // FIXME: email, password が空の場合の対応
        // FIXME: 完了ボタンのカスタマイズ
        Log.v("EMAIL", viewModel.email.value ?: "")
        Log.v("PASS", viewModel.password.value ?: "")
        Api(this).login(viewModel.email.value ?: "", viewModel.password.value ?: "") {
            Log.v("DEBUG", "ログイン成功: userId=${it.userId}")
            // FIXME:　地図画面、チュートリアルへの遷移
            // FIXME:　戻るボタンでの戻り先からは外したい
        }
    }

    override fun onClickReminderLink(view: View) {
        val reminderURLString = BuildConfig.SERVER_BASE_URL + "users/password/new"
        val intent = Intent(this, WebViewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(reminderURLString)
        }
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
}

class LoginViewModel: ViewModel() {
    val email: MutableLiveData<String> = MutableLiveData()
    val password: MutableLiveData<String> = MutableLiveData()

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
