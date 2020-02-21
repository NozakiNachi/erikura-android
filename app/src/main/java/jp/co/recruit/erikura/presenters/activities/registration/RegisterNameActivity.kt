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

class RegisterNameActivity : AppCompatActivity(),
    RegisterNameEventHandlers {
    private val viewModel: RegisterNameViewModel by lazy {
        ViewModelProvider(this).get(RegisterNameViewModel::class.java)
    }

    var user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_register_name)

        // ユーザ情報を受け取る
        user = intent.getParcelableExtra("user")

        val binding: ActivityRegisterNameBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_name)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
        viewModel.lastNameErrorVisibility.value = 8
        viewModel.firstNameErrorVisibility.value = 8
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
    val lastNameErrorMsg: MutableLiveData<String> = MutableLiveData()
    val lastNameErrorVisibility: MutableLiveData<Int> = MutableLiveData()
    val firstName: MutableLiveData<String> = MutableLiveData()
    val firstNameErrorMsg: MutableLiveData<String> = MutableLiveData()
    val firstNameErrorVisibility: MutableLiveData<Int> = MutableLiveData()

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
            lastNameErrorMsg.value = ""
            lastNameErrorVisibility.value = 8
        } else if (valid && !(lastName.value?.length ?: 0 <= 30)) {
            valid = false
            lastNameErrorMsg.value = ErikuraApplication.instance.getString(R.string.last_name_count_error)
            lastNameErrorVisibility.value = 0
        } else {
            valid = true
            lastNameErrorMsg.value = ""
            lastNameErrorVisibility.value = 8
        }

        return valid
    }

    private fun isValidFirstName(): Boolean {
        var valid = true

        if (valid && firstName.value?.isBlank() ?:true) {
            valid = false
            firstNameErrorMsg.value = ""
            firstNameErrorVisibility.value = 8
        } else if (valid && !(firstName.value?.length ?: 0 <= 30)) {
            valid = false
            firstNameErrorMsg.value = ErikuraApplication.instance.getString(R.string.first_name_count_error)
            firstNameErrorVisibility.value = 0
        } else {
            valid = true
            firstNameErrorMsg.value = ""
            firstNameErrorVisibility.value = 8
        }

        return valid
    }
}

interface RegisterNameEventHandlers {
    fun onClickNext(view: View)
}