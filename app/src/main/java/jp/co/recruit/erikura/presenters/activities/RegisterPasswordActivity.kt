package jp.co.recruit.erikura.presenters.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityRegisterPasswordBinding

class RegisterPasswordActivity : AppCompatActivity() {
    private val viewModel: RegisterPasswordViewModel by lazy {
        ViewModelProvider(this).get(RegisterPasswordViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_register_password)

        val binding: ActivityRegisterPasswordBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_email)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        viewModel.errorVisibility.value = 8
    }
}

class RegisterPasswordViewModel: ViewModel() {
    val password: MutableLiveData<String> = MutableLiveData()
    val errorMsg: MutableLiveData<String> = MutableLiveData()
    val errorVisibility: MutableLiveData<Int> = MutableLiveData()

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(password) {result.value = isValid() }
    }

    private fun isValid(): Boolean {
        var valid = true

        if (valid && password.value?.isBlank() ?:true) {
            valid = false
            errorMsg.value = ""
            errorVisibility.value = 8
        }else {
            valid = true
            errorMsg.value = ""
            errorVisibility.value = 8
        }

        return valid
    }
}