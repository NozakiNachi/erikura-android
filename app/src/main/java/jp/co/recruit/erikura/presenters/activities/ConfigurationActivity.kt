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
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityConfigurationBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.util.MessageUtils

class ConfigurationActivity : AppCompatActivity(), ConfigurationEventHandlers {

    var user: User = User()

    private val viewModel: ConfigurationViewModel by lazy {
        ViewModelProvider(this).get(ConfigurationViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        val binding: ActivityConfigurationBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_configuration)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        viewModel.email.value = ""
        viewModel.password.value = ""
        viewModel.enableAutoLogin.value = true
    }

//    override fun onClickUnreachLink(view: View) {
//        //
//    }

    override fun onRegistrationLink(view: View) {
        // リンク先の作成
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onAccountRegistration(view: View) {
        // リンク先の作成
        val intent = Intent(this, ConfigurationActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onNotificationSettings(view: View) {
        // リンク先の作成
        val intent = Intent(this, RegisterEmailActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onAboutTheApp(view: View) {
        // リンク先の作成
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onFrequentQuestions(view: View) {
        // リンク先の作成
        val intent = Intent(this, ConfigurationActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onInquiry(view: View) {
        // リンク先の作成
        val intent = Intent(this, RegisterEmailActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onClickLogout(view: View) {
//        MessageUtils.displayLogout(this)
    }
}



    class ConfigurationViewModel: ViewModel() {
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

    interface ConfigurationEventHandlers {
        // 非ログイン対処
        //fun onClickUnreachLink(view: View)
        // 会員情報変更へのリンク
        fun onRegistrationLink(view: View)
        // 口座情報登録・変更へのリンク
        fun onAccountRegistration(view: View)
        // 通知設定へのリンク
        fun onNotificationSettings(view: View)
        // このアプリについてへのリンク
        fun onAboutTheApp(view: View)
        // よくある質問へのリンク
        fun onFrequentQuestions(view: View)
        // 問い合わせへのリンク
        fun onInquiry(view: View)
        // ログアウトへのリンク
        fun onClickLogout(view: View)
    }
