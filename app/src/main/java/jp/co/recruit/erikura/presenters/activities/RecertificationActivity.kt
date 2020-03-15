package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
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
import jp.co.recruit.erikura.databinding.ActivityLoginRequiredBinding
import jp.co.recruit.erikura.databinding.ActivityRecertificationBinding
import jp.co.recruit.erikura.databinding.DialogChangeUserInformationSuccessBinding
import jp.co.recruit.erikura.presenters.activities.LoginActivity
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.registration.RegisterEmailActivity

class RecertificationActivity : AppCompatActivity(), RecertificationHandlers {

    var user: User = User()
    var fromChangeUserInformation: Boolean = false
    var fromAccountSetting: Boolean = false

    private val viewModel: RecertificationViewModel by lazy {
        ViewModelProvider(this).get(RecertificationViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityRecertificationBinding = DataBindingUtil.setContentView(this, R.layout.activity_recertification)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        // 登録メールアドレスを取得
        Api(this).user() {
            user = it
            viewModel.email.value = user.email
        }

        fromChangeUserInformation = intent.getBooleanExtra("onClickChangeUserInformation", false)
        fromAccountSetting = intent.getBooleanExtra("onClickAccountSetting", false)
    }

    override fun onClickRecertification(view: View) {

        Api(this).login(viewModel.email.value ?: "", viewModel.password.value ?: "") {
            Log.v("DEBUG", "再認証成功: userId=${it.userId}")

            // クリックされていた画面へ遷移
            if (fromChangeUserInformation) {
                val intent = Intent(this, ChangeUserInformationActivity::class.java)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                finish()
            }else if(fromAccountSetting) {
                val intent = Intent(this, AccountSettingActivity::class.java)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                finish()
            }
        }
    }
    // FIXME: 再認証の有効時間(アプリをkillまたは10分以上経過しない限り認証不要)を設ける
}

class RecertificationViewModel: ViewModel() {
    val email: MutableLiveData<String> = MutableLiveData()
    val password: MutableLiveData<String> = MutableLiveData()

    val isRecerfiticationEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(password) { result.value = isValid()  }
    }

    fun isValid(): Boolean {
        return (password.value?.isNotBlank() ?: false)
    }
}

interface RecertificationHandlers {
    fun onClickRecertification(view: View)
}