package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.DialogLogoutBinding


class LogoutActivity : AppCompatActivity(), LogoutEventHandlers {
    private val viewModel: LogoutViewModel by lazy {
        ViewModelProvider(this).get(LogoutViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        val binding: DialogLogoutBinding =
            DataBindingUtil.setContentView(this, R.layout.dialog_logout)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
    }

    override fun onClickLogout(view: View) {
        // FIXME: ログアウト処理
        Api(this).logout() {
            // スタート画面に戻る
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
        // 戻るボタンで戻れないようにする。
    }
}

class LogoutViewModel: ViewModel() {}

interface LogoutEventHandlers {
    fun onClickLogout(view: View)
}
