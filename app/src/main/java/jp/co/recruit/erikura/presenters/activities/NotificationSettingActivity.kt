package jp.co.recruit.erikura.presenters.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.*


class NotificationSettingActivity : AppCompatActivity(), NotificationSettingEventHandlers {
    data class MenuItem(val id: Int, val label: String, val iconDrawableId: Int, val requireLogin: Boolean, val onSelect: () -> Unit)

    private val viewModel: NotificationSettingViewModel by lazy {
        ViewModelProvider(this).get(NotificationSettingViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)


        val binding: ActivityNotificationSettingBinding = DataBindingUtil.setContentView(this, R.layout.activity_notification_setting)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this


    }
}

interface NotificationSettingEventHandlers {
}

class NotificationSettingViewModel: ViewModel() {
}