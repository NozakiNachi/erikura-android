package jp.co.recruit.erikura.presenters.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api

class RegisterJobStatusActivity : AppCompatActivity() {

    var user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_job_status)

        // ユーザ情報を受け取る
        user = intent.getParcelableExtra("user")

    }
}
