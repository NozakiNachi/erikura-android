package jp.co.recruit.erikura.presenters.activities.job

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
import jp.co.recruit.erikura.databinding.ActivityApplyCompletedBinding

class ApplyCompletedActivity : AppCompatActivity(), ApplyCompletedEventHandlers {
    private val viewModel: ApplyCompletedViewModel by lazy {
        ViewModelProvider(this).get(ApplyCompletedViewModel::class.java)
    }

    var job: Job = Job()
    private lateinit var recommendedJobsAdapter: JobListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        job = intent.getParcelableExtra<Job>("job")
        Log.v("DEBUG", job.toString())

        val binding: ActivityApplyCompletedBinding = DataBindingUtil.setContentView(this, R.layout.activity_apply_completed)
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

    override fun onClickJobDetails(view: View) {
        val intent= Intent(this, JobDetailsActivity::class.java)
        intent.putExtra("job", job)
        startActivity(intent)
        finish()
    }

    override fun onClickSearchOtherJobs(view: View) {
        val intent = Intent(this, MapViewActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun onJobSelected(job: Job) {
        val intent= Intent(this, JobDetailsActivity::class.java)
        intent.putExtra("job", job)
        startActivity(intent)
    }
}

class ApplyCompletedViewModel: ViewModel() {
    var recommendedJobs: List<Job> = listOf()
}

interface ApplyCompletedEventHandlers {
    fun onClickJobDetails(view: View)
    fun onClickSearchOtherJobs(view: View)
}
