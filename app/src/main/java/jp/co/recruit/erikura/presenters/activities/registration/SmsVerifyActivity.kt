package jp.co.recruit.erikura.presenters.activities.registration

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivitySmsVerifyBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.StartActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ChangeUserInformationActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel
import java.util.regex.Pattern

class SmsVerifyActivity : BaseActivity(),
    SmsVerifyEventHandlers {
    private val viewModel: SmsVerifyViewModel by lazy {
        ViewModelProvider(this).get(SmsVerifyViewModel::class.java)
    }

    var user: User = User()
    var phoneNumber: String? = null
    var requestCode: Int? = null
    var confirmationToken: String? = null
    var isCameThroughLogin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        val binding: ActivitySmsVerifyBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_sms_verify)
        var logoutButton =  findViewById<TextView>(R.id.logout_button)
        if (requestCode == ErikuraApplication.REQUEST_LOGIN_CODE) {
            logoutButton.setVisibility(View.VISIBLE)
        } else {
            logoutButton.setVisibility(View.GONE)
        }
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        // ユーザ情報を受け取る
        requestCode = intent.getIntExtra("requestCode", 0)
        if (requestCode == ErikuraApplication.REQUEST_SIGN_UP_CODE || requestCode == ErikuraApplication.REQUEST_CHANGE_USER_INFORMATION) {
            user = intent.getParcelableExtra("user")
            phoneNumber = intent.getStringExtra("phoneNumber")
        } else {
            Api(this).user {
                user = it
                phoneNumber = user.phoneNumber
            }
        }
        confirmationToken = user.confirmationToken
        isCameThroughLogin = intent.getBooleanExtra("isCameThroughLogin",false)

        Log.v("DEBUG", "SMS認証メール送信： phoneNumber=${phoneNumber}")
        // TODO 現段階ではresultはtrueしか返ってこないので送信結果の判定は入れていない
        Api(this).sendSms(confirmationToken ?: "", phoneNumber ?: "") {
            phoneNumber?.let { viewModel.setCaption(it) }
            viewModel.error.message.value = null
        }
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event = "view_register_sms_verify", params = bundleOf())
        Tracking.view(name = "/user/register/sms_verify", title = "SMS認証")
    }

    override fun onClickAuthenticate(view: View) {
        Log.v("DEBUG", "SMS認証： phoneNumber=${phoneNumber}")
        //trueしか返ってこないので認証結果の判定は入れていない
        Api(this).smsVerify(
            confirmationToken ?: "",
            phoneNumber ?: "",
            viewModel.passCode.value ?: ""
        ) {
            //認証成功後 onActivityResultへ飛ぶ
            val intent: Intent = Intent()
            intent.putExtra("user", user)
            intent.putExtra("requestCode", requestCode)
            if (isCameThroughLogin){
                intent.putExtra("isCameThroughLogin", isCameThroughLogin)
            }
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun onClickPassCodeResend(view: View) {
        Log.v("DEBUG", "SMS認証メール送信： phoneNumber=${phoneNumber}")
        // trueしか返ってこないので送信結果の判定は入れていない
        Api(this).sendSms(confirmationToken ?: "", phoneNumber ?: "") {
            phoneNumber?.let { viewModel.setCaption(it) }
        }
    }

    override fun onClickRegisterPhone(view: View) {
        //本登録の電話番号画面と会員情報変更画面のどちらかへ遷移する
        when(requestCode) {
            ErikuraApplication.REQUEST_SIGN_UP_CODE -> {
                val intent = Intent(this, RegisterPhoneActivity::class.java)
                intent.putExtra("user", user)
                intent.putExtra("requestCode", requestCode!!)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
            ErikuraApplication.REQUEST_LOGIN_CODE -> {
                val intent = Intent(this, ChangeUserInformationActivity::class.java)
                intent.putExtra("user", user)
                intent.putExtra("requestCode", requestCode!!)
                //ログイン経由で番号を編集する場合地図画面へ遷移させるフラグを付けます。
                intent.putExtra("isCameThroughLogin", true)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
            else -> {
                val intent = Intent(this, ChangeUserInformationActivity::class.java)
                intent.putExtra("user", user)
                intent.putExtra("requestCode", requestCode)
                if (isCameThroughLogin) {
                    intent.putExtra("isCameThroughLogin", isCameThroughLogin)
                }
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
        }
    }

    override fun onBackPressed() {
        //ログイン、自動ログインから遷移してきた場合、戻るボタンを制御します。
        if (requestCode == ErikuraApplication.REQUEST_LOGIN_CODE) {
            Logout()
        } else {
            //　その他からの遷移は通常遷移
            super.onBackPressed()
        }
    }

    fun Logout() {
        Api(this).logout() { deletedSession ->
            // ログアウトのトラッキングの送出
            Tracking.logEvent(event= "logout", params= bundleOf())
            deletedSession?.let {
                it.user?.let { user ->
                    Tracking.identify(user= user, status= "logout")
                }
            }

            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_logout", params= bundleOf())
            Tracking.view(name= "/mypage/logout", title= "ログアウト完了画面")

            // スタート画面に戻る
            val intent = Intent(this, StartActivity::class.java)
            // 戻るボタンの無効化
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    override fun onClickLogout(view: View) {
        Api(this).logout() { deletedSession ->
            // ログアウトのトラッキングの送出
            Tracking.logEvent(event= "logout", params= bundleOf())
            deletedSession?.let {
                it.user?.let { user ->
                    Tracking.identify(user= user, status= "logout")
                }
            }

            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_logout", params= bundleOf())
            Tracking.view(name= "/mypage/logout", title= "ログアウト完了画面")

            // スタート画面に戻る
            val intent = Intent(this, StartActivity::class.java)
            // 戻るボタンの無効化
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }
}

class SmsVerifyViewModel : ViewModel() {
    val passCode: MutableLiveData<String> = MutableLiveData()
    val error: ErrorMessageViewModel = ErrorMessageViewModel()
    var caption: MutableLiveData<String> = MutableLiveData()

    val isAuthenticateButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(passCode) { result.value = isValid() }
    }

    private fun isValid(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([0-9])")

        if (valid && passCode.value?.isBlank() != false) {
            valid = false
            error.message.value = null
        } else if (valid && !(pattern.matcher(passCode.value).find())) {
            valid = false
            error.message.value =
                ErikuraApplication.instance.getString(R.string.passcode_pattern_error)
        } else if (valid && !(passCode.value?.length ?: 0 == 4)) {
            valid = false
            error.message.value =
                ErikuraApplication.instance.getString(R.string.passcode_count_error)
        } else {
            valid = true
            error.message.value = null
        }

        return valid
    }

    fun setCaption(phoneNumber: String) {
        caption.value = String.format(
            "ご登録の電話番号%sにパスコード記載のSMSメッセージをお送りしました。ご確認いただき、10分以内に下記にご入力ください。",
            phoneNumber
        )
    }
}

interface SmsVerifyEventHandlers {
    fun onClickAuthenticate(view: View)
    fun onClickPassCodeResend(view: View)
    fun onClickRegisterPhone(view: View)
    fun onClickLogout(view: View)
}
