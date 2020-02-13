package jp.co.recruit.erikura.presenters.activities.job

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobStatus
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.presenters.fragments.AppliedJobDetailsFragment
import jp.co.recruit.erikura.presenters.fragments.NormalJobDetailsFragment
import jp.co.recruit.erikura.presenters.fragments.WorkingJobDetailsFragment
import jp.co.recruit.erikura.presenters.util.GoogleFitApiManager
import java.util.*
import java.util.concurrent.TimeUnit


class JobDetailsActivity : AppCompatActivity() {

    var job: Job = Job()
    var user: User = User()
    var fragment = Fragment()
    var fromAppliedJobDetailsFragment: Boolean = false
    var fromWorkingJobDetailsFragment: Boolean = false

    private val fitApiManager: GoogleFitApiManager = ErikuraApplication.fitApiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_details)

        val value = intent.getParcelableExtra<Job>("job")
        if (value != null) {
            job = value
        }else {
            handleIntent(intent)
        }
        Log.v("DEBUG", job.toString())

        fromAppliedJobDetailsFragment = intent.getBooleanExtra("onClickStart", false)
        fromWorkingJobDetailsFragment = intent.getBooleanExtra("onClickCancelWorking", false)
    }
    
    override fun onStart() {
        super.onStart()
        fetchJob()
    }

    private fun fetchJob() {
        // jobの再取得
        Api(this).reloadJob(job) {
            it.toString()
            job = it
            if (UserSession.retrieve() != null) {
                Api(this).user {
                    user = it
                    refreshContents()
                }
            }else {
                refreshContents()
            }
        }
    }

    private fun refreshContents() {
        val transaction = supportFragmentManager.beginTransaction()
        // fragmentの作成
        // jobのステータスで挿しこむフラグメントを変更します
        when(job.status) {
            JobStatus.Normal -> {
                fragment = NormalJobDetailsFragment(this, job, user)
            }
            JobStatus.Applied -> {
                fragment = AppliedJobDetailsFragment(this, job, user, fromWorkingJobDetailsFragment)
                fromWorkingJobDetailsFragment = false
            }
            JobStatus.Working -> {
                fragment = WorkingJobDetailsFragment(this, job, user, fromAppliedJobDetailsFragment)
                fromAppliedJobDetailsFragment = false
            }
            JobStatus.Finished -> {
                // FIXME: 作業完了画面
                fragment = NormalJobDetailsFragment(this, job, user)
            }
            JobStatus.Reported -> {
                // FIXME: 作業報告済み画面
                fragment = NormalJobDetailsFragment(this, job, user)
            }
            else -> {
                fragment = NormalJobDetailsFragment(this, job, user)
            }
        }
        // fragmentの更新
        transaction.replace(R.id.job_details, fragment)
        transaction.commit()
    }


    private fun handleIntent(intent: Intent) {
        val appLinkData: Uri? = intent.data
        val jobId = appLinkData!!.lastPathSegment!!.toInt()
        job.id = jobId
    }
}
