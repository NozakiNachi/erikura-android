package jp.co.recruit.erikura.presenters.activities.job

import android.app.ActivityOptions
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityWorkingFinishedBinding
import jp.co.recruit.erikura.presenters.activities.OwnJobsActivity

class WorkingFinishedActivity : AppCompatActivity(), WorkingFinishedEventHandlers {
    private val viewModel: WorkingFinishedViewModel by lazy {
        ViewModelProvider(this).get(WorkingFinishedViewModel::class.java)
    }

    var job: Job = Job()
    private lateinit var recommendedJobsAdapter: JobListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        job = intent.getParcelableExtra<Job>("job")
        Log.v("DEBUG", job.toString())

        val binding: ActivityWorkingFinishedBinding = DataBindingUtil.setContentView(this, R.layout.activity_working_finished)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        recommendedJobsAdapter = JobListAdapter(this, listOf(), null).also {
            it.onClickListner =  object: JobListAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    onJobSelected(job)
                }
            }
        }
        val jobList: RecyclerView = findViewById(R.id.applyCompleted_recommend)
        jobList.setHasFixedSize(true)
        jobList.adapter = recommendedJobsAdapter
        jobList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        jobList.addItemDecoration(JobListItemDecorator())
    }

    override fun onResume() {
        super.onResume()
        Api(this).recommendedJobs(job) { jobsList ->
            viewModel.recommendedJobs = jobsList
            recommendedJobsAdapter.jobs = viewModel.recommendedJobs
            recommendedJobsAdapter.notifyDataSetChanged()
        }
    }

    override fun onClickReport(view: View) {
        // FIXME: 作業報告画面へ遷移
    }

    override fun onClickAppliedJobs(view: View) {
        Intent(this, OwnJobsActivity::class.java).let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    fun onJobSelected(job: Job) {
        val intent= Intent(this, JobDetailsActivity::class.java)
        intent.putExtra("job", job)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }
}

class WorkingFinishedViewModel: ViewModel() {
    var recommendedJobs: List<Job> = listOf()
}

interface WorkingFinishedEventHandlers {
    fun onClickReport(view: View)
    fun onClickAppliedJobs(view: View)
}