package jp.co.recruit.erikura.presenters.activities.registration

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
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
import jp.co.recruit.erikura.presenters.activities.mypage.ChangeUserInformationActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel
import java.util.regex.Pattern

class RegisterSmsVerifyActivity : BaseActivity(),
    RegisterSmsVerifyEventHandlers {
    private val viewModel: RegisterSmsVerifyViewModel by lazy {
        ViewModelProvider(this).get(RegisterSmsVerifyViewModel::class.java)
    }

    var user: User = User()
    var requestCode: Int = 0
    var confirmationToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        // ユーザ情報を受け取る
        requestCode = intent.getIntExtra("requestCode",0)
        if (requestCode == 1 || requestCode == 3) {
            user = intent.getParcelableExtra("user")
        } else {
            Api(this).user() {
                user = it
            }
        }
        // 仮登録トークン取得
        var uri: Uri? = intent.data
        if (uri?.path == "/api/v1/utils/open_android_app") {
            val path = uri?.getQueryParameter("path")
            uri = Uri.parse("erikura://${path}")
        }
        confirmationToken = uri?.getQueryParameter("confirmation_token")

        Log.v("DEBUG", "SMS認証メール送信： phoneNumber=${user.phoneNumber}")
        // TODO 現段階ではresultはtrueしか返ってこないので送信結果の判定は入れていない
        Api(this).sendSms(confirmationToken ?:"",user.phoneNumber ?:"", onError = {
            Log.v("DEBUG","SMS認証送信失敗： phoneNumber=${user.phoneNumber}")
            //本登録電話番号画面か会員情報変更画面へ遷移
            if (requestCode == 1) {
                val intent = Intent(this, RegisterPhoneActivity::class.java)
                if(it != null) {
                    val array = it.toTypedArray()
                    intent.putExtra("errorMessages", array)
                }
                intent.putExtra("user",user)
                intent.putExtra("requestCode",requestCode)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            } else {
                val intent = Intent(this, ChangeUserInformationActivity::class.java)
                if(it != null) {
                    val array = it.toTypedArray()
                    intent.putExtra("errorMessages", array)
                }
                intent.putExtra("user",user)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
        }){}

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
        Log.v("DEBUG", "SMS認証： phoneNumber=${user.phoneNumber}")
        // TODO 現段階ではresultはtrueしか返ってこないので認証結果の判定は入れていない
        Api(this).smsVerify(confirmationToken ?:"",user.phoneNumber ?:"", viewModel.passCode.value ?: "", onError = {
            Log.v("DEBUG","SMS認証失敗： phoneNumber=${user.phoneNumber}")
        }){
            //認証成功後 onActivityResultへ飛ぶ
            val intent: Intent = Intent()
            intent.putExtra("user",user)
            setResult(RESULT_OK,intent)
            finish()
        }
    }

    override fun onClickPassCodeResend(view: View) {
        Log.v("DEBUG", "SMS認証メール送信： phoneNumber=${user.phoneNumber}")
        // TODO 現段階ではresultはtrueしか返ってこないので送信結果の判定は入れていない
        Api(this).sendSms(confirmationToken ?:"",user.phoneNumber ?:"", onError = {
            Log.v("DEBUG","SMS認証送信失敗： phoneNumber=${user.phoneNumber}")
            //本登録電話番号画面か会員情報変更画面へ遷移
            if (requestCode == 1) {
                val intent = Intent(this, RegisterPhoneActivity::class.java)
                if(it != null) {
                    val array = it.toTypedArray()
                    intent.putExtra("errorMessages", array)
                }
                intent.putExtra("user",user)
                intent.putExtra("requestCode",requestCode)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            } else {
                val intent = Intent(this, ChangeUserInformationActivity::class.java)
                if(it != null) {
                    val array = it.toTypedArray()
                    intent.putExtra("errorMessages", array)
                }
                intent.putExtra("user",user)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
        }){}
    }

    override fun onClickRegisterPhone(view: View) {
        //本登録の電話番号画面と会員情報変更画面のどちらかへ遷移する
        if (requestCode == 1) {
            val intent = Intent(this, RegisterPhoneActivity::class.java)
            intent.putExtra("user",user)
            intent.putExtra("requestCode",requestCode)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        } else {
            val intent = Intent(this, ChangeUserInformationActivity::class.java)
            intent.putExtra("user",user)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    private fun makeCaption(phoneNumber: String): String {
        var str = "ご登録の電話番号%sにパスコード記載のSMSメッセージをお送りしました。ご確認いただき、10分以内に下記にご入力ください。".format(phoneNumber)
        return str
    }
}

class RegisterSmsVerifyViewModel: ViewModel() {
    val passCode: MutableLiveData<String> = MutableLiveData()
    val error: ErrorMessageViewModel = ErrorMessageViewModel()

    private fun isValid(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([0-9])")

        if (valid && passCode.value?.isBlank() ?:true) {
            valid = false
            error.message.value = null
        }else if(valid && !(pattern.matcher(passCode.value).find())) {
            valid = false
            error.message.value = ErikuraApplication.instance.getString(R.string.passcode_pattern_error)
        }else if(valid && !(passCode.value?.length ?: 0 == 4)) {
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
