package jp.co.recruit.erikura.presenters.fragments

import JobUtil
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.OwnJobQuery
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentAppliedJobsBinding
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.job.JobListAdapter
import jp.co.recruit.erikura.presenters.activities.job.JobListItemDecorator
import okhttp3.internal.wait


class AppliedJobsFragment : Fragment(), AppliedJobsHandlers {
    private val viewModel: AppliedJobsViewModel by lazy {
        ViewModelProvider(this).get(AppliedJobsViewModel::class.java)
    }

    private val monitor = Object()
    private var entriedJobFetched = false
    private var startedJobFetched = false
    private lateinit var jobListView: RecyclerView
    private lateinit var jobListAdapter: JobListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentAppliedJobsBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_applied_jobs, container, false
        )
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handlers = this

        jobListAdapter = JobListAdapter(activity!!, listOf(), currentPosition = null, timeLabelType = JobUtil.TimeLabelType.OWNED).also {
            it.onClickListner = object: JobListAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    Intent(activity, JobDetailsActivity::class.java).let { intent ->
                        intent.putExtra("job", job)
                        startActivity(intent)
                    }
                }
            }
        }
        jobListView = binding.root.findViewById(R.id.applied_jobs_recycler_view)
        jobListView.setHasFixedSize(true)
        jobListView.adapter = jobListAdapter
        jobListView.addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL))
        jobListView.addItemDecoration(JobListItemDecorator())

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        fetchEntriedJobs()
        fetchStartedJobs()

        val completable = Completable.fromAction {
            synchronized(monitor) {
                while(entriedJobFetched == false || startedJobFetched == false) {
                    try {
                        monitor.wait()
                    } catch (e: InterruptedException) {

                    }
                }
            }
            AndroidSchedulers.mainThread().scheduleDirect {
                // ページ参照のトラッキングの送出
                Tracking.logEvent(event = "view_entried_job_list_started", params = bundleOf())
                Tracking.viewJobs(
                    name = "/jobs/own/entried",
                    title = "応募した仕事画面（未実施）",
                    jobId = viewModel.appliedJobs.map { it.id })
            }
        }
        completable.subscribeOn(Schedulers.newThread())
            .subscribe()
    }

    private fun fetchEntriedJobs() {
        val api = Api(context!!)
        val errorHandler: ((messages: List<String>?) -> Unit)? = { messages ->
            api.displayErrorAlert(messages)
            synchronized(monitor) {
                entriedJobFetched = true
                monitor.notifyAll()
            }
        }

        api.ownJob(OwnJobQuery(status = OwnJobQuery.Status.ENTRIED), onError = errorHandler) { jobs ->
            viewModel.entriedJobs.value = jobs.filter { !it.isExpired }
            jobListAdapter.jobs = viewModel.appliedJobs
            jobListAdapter.notifyDataSetChanged()

            synchronized(monitor) {
                entriedJobFetched = true
                monitor.notifyAll()
            }
        }
    }

    private fun fetchStartedJobs() {
        val api = Api(context!!)
        val errorHandler: ((messages: List<String>?) -> Unit)? = { messages ->
            api.displayErrorAlert(messages)
            synchronized(monitor) {
                startedJobFetched = true
                monitor.notifyAll()
            }
        }
        api.ownJob(OwnJobQuery(status = OwnJobQuery.Status.STARTED), onError = errorHandler) { jobs ->
            viewModel.startedJobs.value = jobs.filter { !it.isExpired }
            jobListAdapter.jobs = viewModel.appliedJobs
            jobListAdapter.notifyDataSetChanged()

            synchronized(monitor) {
                startedJobFetched = true
                monitor.notifyAll()
            }
        }
    }
}

class AppliedJobsViewModel: ViewModel() {
    val entriedJobs: MutableLiveData<List<Job>> = MutableLiveData(listOf())
    val startedJobs: MutableLiveData<List<Job>> = MutableLiveData(listOf())

    val appliedJobs: List<Job> get() {
        val entried = entriedJobs.value ?: listOf()
        val started = startedJobs.value ?: listOf()
        return started + entried
    }
}

interface AppliedJobsHandlers {

}