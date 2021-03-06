package jp.co.recruit.erikura.presenters.activities.mypage

import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.NotificationSetting
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityNotificationSettingBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity


class NotificationSettingActivity : BaseActivity(),
    NotificationSettingEventHandlers {
    private val viewModel: NotificationSettingViewModel by lazy {
        ViewModelProvider(this).get(NotificationSettingViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        val binding: ActivityNotificationSettingBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_notification_setting)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        // ユーザーの現在の通知設定を取得
        Api(this).notificationSetting() { notificationSetting ->
            // メール通知
            viewModel.allowRemindMailReception.value = notificationSetting.allowRemindMailReception
            viewModel.allowInfoMailReception.value = notificationSetting.allowInfoMailReception
            viewModel.allowReopenMailReception.value = notificationSetting.allowReopenMailReception
            viewModel.allowCommentedMailReception.value = notificationSetting.allowCommentedMailReception
            viewModel.allowLikedMailReception.value = notificationSetting.allowLikedMailReception

            // プッシュ通知
            viewModel.allowRemindPushReception.value = notificationSetting.allowRemindPushReception
            viewModel.allowInfoPushReception.value = notificationSetting.allowInfoPushReception
            viewModel.allowReopenPushReception.value = notificationSetting.allowReopenPushReception
            viewModel.allowCommentedPushReception.value = notificationSetting.allowCommentedPushReception
            viewModel.allowLikedPushReception.value = notificationSetting.allowLikedPushReception


            //API処理実行後に実施する
            ErikuraApplication.instance.removePushUriFromFDL(intent, "/app/link/mypage/notification_settings")
        }
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_notification", params= bundleOf())
        Tracking.view(name= "/mypage/notification_settings", title= "通知設定画面")
    }

    // 戻るボタン押下時に通知設定を保存
    override fun onPause() {
        super.onPause()
        val notificationSetting = NotificationSetting()
        notificationSetting.allowRemindMailReception = viewModel.allowRemindMailReception.value ?: false
        notificationSetting.allowInfoMailReception = viewModel.allowInfoMailReception.value ?: false
        notificationSetting.allowReopenMailReception = viewModel.allowReopenMailReception.value ?: false
        notificationSetting.allowCommentedMailReception = viewModel.allowCommentedMailReception.value ?: false
        notificationSetting.allowLikedMailReception = viewModel.allowLikedMailReception.value ?: false

        notificationSetting.allowRemindPushReception = viewModel.allowRemindPushReception.value ?: false
        notificationSetting.allowInfoPushReception = viewModel.allowInfoPushReception.value ?: false
        notificationSetting.allowReopenPushReception = viewModel.allowReopenPushReception.value ?: false
        notificationSetting.allowCommentedPushReception = viewModel.allowCommentedPushReception.value ?: false
        notificationSetting.allowLikedPushReception = viewModel.allowLikedPushReception.value ?: false

        Api(this).updateNotificationSetting(notificationSetting) {}
    }
}

interface NotificationSettingEventHandlers {
}

class NotificationSettingViewModel: ViewModel() {
    val allowRemindMailReception: MutableLiveData<Boolean> = MutableLiveData()
    val allowInfoMailReception: MutableLiveData<Boolean> = MutableLiveData()
    val allowReopenMailReception: MutableLiveData<Boolean> = MutableLiveData()
    val allowCommentedMailReception: MutableLiveData<Boolean> = MutableLiveData()
    val allowLikedMailReception: MutableLiveData<Boolean> = MutableLiveData()
    val allowRemindPushReception: MutableLiveData<Boolean> = MutableLiveData()
    val allowInfoPushReception: MutableLiveData<Boolean> = MutableLiveData()
    val allowReopenPushReception: MutableLiveData<Boolean> = MutableLiveData()
    val allowCommentedPushReception: MutableLiveData<Boolean> = MutableLiveData()
    val allowLikedPushReception: MutableLiveData<Boolean> = MutableLiveData()
}