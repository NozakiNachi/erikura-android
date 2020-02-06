package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.User
import java.util.*
import jp.co.recruit.erikura.databinding.ActivityRegisterBirthdayBinding

class RegisterBirthdayActivity : AppCompatActivity(), RegisterBirthdayEventHandlers {
    private val viewModel: RegisterBirthdayViewModel by lazy {
        ViewModelProvider(this).get(RegisterBirthdayViewModel::class.java)
    }

    var user: User = User()
    // カレンダー設定
    val calender: Calendar = Calendar.getInstance()
    var date: DatePickerDialog.OnDateSetListener =
        DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            calender.set(Calendar.YEAR, year)
            calender.set(Calendar.MONTH, monthOfYear)
            calender.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            viewModel.birthday.value =
                String.format("%d/%02d/%02d", year, monthOfYear + 1, dayOfMonth)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_register_birthday)

        // ユーザ情報を受け取る
        user = intent.getParcelableExtra("user")

        val binding: ActivityRegisterBirthdayBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_register_birthday)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        calender.set(Calendar.YEAR, 1980)
        calender.set(Calendar.MONTH, 1 - 1)
        calender.set(Calendar.DAY_OF_MONTH, 1)
        viewModel.birthday.value = String.format("%d/%02d/%02d", 1980, 1, 1)
    }

    override fun onClickNext(view: View) {
        Log.v("BIRTHDAY", viewModel.birthday.value ?: "")
        user.dateOfBirth = viewModel.birthday.value
        val intent: Intent = Intent(this@RegisterBirthdayActivity, RegisterGenderActivity::class.java)
        intent.putExtra("user", user)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onClickEditView(view: View) {
        Log.v("EditView", "EditTextTapped!")
        val dpd = DatePickerDialog(
            this@RegisterBirthdayActivity, date, calender
                .get(Calendar.YEAR), calender.get(Calendar.MONTH),
            calender.get(Calendar.DAY_OF_MONTH)
        )

        val dp = dpd.datePicker
        val maxDate: Calendar = Calendar.getInstance()
        maxDate.add(Calendar.YEAR, -18)
        dp.maxDate = maxDate.timeInMillis

        dpd.show()
    }
}

class RegisterBirthdayViewModel : ViewModel() {
    val birthday: MutableLiveData<String> = MutableLiveData()
}

interface RegisterBirthdayEventHandlers {
    fun onClickNext(view: View)
    fun onClickEditView(view: View)
}