package jp.co.recruit.erikura.presenters.activities.errors

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.ActivityLoginRequiredBinding
import jp.co.recruit.erikura.presenters.activities.LoginActivity
import jp.co.recruit.erikura.presenters.activities.RegisterEmailActivity

class LoginRequiredActivity : AppCompatActivity(), LoginRequiredHandlers {
    private val viewModel: LoginRequiredViewModel by lazy {
        ViewModelProvider(this).get(LoginRequiredViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityLoginRequiredBinding = DataBindingUtil.setContentView(this, R.layout.activity_login_required)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
    }

    override fun onClickRegisterButton(view: View) {
        val intent = Intent(this, RegisterEmailActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onClickLoginButton(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }
    // FIXME: 戻るボタンが押下された場合の振る舞いを修正する
}

class LoginRequiredViewModel: ViewModel() {
}

interface LoginRequiredHandlers {
    fun onClickRegisterButton(view: View)
    fun onClickLoginButton(view: View)
}