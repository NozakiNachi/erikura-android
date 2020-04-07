package jp.co.recruit.erikura.presenters.activities.registration

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityRegisterPasswordBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.StartActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel
import java.util.regex.Pattern


class RegisterPasswordActivity : BaseActivity(),
    RegisterPasswordEventHandlers {
    private val viewModel: RegisterPasswordViewModel by lazy {
        ViewModelProvider(this).get(RegisterPasswordViewModel::class.java)
    }

    val user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_register_password)

        val binding: ActivityRegisterPasswordBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_password)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
        viewModel.error.message.value = null

        // 仮登録トークン取得
        var uri: Uri? = intent.data
        if (uri?.path == "/api/v1/utils/open_android_app") {
            val path = uri?.getQueryParameter("path")
            uri = Uri.parse("erikura://${path}")
        }
        val confirmationToken: String? = uri?.getQueryParameter("confirmation_token")
        // ワーカ仮登録の確認
        Api(this).registerConfirm(confirmationToken ?:"", onError = {
            Log.v("DEBUG", "ユーザ仮登録確認失敗")
            // スタート画面へ遷移する
            val intent = Intent(this, StartActivity::class.java)
            if(it != null) {
                val array = it.toTypedArray()
                intent.putExtra("errorMessages", array)
            }
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            finish()
        }) {
            Log.v("DEBUG", "仮登録確認： userId=${it}")
            user.confirmationToken = confirmationToken
        }
    }

    override fun onClickNext(view: View) {
        Log.v("PASSWORD", viewModel.password.value ?: "")
        user.password = viewModel.password.value
        val intent: Intent = Intent(this@RegisterPasswordActivity, RegisterNameActivity::class.java)
        intent.putExtra("user", user)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }
}

class RegisterPasswordViewModel: ViewModel() {
    val password: MutableLiveData<String> = MutableLiveData()
    val error: ErrorMessageViewModel = ErrorMessageViewModel()

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(password) {result.value = isValid() }
    }

    private fun isValid(): Boolean {
        var valid = true
        val pattern = Pattern.compile("^([a-zA-Z0-9]{6,})\$")
        val alPattern = Pattern.compile("^(.*[A-z]+.*)")
        val numPattern = Pattern.compile("^(.*[0-9]+.*)")

        if (valid && password.value?.isBlank() ?:true) {
            valid = false
            error.message.value = null
        }else if(valid && !(pattern.matcher(password.value).find())) {
            valid = false
            error.message.value = ErikuraApplication.instance.getString(R.string.password_count_error)
        }else if(valid && (!(alPattern.matcher(password.value).find()) || !(numPattern.matcher(password.value).find()))) {
            valid = false
            error.message.value = ErikuraApplication.instance.getString(R.string.password_pattern_error)
        } else {
            valid = true
            error.message.value = null
        }

        return valid
    }
}

interface RegisterPasswordEventHandlers {
    fun onClickNext(view: View)
}