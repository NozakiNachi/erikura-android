package jp.co.recruit.erikura.presenters.activities.registration

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import jp.co.recruit.erikura.databinding.ActivityRegisterPhoneBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel
import java.util.regex.Pattern

class RegisterPhoneActivity : BaseActivity(),
    RegisterPhoneEventHandlers {
    private val viewModel: RegisterPhoneViewModel by lazy {
        ViewModelProvider(this).get(RegisterPhoneViewModel::class.java)
    }

    var user: User = User()
    var requestCode: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        // ユーザ情報を受け取る
        user = intent.getParcelableExtra("user")
        requestCode = intent.getIntExtra("requestCode",ErikuraApplication.REQUEST_DEFAULT_CODE)

        // エラーメッセージを受け取る
        val errorMessages = intent.getStringArrayExtra("errorMessages")
        if(errorMessages != null){
            Api(this).displayErrorAlert(errorMessages.asList())
        }

        val binding: ActivityRegisterPhoneBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_phone)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
        viewModel.error.message.value = null
        user.phoneNumber?.let {
            // SMS認証画面の電話番号編集できた場合初期値を入れる
            viewModel.phone.value = it
        }
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_register_phone", params= bundleOf())
        Tracking.view(name= "/user/register/tel", title= "本登録画面（電話番号）")
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val view = this.currentFocus
        if (view != null) {
            val constraintLayout = findViewById<ConstraintLayout>(R.id.register_phone_constraintLayout)
            constraintLayout.requestFocus()

            val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(constraintLayout.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onClickNext(view: View) {
        Log.v("PHONE", viewModel.phone.value ?: "")
        user.phoneNumber = viewModel.phone.value

        //新規登録のSMS認証経由で来た場合SMS認証画面へ遷移する
        if (requestCode == ErikuraApplication.REQUEST_SIGN_UP_CODE) {
            val intent: Intent = Intent()
            intent.putExtra("user", user)
            intent.putExtra("phoneNumber", user.phoneNumber)
            intent.putExtra("requestCode",ErikuraApplication.REQUEST_SIGN_UP_CODE)
            setResult(RESULT_OK, intent)
            finish()
        } else {
            val intent: Intent = Intent(this@RegisterPhoneActivity, RegisterJobStatusActivity::class.java)
            intent.putExtra("user", user)
            startActivity(intent)
        }
    }
}

class RegisterPhoneViewModel: ViewModel() {
    val phone: MutableLiveData<String> = MutableLiveData()
    val error: ErrorMessageViewModel = ErrorMessageViewModel()

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(phone) {result.value = isValid() }
    }

    private fun isValid(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([0-9])")
        val pattern2 = Pattern.compile("^(070|080|090)")

        if (valid && phone.value?.isBlank() ?:true) {
            valid = false
            error.message.value = null
        }else if(valid && !(pattern.matcher(phone.value).find())) {
            valid = false
            error.message.value = ErikuraApplication.instance.getString(R.string.phone_pattern_error)
        }else if(valid && !(phone.value?.length ?: 0 == 11)) {
            valid = false
            error.message.value = ErikuraApplication.instance.getString(R.string.phone_count_error)
        }else if(valid && !(pattern2.matcher(phone.value).find())) {
            valid = false
            error.message.value = ErikuraApplication.instance.getString(R.string.phone_pattern2_error)
        } else {
            valid = true
            error.message.value = null
        }

        return valid
    }
}

interface RegisterPhoneEventHandlers {
    fun onClickNext(view: View)
}
