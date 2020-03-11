package jp.co.recruit.erikura.presenters.activities

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.NotificationSetting
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.*
import android.widget.ToggleButton










class NotificationSettingActivity : AppCompatActivity(), NotificationSettingEventHandlers {

    var notificationSetting: NotificationSetting = NotificationSetting()

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
        Api(this).notificationSetting() {
            notificationSetting = it

            viewModel.allowRemindMailReception.value = notificationSetting.allowRemindMailReception
            viewModel.allowInfoMailReception.value = notificationSetting.allowInfoMailReception
            viewModel.allowRemindPushReception.value = notificationSetting.allowRemindPushReception
            viewModel.allowInfoPushReception.value = notificationSetting.allowInfoPushReception

            // 初期表示
            if (viewModel.allowRemindMailReception.value == true) {
                animOff(false, "AllowRemindMailReception")
//                binding.allowRemindMailReception.setVisibility(View.GONE)
            }
            if (viewModel.allowInfoMailReception.value == true) {
                animOff(false, "AllowInfoMailReception")
//                binding.allowInfoMailReception.setVisibility(View.GONE)
            }
            if (viewModel.allowRemindPushReception.value == true) {
                animOff(false, "AllowRemindPushReception")
//                binding.allowRemindPushReception.setVisibility(View.GONE)
            }
            if (viewModel.allowInfoPushReception.value == true) {
                animOff(false, "AllowInfoPushReception")
//                binding.allowInfoPushReception.setVisibility(View.GONE)
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
            animOn(false, "AllowRemindMailReception")
        }else {
            viewModel.allowRemindMailReception.value =true
            notificationSetting.allowRemindMailReception = true
            animOff(false, "AllowRemindMailReception")
        }
    }
    override fun onAllowInfoMailReception(view: View) {
        if (viewModel.allowInfoMailReception.value == true){
            viewModel.allowInfoMailReception.value = false
            notificationSetting.allowInfoMailReception = false
            animOn(false, "AllowInfoMailReception")
        }else {
            viewModel.allowInfoMailReception.value = true
            notificationSetting.allowInfoMailReception = true
            animOff(false, "AllowInfoMailReception")
        }
    }
    override fun onAllowRemindPushReception(view: View) {
        if (viewModel.allowRemindPushReception.value == true){
            viewModel.allowRemindPushReception.value = false
            notificationSetting.allowRemindPushReception = false
            animOn(false, "AllowRemindPushReception")
        }else {
            viewModel.allowRemindPushReception.value = true
            notificationSetting.allowRemindPushReception = true
            animOff(false, "AllowRemindPushReception")
        }
    }
    override fun onAllowInfoPushReception(view: View) {
        if (viewModel.allowInfoPushReception.value == true){
            viewModel.allowInfoPushReception.value = false
            notificationSetting.allowInfoPushReception = false
            animOn(false, "AllowInfoPushReception")
        }else {
            viewModel.allowInfoPushReception.value = true
            notificationSetting.allowInfoPushReception = true
            animOff(false, "AllowInfoPushReception")
        }
    }

