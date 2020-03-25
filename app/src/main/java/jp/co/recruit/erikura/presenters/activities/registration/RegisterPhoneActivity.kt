package jp.co.recruit.erikura.presenters.activities.registration

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
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.ActivityRegisterPhoneBinding
import jp.co.recruit.erikura.presenters.activities.ErrorMessageViewModel
import java.util.regex.Pattern

class RegisterPhoneActivity : AppCompatActivity(),
    RegisterPhoneEventHandlers {
    private val viewModel: RegisterPhoneViewModel by lazy {
        ViewModelProvider(this).get(RegisterPhoneViewModel::class.java)
    }

    var user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        // ユーザ情報を受け取る
        user = intent.getParcelableExtra("user")

        val binding: ActivityRegisterPhoneBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_phone)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
        viewModel.error.message.value = null
    }

    override fun onClickNext(view: View) {
        Log.v("PHONE", viewModel.phone.value ?: "")
        user.phoneNumber = viewModel.phone.value

        val intent: Intent = Intent(this@RegisterPhoneActivity, RegisterJobStatusActivity::class.java)
        intent.putExtra("user", user)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
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

        if (valid && phone.value?.isBlank() ?:true) {
            valid = false
            error.message.value = null
        }else if(valid && !(pattern.matcher(phone.value).find())) {
            valid = false
            error.message.value = ErikuraApplication.instance.getString(R.string.phone_pattern_error)
        }else if(valid && !(phone.value?.length ?: 0 == 10 || phone.value?.length ?: 0 == 11)) {
            valid = false
            error.message.value = ErikuraApplication.instance.getString(R.string.phone_count_error)
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
