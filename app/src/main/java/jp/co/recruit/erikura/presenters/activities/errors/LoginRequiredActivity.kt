package jp.co.recruit.erikura.presenters.activities.errors

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.databinding.ActivityLoginRequiredBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.LoginActivity
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.mypage.MypageActivity
import jp.co.recruit.erikura.presenters.activities.registration.RegisterEmailActivity

class LoginRequiredActivity : BaseActivity(), LoginRequiredHandlers {
    private val viewModel: LoginRequiredViewModel by lazy {
        ViewModelProvider(this).get(LoginRequiredViewModel::class.java)
    }

    var fromMypage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityLoginRequiredBinding = DataBindingUtil.setContentView(this, R.layout.activity_login_required)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        fromMypage = intent.getBooleanExtra(MypageActivity.FROM_MYPAGE_KEY, false)
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "error_login", params= bundleOf())
        Tracking.track(name= "error_login")
    }

    override fun onClickRegisterButton(view: View) {
        val intent = Intent(this, RegisterEmailActivity::class.java)
        startActivity(intent)
    }

    override fun onClickLoginButton(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (fromMypage) {
            val intent = Intent(this, MypageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
        else {
            val intent = Intent(this, MapViewActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}

class LoginRequiredViewModel: ViewModel() {
}

interface LoginRequiredHandlers {
    fun onClickRegisterButton(view: View)
    fun onClickLoginButton(view: View)
}