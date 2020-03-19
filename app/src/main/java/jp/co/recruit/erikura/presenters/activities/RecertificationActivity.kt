package jp.co.recruit.erikura.presenters.activities

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
import jp.co.recruit.erikura.databinding.ActivityRecertificationBinding
import java.util.*

class RecertificationActivity : AppCompatActivity(), RecertificationHandlers {

    var user: User = User()
    val date: Date = Date()

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
    }

    override fun onClickRecertification(view: View) {
        Api(this).recertification(viewModel.email.value ?: "", viewModel.password.value ?: "") {
            Log.v("DEBUG", "再認証成功: userId=${it.userId}")
        }
        finish()
    }
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