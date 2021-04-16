package jp.co.recruit.erikura.presenters.activities.job

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
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
import java.util.*

class ApplyCompletedActivity : BaseActivity(), ApplyCompletedEventHandlers {
    private val viewModel: ApplyCompletedViewModel by lazy {
        ViewModelProvider(this).get(ApplyCompletedViewModel::class.java)
    }

    var job: Job = Job()
    private lateinit var recommendedJobsAdapter: JobListAdapter

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        job = intent.getParcelableExtra<Job>("job")
        viewModel.job.value = job
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
        viewModel.applyCompletedTitle.value = ErikuraApplication.instance.getString(R.string.applyCompleted_caption)
        if (job.isPreEntry) {
            // 先行応募経由の場合
            viewModel.applyCompletedPreEntryCaption.value = String.format(
                ErikuraApplication.instance.getString(R.string.preEntryCompleted_note),
                JobUtil.getWorkingDay(job.workingStartAt?: Date())
            )
        } else {
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
    var applyCompletedPreEntryCaption = MutableLiveData<String>()
    val job: MutableLiveData<Job> = MutableLiveData()
    val applyCompletedCaptionVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(job) {
            result.value =
                if (it.isPreEntry) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        }
    }
    val applyCompletedPreEntryCaptionVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(job) {
            result.value =
                if (it.isPreEntry) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
        }
    }
}

interface ApplyCompletedEventHandlers {
    fun onClickJobDetails(view: View)
    fun onClickSearchOtherJobs(view: View)
}
