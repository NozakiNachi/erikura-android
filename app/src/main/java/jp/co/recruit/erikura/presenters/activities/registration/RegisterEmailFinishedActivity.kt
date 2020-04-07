package jp.co.recruit.erikura.presenters.activities.registration

import android.os.Bundle
import androidx.core.os.bundleOf
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.presenters.activities.BaseActivity

class RegisterEmailFinishedActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_email_finished)
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event =  "view_temp_register_finish", params = bundleOf())
        Tracking.view(name = "/user/register/pre/completed", title = "仮登録完了画面")
    }
}
