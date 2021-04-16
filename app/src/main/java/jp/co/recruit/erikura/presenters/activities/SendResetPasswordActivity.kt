package jp.co.recruit.erikura.presenters.activities

import android.content.Intent
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
import jp.co.recruit.erikura.business.models.ErikuraConst
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivitySendResetPasswordBinding
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel
import org.apache.commons.lang.StringUtils


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
        viewModel.error.message.value = null

        // エラーメッセージを受け取る
        val errorMessages = intent.getStringArrayExtra("errorMessages")
        if (errorMessages != null) {
            Api(this).displayErrorAlert(errorMessages.asList())
        }
    }

    override fun onClickSendResetPassword(view: View) {
        Api(this).sendPasswordReset(viewModel.email.value ?:"") {
            Tracking.logEvent(event = "view_password_edit", params = bundleOf())
            Tracking.view(
                name = "/user/password/edit",
                title = "パスワード再設定通知完了画面"
            )
            // 常にrespons　trueなので送信完了画面へ遷移します
            var intent = Intent(this, SendedResetPasswordActivity::class.java)
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

class SendResetPasswordViewModel : ViewModel() {

    // バリデーションルール
    private val emailPattern = ErikuraConst.emailPattern

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
        } else {
            valid = true
            error.message.value = null
        }

        return valid
    }

}

class ErrorMessageViewModel {
    val message: MutableLiveData<String> = MutableLiveData()
    val visibility = MediatorLiveData<Int>().also { result ->
        result.addSource(message) {
            result.value = if (message.value == null || StringUtils.isBlank(message.value)) {
                View.GONE
            }
            else {
                View.VISIBLE
            }
        }
    }
}

interface SendResetPasswordEventHandlers {
    fun onClickSendResetPassword(view: View)
}