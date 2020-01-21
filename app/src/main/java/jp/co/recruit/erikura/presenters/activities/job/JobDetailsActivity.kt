package jp.co.recruit.erikura.presenters.activities.job

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobStatus
import jp.co.recruit.erikura.presenters.fragments.NomalJobDetailsFragment


class JobDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_details)

        var job: Job = intent.getParcelableExtra("job")
        Log.v("DEBUG", job.toString())

        var fragment = Fragment()

        // jobのステータスで挿しこむフラグメントを変更します
        if(job != null){
            when(job.status) {
                JobStatus.Normal -> {
                    fragment = NomalJobDetailsFragment(this, job = job)
                }
                JobStatus.Applied -> {
                    // FIXME: 応募済み画面
                    fragment = NomalJobDetailsFragment(this, job = job)
                }
                JobStatus.Working -> {
                    // FIXME: 作業中画面
                    fragment = NomalJobDetailsFragment(this, job = job)
                }
                JobStatus.Finished -> {
                    // FIXME: 作業完了画面
                    fragment = NomalJobDetailsFragment(this, job = job)
                }
                JobStatus.Reported -> {
                    // FIXME: 作業報告済み画面
                    fragment = NomalJobDetailsFragment(this, job = job)
                }
                else -> {
                    fragment = NomalJobDetailsFragment(this, job = job)
                }
            }

        }

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.job_details, fragment)
        transaction.commit()

    }
}
