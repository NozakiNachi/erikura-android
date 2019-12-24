package jp.co.recruit.erikura.presenters.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import jp.co.recruit.erikura.R

class RegisterEmailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_email)
    }
}
