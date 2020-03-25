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
import jp.co.recruit.erikura.databinding.ActivityRegisterNameBinding
import jp.co.recruit.erikura.presenters.activities.ErrorMessageViewModel

class RegisterNameActivity : AppCompatActivity(),
    RegisterNameEventHandlers {
    private val viewModel: RegisterNameViewModel by lazy {
        ViewModelProvider(this).get(RegisterNameViewModel::class.java)
    }

    var user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        // ユーザ情報を受け取る
        user = intent.getParcelableExtra("user")

        val binding: ActivityRegisterNameBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_name)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
        viewModel.firstNameError.message.value = null
        viewModel.lastNameError.message.value = null
    }

    override fun onClickNext(view: View) {
        Log.v("LastName", viewModel.lastName.value ?: "")
        user.lastName = viewModel.lastName.value
        Log.v("FirstName", viewModel.firstName.value ?: "")
        user.firstName = viewModel.firstName.value
        val intent: Intent = Intent(this@RegisterNameActivity, RegisterBirthdayActivity::class.java)
        intent.putExtra("user", user)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }
}

class RegisterNameViewModel: ViewModel() {
    val lastName: MutableLiveData<String> = MutableLiveData()
    val lastNameError: ErrorMessageViewModel = ErrorMessageViewModel()
    val firstName: MutableLiveData<String> = MutableLiveData()
    val firstNameError: ErrorMessageViewModel = ErrorMessageViewModel()

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(lastName) { result.value = isValid() }
        result.addSource(firstName) { result.value = isValid()  }
    }

    private fun isValid(): Boolean {
        var valid = true
        valid = isValidFirstName() && valid
        valid = isValidLastName() && valid

        return valid
    }

    private fun isValidLastName(): Boolean {
        var valid = true

        if (valid && lastName.value?.isBlank() ?:true) {
            valid = false
            lastNameError.message.value = null
        } else if (valid && !(lastName.value?.length ?: 0 <= 30)) {
            valid = false
            lastNameError.message.value = ErikuraApplication.instance.getString(R.string.last_name_count_error)
        } else {
            valid = true
            lastNameError.message.value = null
        }

        return valid
    }

    private fun isValidFirstName(): Boolean {
        var valid = true

        if (valid && firstName.value?.isBlank() ?:true) {
            valid = false
            firstNameError.message.value = null
        } else if (valid && !(firstName.value?.length ?: 0 <= 30)) {
            valid = false
            firstNameError.message.value = ErikuraApplication.instance.getString(R.string.first_name_count_error)
        } else {
            valid = true
            firstNameError.message.value = null
        }

        return valid
    }
}

interface RegisterNameEventHandlers {
    fun onClickNext(view: View)
}