    private fun animOff(durationZero: Boolean, actionName: String) {
        val btnAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_btn_on
        )
        val bgWhiteAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_bg_white_on
        )
        val bgGreenAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_bg_green_on
        )

        if (durationZero) {
            btnAnim.setDuration(0)
            bgWhiteAnim.setDuration(0)
            bgGreenAnim.setDuration(0)
        }

        if(actionName == "AllowRemindMailReception") {
            var mTglBtn = findViewById<View>(R.id.allow_remind_mail_reception) as ToggleButton
            var mTglBgWhite = findViewById(R.id.allow_remind_mail_reception_white) as View
            var mTglBgGreen = findViewById(R.id.allow_remind_mail_reception_green) as View

            mTglBtn.startAnimation(btnAnim)
            mTglBgWhite.startAnimation(bgWhiteAnim)
            mTglBgGreen.startAnimation(bgGreenAnim)
        }
        if(actionName == "AllowInfoMailReception") {
            var mTglBtn = findViewById<View>(R.id.allow_info_mail_reception) as ToggleButton
            var mTglBgWhite = findViewById(R.id.allow_info_mail_reception_white) as View
            var mTglBgGreen = findViewById(R.id.allow_info_mail_reception_green) as View

            mTglBtn.startAnimation(btnAnim)
            mTglBgWhite.startAnimation(bgWhiteAnim)
            mTglBgGreen.startAnimation(bgGreenAnim)
        }
        if(actionName == "AllowRemindPushReception") {
            var mTglBtn = findViewById<View>(R.id.allow_remind_push_reception) as ToggleButton
            var mTglBgWhite = findViewById(R.id.allow_remind_push_reception_white) as View
            var mTglBgGreen = findViewById(R.id.allow_remind_push_reception_green) as View

            mTglBtn.startAnimation(btnAnim)
            mTglBgWhite.startAnimation(bgWhiteAnim)
            mTglBgGreen.startAnimation(bgGreenAnim)
        }
        if (actionName == "AllowInfoPushReception") {
            var mTglBtn = findViewById<View>(R.id.allow_info_push_reception) as ToggleButton
            var mTglBgWhite = findViewById(R.id.allow_info_push_reception_white) as View
            var mTglBgGreen = findViewById(R.id.allow_info_push_reception_green) as View

            mTglBtn.startAnimation(btnAnim)
            mTglBgWhite.startAnimation(bgWhiteAnim)
            mTglBgGreen.startAnimation(bgGreenAnim)
        }
    }


    private fun animOn(durationZero: Boolean, actionName: String) {
        val btnAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_btn_off
        )
        val bgWhiteAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_bg_white_off
        )
        val bgGreenAnim = AnimationUtils.loadAnimation(
            applicationContext, R.anim.toggle_bg_green_off
        )

        if (durationZero) {
            btnAnim.setDuration(0)
            bgWhiteAnim.setDuration(0)
            bgGreenAnim.setDuration(0)
        }


        if(actionName == "AllowRemindMailReception") {
            var mTglBtn = findViewById<View>(R.id.allow_remind_mail_reception) as ToggleButton
            var mTglBgWhite = findViewById(R.id.allow_remind_mail_reception_white) as View
            var mTglBgGreen = findViewById(R.id.allow_remind_mail_reception_green) as View

            mTglBtn.startAnimation(btnAnim)
            mTglBgWhite.startAnimation(bgWhiteAnim)
            mTglBgGreen.startAnimation(bgGreenAnim)
        }
        if(actionName == "AllowInfoMailReception") {
            var mTglBtn = findViewById<View>(R.id.allow_info_mail_reception) as ToggleButton
            var mTglBgWhite = findViewById(R.id.allow_info_mail_reception_white) as View
            var mTglBgGreen = findViewById(R.id.allow_info_mail_reception_green) as View

            mTglBtn.startAnimation(btnAnim)
            mTglBgWhite.startAnimation(bgWhiteAnim)
            mTglBgGreen.startAnimation(bgGreenAnim)
        }
        if(actionName == "AllowRemindPushReception") {
            var mTglBtn = findViewById<View>(R.id.allow_remind_push_reception) as ToggleButton
            var mTglBgWhite = findViewById(R.id.allow_remind_push_reception_white) as View
            var mTglBgGreen = findViewById(R.id.allow_remind_push_reception_green) as View

            mTglBtn.startAnimation(btnAnim)
            mTglBgWhite.startAnimation(bgWhiteAnim)
            mTglBgGreen.startAnimation(bgGreenAnim)
        }
        if(actionName == "AllowInfoPushReception") {
            var mTglBtn = findViewById<View>(R.id.allow_info_push_reception) as ToggleButton
            var mTglBgWhite = findViewById(R.id.allow_info_push_reception_white) as View
            var mTglBgGreen = findViewById(R.id.allow_info_push_reception_green) as View

            mTglBtn.startAnimation(btnAnim)
            mTglBgWhite.startAnimation(bgWhiteAnim)
            mTglBgGreen.startAnimation(bgGreenAnim)
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