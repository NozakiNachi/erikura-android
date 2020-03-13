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
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.presenters.fragments.ToggleSwitchView


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


            // 初期表示
            if (viewModel.allowRemindMailReception.value == true) {
                binding.allowRemindMailReception.isChecked = true
            }else{
                binding.allowRemindMailReception.isChecked = false
            }
            if (viewModel.allowInfoMailReception.value == true) {
                binding.allowInfoMailReception.isChecked = true
            }else{
                binding.allowInfoMailReception.isChecked = false
            }
//            if (viewModel.allowRemindPushReception.value == true) {
//                animOn("AllowRemindPushReception")
//            }else{
//                binding.allowRemindPushReceptionGreen.setVisibility(View.GONE)
//            }
//            if (viewModel.allowInfoPushReception.value == true) {
//                animOn("AllowInfoPushReception")
//            }else{
//                binding.allowInfoPushReceptionGreen.setVisibility(View.GONE)
//            }
        }
    }

    // 戻るボタン押下時に通知設定を保存
    override fun onPause() {
        super.onPause()
        notificationSetting.allowRemindMailReception = true
        Api(this).updateNotificationSetting(notificationSetting) {}
    }

    // 通知の登録値切替
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
    override fun onAllowReopenMailReception(view: View) {
        if (viewModel.allowReopenMailReception.value == true){
            viewModel.allowReopenMailReception.value = false
            notificationSetting.allowReopenMailReception = false
        }else {
            viewModel.allowReopenMailReception.value = true
            notificationSetting.allowReopenMailReception = true
        }
    }
    override fun onAllowCommentedMailReception(view: View) {
        if (viewModel.allowCommentedMailReception.value == true){
            viewModel.allowCommentedMailReception.value = false
            notificationSetting.allowCommentedMailReception = false
        }else {
            viewModel.allowCommentedMailReception.value = true
            notificationSetting.allowCommentedMailReception = true
        }
    }
    override fun onAllowLikedMailReception(view: View) {
        if (viewModel.allowLikedMailReception.value == true){
            viewModel.allowLikedMailReception.value = false
            notificationSetting.allowLikedMailReception = false
        }else {
            viewModel.allowLikedMailReception.value = true
            notificationSetting.allowLikedMailReception = true
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
    override fun onAllowReopenPushReception(view: View) {
        if (viewModel.allowReopenPushReception.value == true){
            viewModel.allowReopenPushReception.value = false
            notificationSetting.allowReopenPushReception = false
        }else {
            viewModel.allowReopenPushReception.value = true
            notificationSetting.allowReopenPushReception = true
        }
    }
    override fun onAllowCommentedPushReception(view: View) {
        if (viewModel.allowCommentedPushReception.value == true){
            viewModel.allowCommentedPushReception.value = false
            notificationSetting.allowReopenPushReception = false
        }else {
            viewModel.allowCommentedPushReception.value = true
            notificationSetting.allowCommentedPushReception = true
        }
    }
    override fun onAllowLikedPushReception(view: View) {
        if (viewModel.allowLikedPushReception.value == true){
            viewModel.allowLikedPushReception.value = false
            notificationSetting.allowLikedPushReception = false
        }else {
            viewModel.allowLikedPushReception.value = true
            notificationSetting.allowLikedPushReception = true
        }
    }

    fun animOn() {
        val btnAnim = AnimationUtils.loadAnimation(
            ErikuraApplication.applicationContext, R.anim.toggle_btn_on
        )
        val bgWhiteAnim = AnimationUtils.loadAnimation(
            ErikuraApplication.applicationContext, R.anim.toggle_bg_white_on
        )
        val bgGreenAnim = AnimationUtils.loadAnimation(
            ErikuraApplication.applicationContext, R.anim.toggle_bg_green_on
        )

        var mTglBtn = findViewById<View>(R.id.notification) as ToggleButton
        var mTglBgWhite = findViewById(R.id.white) as View
        var mTglBgGreen = findViewById(R.id.green) as View

        mTglBtn.startAnimation(btnAnim)
        mTglBgWhite.startAnimation(bgWhiteAnim)
        mTglBgGreen.startAnimation(bgGreenAnim)
    }

    fun animOff() {
        val btnAnim = AnimationUtils.loadAnimation(
            ErikuraApplication.applicationContext, R.anim.toggle_btn_off
        )
        val bgWhiteAnim = AnimationUtils.loadAnimation(
            ErikuraApplication.applicationContext, R.anim.toggle_bg_white_off
        )
        val bgGreenAnim = AnimationUtils.loadAnimation(
            ErikuraApplication.applicationContext, R.anim.toggle_bg_green_off
        )

        var mTglBtn = findViewById<View>(R.id.notification) as ToggleButton
        var mTglBgWhite = findViewById(R.id.white) as View
        var mTglBgGreen = findViewById(R.id.green) as View

        mTglBtn.startAnimation(btnAnim)
        mTglBgWhite.startAnimation(bgWhiteAnim)
        mTglBgGreen.startAnimation(bgGreenAnim)
    }
}

interface NotificationSettingEventHandlers {
    fun onAllowRemindMailReception(view: View)
    fun onAllowInfoMailReception(view: View)
    fun onAllowReopenMailReception(view: View)
    fun onAllowCommentedMailReception(view: View)
    fun onAllowLikedMailReception(view: View)
    fun onAllowRemindPushReception(view: View)
    fun onAllowInfoPushReception(view: View)
    fun onAllowReopenPushReception(view: View)
    fun onAllowCommentedPushReception(view: View)
    fun onAllowLikedPushReception(view: View)
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