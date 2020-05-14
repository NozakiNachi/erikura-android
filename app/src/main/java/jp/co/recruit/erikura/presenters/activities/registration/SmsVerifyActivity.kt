package jp.co.recruit.erikura.presenters.activities.registration

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivitySmsVerifyBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.StartActivity
import jp.co.recruit.erikura.presenters.activities.job.ChangeUserInformationOtherThanPhoneFragment
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
    var beforeChangeNewPhoneNumber: String? = null
    var requestCode: Int? = null
    var confirmationToken: String? = null
    var isCameThroughLogin: Boolean = false
    var isMobilePhoneNumber: Boolean? = false
    var isChangeUserInformationOtherThanPhone: Boolean = false
    val pattern = Pattern.compile("^(070|080|090)")

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        val binding: ActivitySmsVerifyBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_sms_verify)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        // ユーザ情報を受け取る
        requestCode = intent.getIntExtra("requestCode", ErikuraApplication.REQUEST_DEFAULT_CODE)
        viewModel.requestCode.value = requestCode
        isCameThroughLogin = intent.getBooleanExtra("isCameThroughLogin", false)
        viewModel.isCameThroughLogin.value = isCameThroughLogin
        isChangeUserInformationOtherThanPhone = intent.getBooleanExtra("onClickChangeUserInformationOtherThanPhone", false)

        if (requestCode == ErikuraApplication.REQUEST_SIGN_UP_CODE || requestCode == ErikuraApplication.REQUEST_CHANGE_USER_INFORMATION) {
            user = intent.getParcelableExtra("user")
            phoneNumber = intent.getStringExtra("phoneNumber")
            confirmationToken = user.confirmationToken
            beforeChangeNewPhoneNumber = intent.getStringExtra("beforeChangeNewPhoneNumber")
            sendSms(pattern, phoneNumber ?:"")
        } else {
            Api(this).user {
                user = it
                phoneNumber = user.phoneNumber
                confirmationToken = user.confirmationToken
                beforeChangeNewPhoneNumber = intent.getStringExtra("beforeChangeNewPhoneNumber")
                sendSms(pattern, phoneNumber ?:"")
            }
        }
    }

    fun sendSms(pattern: Pattern, phoneNumber: String) {
        //携帯番号形式化チェック
        isMobilePhoneNumber = pattern.matcher(phoneNumber ?:"").find()
        if (isMobilePhoneNumber == true) {
            Log.v("DEBUG", "SMS認証メール送信")
            // trueしか返ってこないので送信結果の判定は入れていない
            Api(this).sendSms(confirmationToken ?: "", phoneNumber ?: "") {
                phoneNumber?.let { viewModel.setCaption(it) }
                viewModel.error.message.value = null
                viewModel.isMobilePhoneNumber.value = isMobilePhoneNumber
            }
        } else {
            viewModel.isMobilePhoneNumber.value = isMobilePhoneNumber
        }
    }

    override fun onStart() {
        super.onStart()
        if (isChangeUserInformationOtherThanPhone) {
            val dialog = ChangeUserInformationOtherThanPhoneFragment()
            dialog.show(supportFragmentManager, "ChangeUserInformationOtherThanPhone")
            isChangeUserInformationOtherThanPhone = false
        }
        //SMS認証画面表示のトラッキングの送出
        Tracking.logEvent(event= "view_sms_verify", params= bundleOf())
        Tracking.view(name= "/user/view_sms_verify", title= "SMS認証画面")
    }

    override fun onClickAuthenticate(view: View) {
        Log.v("DEBUG", "SMS認証")
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
            intent.putExtra("phoneNumber", phoneNumber)
            if (isCameThroughLogin) {
                intent.putExtra("isCameThroughLogin", isCameThroughLogin)
            }
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun onClickPassCodeResend(view: View) {
        Log.v("DEBUG", "SMS認証メール送信")
        // trueしか返ってこないので送信結果の判定は入れていない
        Api(this).sendSms(confirmationToken ?: "", phoneNumber ?: "") {
            phoneNumber?.let { viewModel.setCaption(it) }
        }
    }

    override fun onClickRegisterPhone(view: View) {
        //本登録の電話番号画面と会員情報変更画面のどちらかへ遷移する
        when (requestCode) {
            ErikuraApplication.REQUEST_SIGN_UP_CODE -> {
                val intent = Intent(this, RegisterPhoneActivity::class.java)
                intent.putExtra("user", user)
                intent.putExtra("requestCode", ErikuraApplication.REQUEST_SIGN_UP_CODE)
                startActivityForResult(intent, ErikuraApplication.REQUEST_SIGN_UP_CODE)
            }
            ErikuraApplication.REQUEST_LOGIN_CODE -> {
                val intent = Intent(this, ChangeUserInformationActivity::class.java)
                intent.putExtra("user", user)
                if (beforeChangeNewPhoneNumber != null) {
                    intent.putExtra("beforeChangeNewPhoneNumber", beforeChangeNewPhoneNumber)
                }
                intent.putExtra("requestCode", ErikuraApplication.REQUEST_LOGIN_CODE)
                //ログイン経由で番号を編集する場合地図画面へ遷移させるフラグを付けます。
                intent.putExtra("isCameThroughLogin", true)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
            else -> {
                val intent = Intent(this, ChangeUserInformationActivity::class.java)
                intent.putExtra("user", user)
                if (beforeChangeNewPhoneNumber != null) {
                    intent.putExtra("beforeChangeNewPhoneNumber", beforeChangeNewPhoneNumber)
                }
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
            Tracking.logEvent(event = "logout", params = bundleOf())
            deletedSession?.let {
                it.user?.let { user ->
                    Tracking.identify(user = user, status = "logout")
                }
            }

            // ページ参照のトラッキングの送出
            Tracking.logEvent(event = "view_logout", params = bundleOf())
            Tracking.view(name = "/mypage/logout", title = "ログアウト完了画面")

            // スタート画面に戻る
            val intent = Intent(this, StartActivity::class.java)
            // 戻るボタンの無効化
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    override fun onClickSkip(view: View) {
        //SMS認証せずに遷移します。
        val intent: Intent = Intent()
        intent.putExtra("user", user)
        intent.putExtra("requestCode", requestCode)
        if (isCameThroughLogin) {
            intent.putExtra("isCameThroughLogin", isCameThroughLogin)
        }
        setResult(RESULT_OK, intent)
        finish()
        // SMS認証スキップのトラッキングの送出
        Tracking.logEvent(event = "skip_sms_verify", params = bundleOf())
        Tracking.skipSmsVerify(name = "skip_sms_verify", user = user)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ErikuraApplication.REQUEST_SIGN_UP_CODE && resultCode == RESULT_OK) {
            data?.let{
                user = it.getParcelableExtra("user")
            }
            data?.getStringExtra("phoneNumber")?.let { newPhoneNumber ->
                if (newPhoneNumber != phoneNumber) {
                    phoneNumber = newPhoneNumber
                    Log.v("INFO", "SMS認証メール送信")
                    // trueしか返ってこないので送信結果の判定は入れていない
                    Api(this).sendSms(confirmationToken ?: "", phoneNumber ?: "") {
                        phoneNumber?.let { viewModel.setCaption(it) }
                        viewModel.error.message.value = null
                    }
                }
            }
        }
    }
}

class SmsVerifyViewModel : ViewModel() {
    val requestCode = MutableLiveData<Int>()
    val isMobilePhoneNumber = MutableLiveData<Boolean>()
    val isCameThroughLogin = MutableLiveData<Boolean>()

    val passCode: MutableLiveData<String> = MutableLiveData()
    val error: ErrorMessageViewModel = ErrorMessageViewModel()
    var caption: MutableLiveData<String> = MutableLiveData()

    val sendSmsVerifyVisible: MutableLiveData<Int> = MediatorLiveData<Int>().also { result ->
        result.addSource(isMobilePhoneNumber) {
            if (isMobilePhoneNumber.value == true ) {
                result.value =View.VISIBLE
            } else {
                result.value = View.GONE
            }
        }
    }

    val notSendSmsVerifyVisible: MutableLiveData<Int> = MediatorLiveData<Int>().also { result ->
        result.addSource(isMobilePhoneNumber) {
            if (isMobilePhoneNumber.value == true ) {
                result.value =View.GONE
            } else {
               result.value = View.VISIBLE
            }
        }
    }

    val isAuthenticateButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(passCode) { result.value = isValid() }
    }

    val skipButtonVisible: MutableLiveData<Int> = MediatorLiveData<Int>().also { result ->
        result.addSource(isCameThroughLogin) {
            result.addSource(requestCode) {
                if (isCameThroughLogin.value == true || requestCode.value == ErikuraApplication.REQUEST_LOGIN_CODE) {
                    result.value =View.VISIBLE
                } else {
                    result.value =View.GONE
                }
            }
        }
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
        } else if (valid && !(passCode.value?.length ?: 0 == 8)) {
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
        var phoneUtil = PhoneNumberUtil.getInstance()
        try {
            var pn = phoneUtil.parse(phoneNumber, "JP")
            var formatPhoneNumber = phoneUtil.format(pn, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
            caption.value = String.format(
                "ご登録の電話番号%sにパスコード記載のショートメッセージをお送りしました。ご確認いただき、10分以内に下記にご入力ください。",
                formatPhoneNumber
            )
        } catch (e: NumberParseException) {
            Log.e("ERROR", e.message, e)
        }
    }
}

interface SmsVerifyEventHandlers {
    fun onClickAuthenticate(view: View)
    fun onClickPassCodeResend(view: View)
    fun onClickRegisterPhone(view: View)
    fun onClickSkip(view: View)
}
