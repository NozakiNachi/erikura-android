package jp.co.recruit.erikura.presenters.activities

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
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
import jp.co.recruit.erikura.databinding.ActivitySendResetPasswordBinding
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel

//FIXME 遷移するのにログイン求められているのでそちらを修正する必要あり

class SendResetPasswordActivity : BaseActivity(),
    SendResetPasswordEventHandlers {
    var user: User = User()

    private val viewModel: SendResetPasswordViewModel by lazy {
        ViewModelProvider(this).get(SendResetPasswordViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivitySendResetPasswordBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_send_reset_password)
        binding.lifecycleOwner = this
        binding.handlers = this
        binding.viewModel = viewModel

        //FIXME トークンが一致するか判定API　できなければ画面を表示しない

        // エラーメッセージを受け取る
        val errorMessages = intent.getStringArrayExtra("errorMessages")
        if (errorMessages != null) {
            Api(this).displayErrorAlert(errorMessages.asList())
        }

        // ページ参照のトラッキングの送出
        Tracking.logEvent(event = "view_send_reset_password", params = bundleOf())
        Tracking.view(name = "/users/send/reset/password/", title = "パスワード再設定メール送信")

        // 変更するユーザーの現在の登録値を取得
        val api = Api(this)
        api.user() {
            user = it
            user.id?.let { userId ->
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val view = this.currentFocus
        if (view != null) {
            val constraintLayout =
                findViewById<ConstraintLayout>(R.id.change_user_information_constraintLayout)
            constraintLayout.requestFocus()

            val imm: InputMethodManager =
                getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(constraintLayout.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onClickSendResetPassword(view: View) {
        //API実行
        //メール送信後下記を行う
        //ログインフォームへ遷移　遷移する際文言を表示する
        //「パスワードのリセット方法を数分以内にメールでご連絡します。」
    }

    override fun onClickLoginForm(view: View) {
        //ログインフォーム
        TODO("Not yet implemented")
    }

    override fun onClickResendPreRegister(view: View) {
        //仮登録メールの再送信
        TODO("Not yet implemented")
    }
}

class SendResetPasswordViewModel : ViewModel() {

    // バリデーションルール
    private val emailPattern = """\A[\w._%+-|]+@[\w0-9.-]+\.[A-Za-z]{2,}\z""".toRegex()

    val email: MutableLiveData<String> = MutableLiveData()
    val error: ErrorMessageViewModel = ErrorMessageViewModel()

    val isSendButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(email) { result.value = isValid() }
    }

    private fun isValid(): Boolean {
        //FIXME API側で確認するかもやけど　アドレスがDBに存在するか、そのユーザーと一致してるか要確認
        var valid = true

        if (valid && email.value?.isBlank() ?: true) {
            valid = false
            error.message.value = null
        }else if (valid && !(emailPattern.matches(email.value ?: ""))) {
            valid = false
            error.message.value = ErikuraApplication.instance.getString(R.string.email_format_error)
        } else {
            valid = true
            error.message.value = null
        }

        return valid
    }

}

interface SendResetPasswordEventHandlers {
    fun onClickSendResetPassword(view: View)
    fun onClickLoginForm(view: View)
    fun onClickResendPreRegister(view: View)
}