package jp.co.recruit.erikura.presenters.activities.job

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobStatus
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.presenters.fragments.NormalJobDetailsFragment


class JobDetailsActivity : AppCompatActivity() {

    var job: Job = Job()
    var fragment = Fragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_details)

        job = intent.getParcelableExtra("job")
        Log.v("DEBUG", job.toString())
    }


    override fun onResume() {
        super.onResume()
        fetchJob()
    }

    private fun fetchJob() {
        if (job != null) {
            // jobの再取得
            Api(this).reloadJob(job) {
                it.toString()
                job = it
                refreshContents()
            }
        }
    }

    private fun refreshContents() {
        val transaction = supportFragmentManager.beginTransaction()
        // fragmentの作成
        // jobのステータスで挿しこむフラグメントを変更します
        if(job != null){
            when(job.status) {
                JobStatus.Normal -> {
                    fragment = NormalJobDetailsFragment(this, job)
                }
                JobStatus.Applied -> {
                    // FIXME: 応募済み画面
                    fragment = NormalJobDetailsFragment(this, job)
                }
                JobStatus.Working -> {
                    // FIXME: 作業中画面
                    fragment = NormalJobDetailsFragment(this, job)
                }
                JobStatus.Finished -> {
                    // FIXME: 作業完了画面
                    fragment = NormalJobDetailsFragment(this, job)
                }
                JobStatus.Reported -> {
                    // FIXME: 作業報告済み画面
                    fragment = NormalJobDetailsFragment(this, job)
                }
                else -> {
                    fragment = NormalJobDetailsFragment(this, job)
                }
            }

        }
        // fragmentの更新
        transaction.replace(R.id.job_details, fragment)
        transaction.commit()
    }
}
