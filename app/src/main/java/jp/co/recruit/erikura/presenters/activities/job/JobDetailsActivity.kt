package jp.co.recruit.erikura.presenters.activities.job

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobStatus
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.fragments.*

class JobDetailsActivity : BaseActivity() {
    var renderedJobId: Int? = null
    var renderedJobStatus: JobStatus? = null
    var job: Job = Job()
    var user: User? = null
    var fragment: BaseJobDetailFragment? = null
    var fromAppliedJobDetailsFragment: Boolean = false
    var fromWorkingJobDetailsFragment: Boolean = false
    var fromIdentify: Boolean = false

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
        fromIdentify = intent.getBooleanExtra("fromIdentify", false)

        // 現時点での案件情報をもとに画面を構築します
        if (jobRestored) {
            refreshContents()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent

        var jobRestored: Boolean = false
        intent?.let { intent ->
            val value = intent.getParcelableExtra<Job>("job")
            if (value != null) {
                jobRestored = true
                job = value
                ErikuraApplication.instance.currentJob = job
            } else {
                handleIntent(intent)
            }
            Log.v("DEBUG", job.toString())

            // 身分確認からの場合
            fromIdentify = intent.getBooleanExtra("fromIdentify", false)
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
        supportFragmentManager.executePendingTransactions()

        val errorMessages = intent.getStringArrayExtra("errorMessages")
        val message = intent.getStringExtra("message")
        if (errorMessages != null) {
            Api(this).displayErrorAlert(errorMessages.asList())
        } else if (fromWorkingJobDetailsFragment) {
            val dialog = CancelWorkingDialogFragment()
            dialog.show(supportFragmentManager, "CancelWorking")
            fromWorkingJobDetailsFragment = false
        } else if (fromAppliedJobDetailsFragment) {
            val dialog = StartDialogFragment.newInstance(job, message)
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
            ErikuraApplication.instance.currentJob = it
            if (Api.isLogin) {
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
        if (isDestroyed) { return }

        if (job.status == JobStatus.Uninitialized) {
            fragment?.let {
                val transaction = supportFragmentManager.beginTransaction()
                transaction.remove(it)
                transaction.commitAllowingStateLoss()
                renderedJobId = null
                renderedJobStatus = null
            }
        }
        else if (job.id == renderedJobId && job.status == renderedJobStatus) {
            fragment?.refresh(job, user)
            // intentを受け取ってfromIdentifyが更新されている可能性があるので身分確認のフラグも更新する
            fragment?.arguments?.let { args ->
                args.putBoolean("fromIdentify", fromIdentify)
                fromIdentify = false
            }
        }
        else {
            val transaction = supportFragmentManager.beginTransaction()
            // fragmentの作成
            // jobのステータスで挿しこむフラグメントを変更します
            when (job.status) {
                JobStatus.Normal -> {
                    fragment = NormalJobDetailsFragment.newInstance(job, user, fromIdentify)
                }
                JobStatus.Applied -> {
                    fragment = AppliedJobDetailsFragment.newInstance(job, user)
                }
                JobStatus.Working -> {
                    fragment = WorkingJobDetailsFragment.newInstance(job, user)
                }
                JobStatus.Finished -> {
                    fragment = FinishedJobDetailsFragment.newInstance(job, user)
                }
                JobStatus.Reported -> {
                    fragment = ReportedJobDetailsFragment.newInstance(job, user)
                }
                else -> {
                    fragment = NormalJobDetailsFragment.newInstance(job, user, fromIdentify)
                }
            }
            // fragmentの更新
            transaction.replace(R.id.job_details, fragment!!)
            transaction.commitAllowingStateLoss()
            renderedJobId = job.id
            renderedJobStatus = job.status
        }
    }

    private fun handleIntent(intent: Intent) {
        val appLinkData: Uri? = intent.data
        val jobId = appLinkData!!.lastPathSegment!!.toInt()
        job = Job(id= jobId)
        job.uninitialized = true
        ErikuraApplication.instance.currentJob = job
    }
}
