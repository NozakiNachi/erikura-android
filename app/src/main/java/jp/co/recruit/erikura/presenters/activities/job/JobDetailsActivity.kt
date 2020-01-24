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
    val transaction = supportFragmentManager.beginTransaction()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_details)

        Log.v("DEBUG", job.toString())
        job = intent.getParcelableExtra("job")
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
                refreshContents()
            }
        }
    }

    private fun refreshContents() {
        // fragmentの破棄
        if (fragment != null ) {
            transaction.remove(fragment)
        }
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
        // fragmentの追加
        transaction.add(R.id.job_details, fragment)
        // FIXME: 非同期処理の中で呼んでいるため?IllegalStateExceptionで落ちる
        transaction.commit()
    }
}
