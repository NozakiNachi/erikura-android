package jp.co.recruit.erikura.presenters.activities.job

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobStatus
import jp.co.recruit.erikura.presenters.fragments.NomalJobDetailsFragment


class JobDetailsActivity : AppCompatActivity() {

    val job: MutableLiveData<Job> = MutableLiveData()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_details)

        Log.v("DEBUG", job.toString())

        var fragment: Fragment = Fragment()

        // jobのステータスで挿しこむフラグメントを変更します
        if(job.value != null){
            when(job.value!!.status) {
                JobStatus.Normal -> {
                    fragment = NomalJobDetailsFragment(job = job.value)
                }
                JobStatus.Applied -> {
                    // FIXME: 応募済み画面
                    fragment = NomalJobDetailsFragment(job = job.value)
                }
                JobStatus.Working -> {
                    // FIXME: 作業中画面
                    fragment = NomalJobDetailsFragment(job = job.value)
                }
                JobStatus.Finished -> {
                    // FIXME: 作業完了画面
                    fragment = NomalJobDetailsFragment(job = job.value)
                }
                JobStatus.Reported -> {
                    // FIXME: 作業報告済み画面
                    fragment = NomalJobDetailsFragment(job = job.value)
                }
                else -> {
                    fragment = NomalJobDetailsFragment(job = job.value)
                }
            }

        }

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.job_details, fragment)
        transaction.commit()

    }
}
