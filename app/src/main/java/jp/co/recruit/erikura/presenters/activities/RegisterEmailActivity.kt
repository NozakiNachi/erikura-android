package jp.co.recruit.erikura.presenters.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityRegisterEmailBinding

class RegisterEmailActivity : AppCompatActivity(), SendEmailEventHandlers {
    private val viewModel: RegisterEmailViewModel by lazy {
        ViewModelProvider(this).get(RegisterEmailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_email)

        val binding: ActivityRegisterEmailBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_email)
        binding.viewModel = viewModel
        binding.handlers = this
    }

    override fun onClickSendEmail(view: View) {
        Log.v("EMAIL", viewModel.email.value ?: "")
        // 仮登録APIの実行
        Api(this).registerEmail(viewModel.email.value ?:"") {
            Log.v("DEBUG", "仮登録メール送信： userId=${it}")
            // FIXME: 仮登録完了画面へ遷移
        }
    }
}

class RegisterEmailViewModel: ViewModel() {
    val email: MutableLiveData<String> = MutableLiveData()
}

interface SendEmailEventHandlers {
    fun onClickSendEmail(view: View)
}