package jp.co.recruit.erikura.presenters.activities

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivitySendedChangeEmailBinding

class SendedChangeEmailActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivitySendedChangeEmailBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_sended_change_email)
        binding.lifecycleOwner = this


        // エラーメッセージを受け取る
        val errorMessages = intent.getStringArrayExtra("errorMessages")
        if (errorMessages != null) {
            Api(this).displayErrorAlert(errorMessages.asList())
        }
    }
}