package jp.co.recruit.erikura.presenters.activities.mypage

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityResignInBinding
import java.util.*
class ResignInActivity : AppCompatActivity(), ResignInHandlers {

    var user: User = User()
    val date: Date = Date()

    var fromChangeUserInformationFragment: Boolean = false
    var fromAccountSettingFragment: Boolean = false

    private val viewModel: ResignInViewModel by lazy {
        ViewModelProvider(this).get(ResignInViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityResignInBinding = DataBindingUtil.setContentView(this, R.layout.activity_resign_in)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        // 登録メールアドレスを取得
        Api(this).user() { user->
            viewModel.email.value = user.email
        }

        fromAccountSettingFragment = intent.getBooleanExtra("fromAccountSetting", false)
        fromChangeUserInformationFragment = intent.getBooleanExtra("fromChangeUserInformation", false)
    }

    override fun onClickResignIn(view: View) {
        Api(this).resignIn(viewModel.email.value ?: "", viewModel.password.value ?: "") {
            Log.v("DEBUG", "再認証成功: userId=${it.userId}")
            finish()

            // 画面遷移
            if(fromChangeUserInformationFragment) {
                val intent = Intent(this, ChangeUserInformationActivity::class.java)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }else if(fromAccountSettingFragment){
                val intent = Intent(this, AccountSettingActivity::class.java)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
        }
    }
}

class ResignInViewModel: ViewModel() {
    val email: MutableLiveData<String> = MutableLiveData()
    val password: MutableLiveData<String> = MutableLiveData()

    val isResignInEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(password) { result.value = isValid()  }
    }
    fun isValid(): Boolean {
        return (password.value?.isNotBlank() ?: false)
    }
}

interface ResignInHandlers {
    fun onClickResignIn(view: View)
}