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
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Gender
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api

class RegisterGenderActivity : AppCompatActivity(), RegisterGenderEventHandlers {

    var user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_gender)

        // ユーザ情報を受け取る
        user = intent.getParcelableExtra("user")
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

    }
}

interface RegisterGenderEventHandlers {
    fun onClickMale(view: View)
    fun onClickFemale(view: View)
}
