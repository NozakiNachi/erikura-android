package jp.co.recruit.erikura.presenters.activities.registration

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.ActivityRegisterFinishedBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.mypage.UpdateIdentityActivity
import jp.co.recruit.erikura.presenters.activities.tutorial.PermitLocationActivity

class RegisterFinishedActivity : BaseActivity(),
    RegisterFinishedEventHandlers {

    var user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_finished)

        // ユーザ情報を受け取る
        user = intent.getParcelableExtra("user")

        val binding: ActivityRegisterFinishedBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_finished)
        binding.lifecycleOwner = this
        binding.handlers = this
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_register_finish", params= bundleOf())
        Tracking.view(name= "/user/register/completed", title= "本登録画面（完了）")
    }

    override fun onClickGo(view: View) {
        // 地図画面へ遷移します
        if (ErikuraApplication.instance.isOnboardingDisplayed()) {
            // 地図画面へ遷移
            val intent = Intent(this, MapViewActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
        else {
            // 位置情報の許諾、オンボーディングを表示します
            Intent(this, PermitLocationActivity::class.java).let { intent ->
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }
    }

    override fun onClickUpdateIdentity(view: View) {
        //本人確認情報入力画面へ遷移します
        val intent = Intent(this, UpdateIdentityActivity::class.java)
        intent.putExtra(ErikuraApplication.FROM_WHERE, ErikuraApplication.FROM_REGISTER)
        intent.putExtra("user", user)
        startActivity(intent)
    }
}

interface RegisterFinishedEventHandlers {
    fun onClickGo(view: View)
    fun onClickUpdateIdentity(view: View)
}
