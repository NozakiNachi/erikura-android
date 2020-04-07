package jp.co.recruit.erikura.presenters.activities.registration

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Gender
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.ActivityRegisterGenderBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity

class RegisterGenderActivity : BaseActivity(),
    RegisterGenderEventHandlers {

    var user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_register_gender)

        // ユーザ情報を受け取る
        user = intent.getParcelableExtra("user")

        val binding: ActivityRegisterGenderBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_gender)
        binding.lifecycleOwner = this
        binding.handlers = this
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_register_gender", params= bundleOf())
        Tracking.view(name= "/user/register/gender", title= "本登録画面（性別）")
    }

    override fun onClickMale(view: View) {
        Log.v("GENDER", "male")
        user.gender = Gender.MALE
        moveToNext(user)
    }

    override fun onClickFemale(view: View) {
        Log.v("GENDER", "female")
        user.gender = Gender.FEMALE
        moveToNext(user)
    }

    fun moveToNext(user: User) {
        val intent: Intent = Intent(this@RegisterGenderActivity, RegisterAddressActivity::class.java)
        intent.putExtra("user", user)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }
}

interface RegisterGenderEventHandlers {
    fun onClickMale(view: View)
    fun onClickFemale(view: View)
}
