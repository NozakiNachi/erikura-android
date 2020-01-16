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
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityRegisterWishWorkBinding

class RegisterWishWorkActivity : AppCompatActivity(), RegisterWishWorkEventHandlers {
    private val viewModel: RegisterWishWorkViewModel by lazy {
        ViewModelProvider(this).get(RegisterWishWorkViewModel::class.java)
    }

    var user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_register_wish_work)

        // ユーザ情報を受け取る
        user = intent.getParcelableExtra("user")

        val binding: ActivityRegisterWishWorkBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_wish_work)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
    }

    override fun onClickRegister(view: View) {
        val list: MutableList<String> = mutableListOf()
        if(viewModel.interestedSmartPhone.value ?: false){ list.add("smart_phone") }
        if(viewModel.interestedCleaning.value ?: false){ list.add("cleaning") }
        if(viewModel.interestedWalk.value ?: false){ list.add("walk") }
        if(viewModel.interestedBicycle.value ?: false){ list.add("bicycle") }
        if(viewModel.interestedCar.value ?: false){ list.add("car") }
        user.wishWorks = list
        Log.v("WISHWORK", list.toString())
        // ユーザ登録Apiの呼び出し
        Api(this).initialUpdateUser(user) {
            Log.v("DEBUG", "ユーザ登録： userId=${it}")
            // 登録完了画面へ遷移
            val intent: Intent = Intent(this@RegisterWishWorkActivity, RegisterFinishedActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onClickTermsOfService(view: View) {
        val termsOfServiceURLString = BuildConfig.SERVER_BASE_URL + "/pdf/terms_of_service.pdf"
        val intent = Intent(this, WebViewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(termsOfServiceURLString)
        }
        startActivity(intent)
    }

    override fun onClickPrivacyPolicy(view: View) {
        val privacyPolicyURLString = BuildConfig.SERVER_BASE_URL + "/pdf/privacy_policy.pdf"
        val intent = Intent(this, WebViewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(privacyPolicyURLString)
        }
        startActivity(intent)
    }
}

class RegisterWishWorkViewModel: ViewModel() {
    val interestedSmartPhone: MutableLiveData<Boolean> = MutableLiveData()
    val interestedCleaning: MutableLiveData<Boolean> = MutableLiveData()
    val interestedWalk: MutableLiveData<Boolean> = MutableLiveData()
    val interestedBicycle: MutableLiveData<Boolean> = MutableLiveData()
    val interestedCar: MutableLiveData<Boolean> = MutableLiveData()

    val isRegisterButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(interestedSmartPhone) {result.value = isValid()}
        result.addSource(interestedCleaning) {result.value = isValid()}
        result.addSource(interestedWalk) {result.value = isValid()}
        result.addSource(interestedBicycle) {result.value = isValid()}
        result.addSource(interestedCar) {result.value = isValid()}
    }

    private fun isValid(): Boolean {
        return interestedSmartPhone.value ?:false || interestedCleaning.value ?:false || interestedWalk.value ?:false || interestedBicycle.value ?:false || interestedCar.value ?:false
    }
}

interface RegisterWishWorkEventHandlers {
    fun onClickRegister(view: View)
    fun onClickTermsOfService(view: View)
    fun onClickPrivacyPolicy(view: View)
}