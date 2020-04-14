package jp.co.recruit.erikura.presenters.activities.job

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobStatus
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.fragments.*


class JobDetailsActivity : BaseActivity() {

    var job: Job = Job()
    var user: User = User()
    var fragment = Fragment()
    var fromAppliedJobDetailsFragment: Boolean = false
    var fromWorkingJobDetailsFragment: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_details)

        var jobRestored: Boolean = false
        val value = intent.getParcelableExtra<Job>("job")
        if (value != null) {
            jobRestored = true
            job = value
        } else {
            handleIntent(intent)
        }
        Log.v("DEBUG", job.toString())

        // アラート表示
        fromAppliedJobDetailsFragment = intent.getBooleanExtra("onClickStart", false)
        fromWorkingJobDetailsFragment = intent.getBooleanExtra("onClickCancelWorking", false)

        // 現時点での案件情報をもとに画面を構築します
        if (jobRestored) {
            refreshContents()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        var jobRestored: Boolean = false
        intent?.let { intent ->
            val value = intent.getParcelableExtra<Job>("job")
            if (value != null) {
                jobRestored = true
                job = value
            } else {
                handleIntent(intent)
            }
            Log.v("DEBUG", job.toString())

            // アラート表示
            fromAppliedJobDetailsFragment = intent.getBooleanExtra("onClickStart", false)
            fromWorkingJobDetailsFragment = intent.getBooleanExtra("onClickCancelWorking", false)
        }

        // 現時点での案件情報をもとに画面を構築します
        if (jobRestored) {
            refreshContents()
        }

        onStart()
    }

    override fun onStart() {
        super.onStart()

        val errorMessages = intent.getStringArrayExtra("errorMessages")
        if (errorMessages != null) {
            Api(this).displayErrorAlert(errorMessages.asList())
        } else if (fromWorkingJobDetailsFragment) {
            val dialog = CancelWorkingDialogFragment()
            dialog.show(supportFragmentManager, "CancelWorking")
            fromWorkingJobDetailsFragment = false
        } else if (fromAppliedJobDetailsFragment) {
            val dialog = StartDialogFragment(job)
            dialog.show(supportFragmentManager, "Start")
            fromAppliedJobDetailsFragment = false
        }

        // 案件情報を取得し直して画面を描画します
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
            } else {
                refreshContents()
            }
        }
    }

    private fun refreshContents() {
        val transaction = supportFragmentManager.beginTransaction()
        // fragmentの作成
        // jobのステータスで挿しこむフラグメントを変更します
        when (job.status) {
            JobStatus.Normal -> {
                fragment = NormalJobDetailsFragment(this, job, user)
            }
            JobStatus.Applied -> {
                fragment = AppliedJobDetailsFragment(this, job, user)
            }
            JobStatus.Working -> {
                fragment = WorkingJobDetailsFragment(this, job, user)
            }
            JobStatus.Finished -> {
                fragment = FinishedJobDetailsFragment(this, job, user)
            }
            JobStatus.Reported -> {
                fragment = ReportedJobDetailsFragment(this, job, user)
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
