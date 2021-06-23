package jp.co.recruit.erikura.presenters.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
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
import jp.co.recruit.erikura.business.models.ErikuraConst
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivitySendChangeEmailBinding
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel
import org.apache.commons.lang.StringUtils


class SendChangeEmailActivity : BaseActivity(),
    SendChangeEmailEventHandlers {
    var user: User = User()

    private val viewModel: SendChangeEmailViewModel by lazy {
        ViewModelProvider(this).get(SendChangeEmailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivitySendChangeEmailBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_send_change_email)
        user = intent.getParcelableExtra("user")?: User()
        binding.lifecycleOwner = this
        binding.handlers = this
        viewModel.currentEmail.value = user.email
        binding.viewModel = viewModel
        viewModel.error.message.value = null

        // エラーメッセージを受け取る
        val errorMessages = intent.getStringArrayExtra("errorMessages")
        if (errorMessages != null) {
            Api(this).displayErrorAlert(errorMessages.asList())
        }
    }

    override fun onClickSendChangeEmail(view: View) {
        Api(this).sendEmailReset(viewModel.email.value ?:"") {
            Tracking.logEvent(event = "view_email_edit", params = bundleOf())
            Tracking.view(
                name = "/user/email/edit",
                title = "メールアドレス再設定通知完了画面"
            )
            // 常にrespons　trueなので送信完了画面へ遷移します
            var intent = Intent(this, SendedChangeEmailActivity::class.java)
            startActivity(intent)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val view = this.currentFocus
        if (view != null) {
            val layout = findViewById<ConstraintLayout>(R.id.reset_in_constraintLayout)
            layout.requestFocus()

            val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(layout.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }
}

class SendChangeEmailViewModel : ViewModel() {

    // バリデーションルール
    private val emailPattern = ErikuraConst.emailPattern
    val currentEmail: MutableLiveData<String> = MutableLiveData()
    val email: MutableLiveData<String> = MutableLiveData()
    val error: ErrorMessageViewModel = ErrorMessageViewModel()

    val isSendButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(email) { result.value = isValid() }
    }

    private fun isValid(): Boolean {
        var valid = true

        if (valid && email.value?.isBlank() ?: true) {
            valid = false
            error.message.value = null
        }else if (valid && !(emailPattern.matches(email.value ?: ""))) {
            valid = false
            error.message.value = ErikuraApplication.instance.getString(R.string.email_format_error)
        }else if (currentEmail.value == email.value) {
            valid = false
            error.message.value = ErikuraApplication.instance.getString(R.string.email_same_error)
        } else {
            valid = true
            error.message.value = null
        }

        return valid
    }

}

interface SendChangeEmailEventHandlers {
    fun onClickSendChangeEmail(view: View)
}