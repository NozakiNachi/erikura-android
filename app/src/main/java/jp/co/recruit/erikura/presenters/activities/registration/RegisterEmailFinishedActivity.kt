package jp.co.recruit.erikura.presenters.activities.registration

import android.os.Bundle
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.presenters.activities.BaseActivity

class RegisterEmailFinishedActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_email_finished)
    }
}
