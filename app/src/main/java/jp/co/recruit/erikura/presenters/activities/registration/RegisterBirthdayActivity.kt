package jp.co.recruit.erikura.presenters.activities.registration

import android.app.ActivityOptions
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.util.DateUtils
import jp.co.recruit.erikura.databinding.ActivityRegisterBirthdayBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import java.text.SimpleDateFormat
import java.util.*

class RegisterBirthdayActivity : BaseActivity(),
    RegisterBirthdayEventHandlers {
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

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_register_birth", params= bundleOf())
        Tracking.view(name= "/user/register/birthday", title= "本登録画面（誕生日）")
    }

    override fun onClickNext(view: View) {
        Log.v("BIRTHDAY", viewModel.birthday.value ?: "")
        user.dateOfBirth = viewModel.birthday.value
        val intent: Intent = Intent(this@RegisterBirthdayActivity, RegisterGenderActivity::class.java)
        intent.putExtra("user", user)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    // 生年月日
    override fun onClickEditView(view: View) {
        Log.v("EditView", "EditTextTapped!")

        var onDateSetListener: DatePickerDialog.OnDateSetListener =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                val calendar = Calendar.getInstance()

                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                var birthday = Date(arrayOf(calendar.timeInMillis, view.maxDate).min()!!)

                val sdf = SimpleDateFormat("yyyy/MM/dd")
                viewModel.birthday.value = sdf.format(birthday)
            }

        val calendar = Calendar.getInstance()
        val dateOfBirth = DateUtils.parseDate(viewModel.birthday.value, arrayOf("yyyy/MM/dd", "yyyy-MM-dd"))
        calendar.time = dateOfBirth
        val dpd = DatePickerDialog(
            this@RegisterBirthdayActivity, onDateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
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