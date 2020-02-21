package jp.co.recruit.erikura.presenters.activities.registration

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jp.co.recruit.erikura.R

class RegisterEmailFinishedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_email_finished)
    }
}
