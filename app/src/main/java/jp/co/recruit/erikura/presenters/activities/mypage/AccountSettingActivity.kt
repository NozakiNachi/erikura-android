package jp.co.recruit.erikura.presenters.activities.mypage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModel
import jp.co.recruit.erikura.R

class AccountSettingActivity : AppCompatActivity(), AccountSettingHandlers {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_setting)
    }
}

class AccountSettingViewModel: ViewModel() {}

interface AccountSettingHandlers {}
