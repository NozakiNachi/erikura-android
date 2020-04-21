package jp.co.recruit.erikura.presenters.activities.registration

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityRegisterSmsVerifyBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel
import java.util.regex.Pattern

class RegisterSmsVerifyActivity : BaseActivity(),
    RegisterSmsVerifyEventHandlers {
    private val viewModel: RegisterSmsVerifyViewModel by lazy {
        ViewModelProvider(this).get(RegisterSmsVerifyViewModel::class.java)
    }

    var user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        // ユーザ情報を受け取る
        user = intent.getParcelableExtra("user")

        //SMS認証送信メールAPIを呼び出す

        val binding: ActivityRegisterSmsVerifyBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_sms_verify)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
        viewModel.error.message.value = null
    }

    override fun onStart() {
        super.onStart()
        var caption = findViewById<TextView>(R.id.registerSmsVerify_caption)
        caption.setText(makeCaption(user.phoneNumber.toString()))
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_register_sms_verify", params= bundleOf())
        Tracking.view(name= "/user/register/sms_verify", title= "SMS認証")
    }

    override fun onClickAuthenticate(view: View) {
        Log.v("PHONE", viewModel.pass_code.value ?: "")


        //FIXME SMS認証APIを呼び出す
//        val intent: Intent = Intent(this@RegisterSmsVerifyActivity, RegisterJobStatusActivity::class.java)
//        intent.putExtra("user", user)
//        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        //FIXME 認証成功後　本登録完了画面を表示
        // ユーザ登録Apiの呼び出し
//        Api(this).initialUpdateUser(user) {
//            Log.v("DEBUG", "ユーザ登録： userSEssion=${it}")
//            // 登録完了画面へ遷移
//            val intent: Intent = Intent(this@RegisterWishWorkActivity, RegisterFinishedActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
//            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
//
//            // 登録完了のトラッキングの送出
//            Tracking.logEvent(event= "signup", params= bundleOf(Pair("user_id", it.userId)))
//            Tracking.identify(user= user, status= "login")
//            Tracking.logCompleteRegistrationEvent()
    }

    override fun onClickPassCodeResend(view: View) {
        //FIXME　SMS認証送信APIを呼ぶ
//        val termsOfServiceURLString = BuildConfig.SERVER_BASE_URL + "/pdf/terms_of_service.pdf"
//        val intent = Intent(this, WebViewActivity::class.java).apply {
//            action = Intent.ACTION_VIEW
//            data = Uri.parse(termsOfServiceURLString)
//        }
//        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onClickRegisterPhone(view: View) {
        val intent = Intent(this, RegisterPhoneActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    private fun makeCaption(phoneNumber: String): String {
        var str = "ご登録の電話番号%sにパスコード記載のSMSメッセージをお送りしました。ご確認いただき、10分以内に下記にご入力ください。".format(phoneNumber)
        return str
    }
}

class RegisterSmsVerifyViewModel: ViewModel() {
    val pass_code: MutableLiveData<String> = MutableLiveData()
    val error: ErrorMessageViewModel = ErrorMessageViewModel()

    private fun isValid(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([0-9])")

        if (valid && pass_code.value?.isBlank() ?:true) {
            valid = false
            error.message.value = null
        }else if(valid && !(pattern.matcher(pass_code.value).find())) {
            valid = false
            error.message.value = ErikuraApplication.instance.getString(R.string.passcode_pattern_error)
        }else if(valid && !(pass_code.value?.length ?: 0 == 4)) {
            valid = false
            error.message.value = ErikuraApplication.instance.getString(R.string.passcode_count_error)
        } else {
            valid = true
            error.message.value = null
        }

        return valid
    }
}

interface RegisterSmsVerifyEventHandlers {
    fun onClickAuthenticate(view: View)
    fun onClickPassCodeResend(view: View)
    fun onClickRegisterPhone(view: View)
}
