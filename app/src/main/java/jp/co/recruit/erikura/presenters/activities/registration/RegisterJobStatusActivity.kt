package jp.co.recruit.erikura.presenters.activities.registration

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.ActivityRegisterJobStatusBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity

class RegisterJobStatusActivity : BaseActivity(),
    RegisterJobStatusEventHandlers {

    var user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_job_status)

        // ユーザ情報を受け取る
        user = intent.getParcelableExtra("user")

        val binding: ActivityRegisterJobStatusBinding = DataBindingUtil.setContentView(this, R.layout.activity_register_job_status)
        binding.lifecycleOwner = this
        binding.handlers = this
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_register_job", params= bundleOf())
        Tracking.view(name= "/user/register/job", title= "本登録画面（職業）")
    }

    override fun onClickUnemployed(view: View) {
        user.jobStatus = "unemployed"
        moveToNext()
    }

    override fun onClickHomemaker(view: View) {
        user.jobStatus= "homemaker"
        moveToNext()
    }

    override fun onClickFreelancer(view: View) {
        user.jobStatus = "freelancer"
        moveToNext()
    }

    override fun onClickStudent(view: View) {
        user.jobStatus = "student"
        moveToNext()
    }

    override fun onClickPartTime(view: View) {
        user.jobStatus = "part_time"
        moveToNext()
    }

    override fun onClickEmployee(view: View) {
        user.jobStatus = "employee"
        moveToNext()
    }

    override fun onClickSelfEmployed(view: View) {
        user.jobStatus = "self_employed"
        moveToNext()
    }

    override fun onClickOtherJob(view: View) {
        user.jobStatus = "other_job"
        moveToNext()
    }

    private fun moveToNext() {
        val intent: Intent = Intent(this@RegisterJobStatusActivity, RegisterWishWorkActivity::class.java)
        intent.putExtra("user", user)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }
}

interface RegisterJobStatusEventHandlers {
    fun onClickUnemployed(view: View)
    fun onClickHomemaker(view: View)
    fun onClickFreelancer(view: View)
    fun onClickStudent(view: View)
    fun onClickPartTime(view: View)
    fun onClickEmployee(view: View)
    fun onClickSelfEmployed(view: View)
    fun onClickOtherJob(view: View)
}
