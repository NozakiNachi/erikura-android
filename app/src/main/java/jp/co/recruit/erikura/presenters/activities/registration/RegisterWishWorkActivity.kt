package jp.co.recruit.erikura.presenters.activities.registration

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityRegisterWishWorkBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.WebViewActivity


class RegisterWishWorkActivity : BaseActivity(),
    RegisterWishWorkEventHandlers {
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

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_register_job_request", params= bundleOf())
        Tracking.view(name= "/user/register/wish_works", title= "本登録画面（希望職種）")
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
        // SMS認証前登録処理
        Api(this).initialRegister(user) {
            //登録処理を行う前にSMS認証を行う
            val intent: Intent = Intent(this, SmsVerifyActivity::class.java)
            intent.putExtra("user", user)
            intent.putExtra("phoneNumber", user.phoneNumber)
            intent.putExtra("requestCode", ErikuraApplication.REQUEST_SIGN_UP_CODE)
            startActivityForResult(intent, ErikuraApplication.REQUEST_SIGN_UP_CODE)
        }
    }

    override fun onClickTermsOfService(view: View) {
        val termsOfServiceURLString = BuildConfig.SERVER_BASE_URL + BuildConfig.TERMS_OF_SERVICE_PATH
        val intent = Intent(this, WebViewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(termsOfServiceURLString)
        }
        startActivity(intent)
    }

    override fun onClickPrivacyPolicy(view: View) {
        val privacyPolicyURLString = BuildConfig.SERVER_BASE_URL + BuildConfig.PRIVACY_POLICY_PATH
        val intent = Intent(this, WebViewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(privacyPolicyURLString)
        }
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ErikuraApplication.REQUEST_SIGN_UP_CODE && resultCode == RESULT_OK) {
            data?.let{
                user = data.getParcelableExtra("user")
            }
            //ユーザー登録API呼び出し
            val api = Api(this)
            api.initialUpdateUser(user) { userSession->
                Log.v("DEBUG", "ユーザ登録： userSession=${userSession}")
                // 登録したuser情報がセッションから取れないのでセッションのuserIdからuserを再取得します
                api.user() {
                    user = it
                    // 登録完了画面へ遷移
                    val intent: Intent = Intent(this, RegisterFinishedActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra("user", user)
                    startActivity(intent)
                    finish()

                    // 登録完了のトラッキングの送出
                    Tracking.logEvent(event = "signup", params = bundleOf(Pair("user_id", user.id)))
                    Tracking.identify(user = user, status = "login")
                    Tracking.logCompleteRegistrationEvent()
                }
            }
        }
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