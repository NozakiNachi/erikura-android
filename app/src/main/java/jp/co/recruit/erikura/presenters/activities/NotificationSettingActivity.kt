package jp.co.recruit.erikura.presenters.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.NotificationSetting
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.*
import kotlinx.android.synthetic.main.activity_notification_setting.*


class NotificationSettingActivity : AppCompatActivity(), NotificationSettingEventHandlers {

    var notificationSetting: NotificationSetting = NotificationSetting()

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

        // ユーザーの現在の通知設定を取得
        Api(this).notificationSetting() {
            notificationSetting = it

            viewModel.allowRemindMailReception.value = notificationSetting.allowRemindMailReception
            viewModel.allowInfoMailReception.value = notificationSetting.allowInfoMailReception
            viewModel.allowRemindPushReception.value = notificationSetting.allowRemindPushReception
            viewModel.allowInfoPushReception.value = notificationSetting.allowInfoPushReception

            // 初期表示
            if (viewModel.allowRemindMailReception.value == true) {
                binding.allowRemindMailReception.isChecked = true
            }
            if (viewModel.allowInfoMailReception.value == true) {
                binding.allowInfoMailReception.isChecked = true
            }
            if (viewModel.allowRemindPushReception.value == true) {
                binding.allowRemindPushReception.isChecked = true
            }
            if (viewModel.allowInfoPushReception.value == true) {
                binding.allowInfoPushReception.isChecked = true
            }
        }
    }

    // 戻るボタン押下時に通知設定を保存
    override fun onPause() {
        super.onPause()
        Api(this).updateNotificationSetting(notificationSetting) {}
    }

    // 通知のスイッチ切替
    override fun onAllowRemindMailReception(view: View) {
        if (viewModel.allowRemindMailReception.value == true){
            viewModel.allowRemindMailReception.value = false
            notificationSetting.allowRemindMailReception = false
        }else {
            viewModel.allowRemindMailReception.value =true
            notificationSetting.allowRemindMailReception = true
        }
    }
    override fun onAllowInfoMailReception(view: View) {
        if (viewModel.allowInfoMailReception.value == true){
            viewModel.allowInfoMailReception.value = false
            notificationSetting.allowInfoMailReception = false
        }else {
            viewModel.allowInfoMailReception.value = true
            notificationSetting.allowInfoMailReception = true
        }
    }
    override fun onAllowRemindPushReception(view: View) {
        if (viewModel.allowRemindPushReception.value == true){
            viewModel.allowRemindPushReception.value = false
            notificationSetting.allowRemindPushReception = false
        }else {
            viewModel.allowRemindPushReception.value = true
            notificationSetting.allowRemindPushReception = true
        }
    }
    override fun onAllowInfoPushReception(view: View) {
        if (viewModel.allowInfoPushReception.value == true){
            viewModel.allowInfoPushReception.value = false
            notificationSetting.allowInfoPushReception = false
        }else {
            viewModel.allowInfoPushReception.value = true
            notificationSetting.allowInfoPushReception = true
        }
    }
}

interface NotificationSettingEventHandlers {
    fun onAllowRemindMailReception(view: View)
    fun onAllowInfoMailReception(view: View)
    fun onAllowRemindPushReception(view: View)
    fun onAllowInfoPushReception(view: View)
}

class NotificationSettingViewModel: ViewModel() {
    val allowRemindMailReception: MutableLiveData<Boolean> = MutableLiveData()
    val allowInfoMailReception: MutableLiveData<Boolean> = MutableLiveData()
    val allowRemindPushReception: MutableLiveData<Boolean> = MutableLiveData()
    val allowInfoPushReception: MutableLiveData<Boolean> = MutableLiveData()
}