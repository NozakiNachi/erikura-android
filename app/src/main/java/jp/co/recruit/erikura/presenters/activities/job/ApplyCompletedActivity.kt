package jp.co.recruit.erikura.presenters.activities.job

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityApplyCompletedBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity

class ApplyCompletedActivity : BaseActivity(), ApplyCompletedEventHandlers {
    private val viewModel: ApplyCompletedViewModel by lazy {
        ViewModelProvider(this).get(ApplyCompletedViewModel::class.java)
    }

    var job: Job = Job()
    var fromPreEntry: Boolean = false
    private lateinit var recommendedJobsAdapter: JobListAdapter

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        job = intent.getParcelableExtra<Job>("job")
        fromPreEntry = intent.getBooleanExtra("fromPreEntry", false)
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
        if (fromPreEntry) {
            viewModel.applyCompletedTitle.value = ErikuraApplication.instance.getString(R.string.preEntryCompleted_caption)
            viewModel.applyCompletedCaption.value = ErikuraApplication.instance.getString(R.string.preEntryCompleted_note)
        } else {
            viewModel.applyCompletedTitle.value = ErikuraApplication.instance.getString(R.string.applyCompleted_caption)
            viewModel.applyCompletedCaption.value = ErikuraApplication.instance.getString(R.string.applyCompleted_note)
        }
    }

    override fun onStart() {
        super.onStart()

        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_job_entry_finish", params = bundleOf())
        Tracking.viewJobDetails(name= "/enrtries/completed/${job?.id ?: 0}", title= "応募完了画面", jobId= job?.id ?: 0)
    }

    override fun onResume() {
        super.onResume()
        Api(this).recommendedJobs(job) { jobsList ->
            viewModel.recommendedJobs = jobsList
            recommendedJobsAdapter.jobs = viewModel.recommendedJobs
            recommendedJobsAdapter.notifyDataSetChanged()

            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "dispaly_list_near_job", params= bundleOf())
            Tracking.trackJobs(name= "dispaly_list_near_job", jobId= jobsList.map { it.id })
        }
    }

    override fun onClickJobDetails(view: View) {
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "push_job_detail", params= bundleOf())
        Tracking.trackJobDetails(name= "push_job_detail", jobId= job?.id ?: 0)

        val intent= Intent(this, JobDetailsActivity::class.java)
        intent.putExtra("job", job)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
        finish()
    }

    override fun onClickSearchOtherJobs(view: View) {
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "push_find_other_job", params= bundleOf())
        Tracking.track(name= "push_find_other_job")

        val intent = Intent(this, MapViewActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun onJobSelected(job: Job) {
        val intent= Intent(this, JobDetailsActivity::class.java)
        intent.putExtra("job", job)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
    }
}

class ApplyCompletedViewModel: ViewModel() {
    var recommendedJobs: List<Job> = listOf()
    var applyCompletedTitle = MutableLiveData<String>()
    var applyCompletedCaption = MutableLiveData<String>()
}

interface ApplyCompletedEventHandlers {
    fun onClickJobDetails(view: View)
    fun onClickSearchOtherJobs(view: View)
}
