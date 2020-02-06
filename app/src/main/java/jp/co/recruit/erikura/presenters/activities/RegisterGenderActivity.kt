package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Gender
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.ActivityRegisterGenderBinding

class RegisterGenderActivity : AppCompatActivity(), RegisterGenderEventHandlers {

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
