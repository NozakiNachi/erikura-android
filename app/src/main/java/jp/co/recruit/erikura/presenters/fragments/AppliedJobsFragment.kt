package jp.co.recruit.erikura.presenters.fragments

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.OwnJobQuery
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentAppliedJobsBinding
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.job.JobListAdapter
import jp.co.recruit.erikura.presenters.activities.job.JobListItemDecorator

class AppliedJobsFragment : Fragment(), AppliedJobsHandlers {
    private val viewModel: AppliedJobsViewModel by lazy {
        ViewModelProvider(this).get(AppliedJobsViewModel::class.java)
    }

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

        jobListAdapter = JobListAdapter(activity!!, listOf(), null).also {
            it.onClickListner = object: JobListAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    Intent(activity, JobDetailsActivity::class.java).let {
                        it.putExtra("job", job)
                        startActivity(it, ActivityOptions.makeSceneTransitionAnimation(activity!!).toBundle())
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        fetchEntriedJobs()
        fetchStartedJobs()
    }

    private fun fetchEntriedJobs() {
        Api(context!!).ownJob(OwnJobQuery(status = OwnJobQuery.Status.ENTRIED)) { jobs ->
            viewModel.entriedJobs.value = jobs
            jobListAdapter.jobs = viewModel.appliedJobs
            jobListAdapter.notifyDataSetChanged()

            // FIXME: 0件になる場合の表示内容の更新
            // FIXME: トラッキングタグの送出(jobIdが必要)
        }
    }

    private fun fetchStartedJobs() {
        Api(context!!).ownJob(OwnJobQuery(status = OwnJobQuery.Status.STARTED)) { jobs ->
            viewModel.startedJobs.value = jobs
            jobListAdapter.jobs = viewModel.appliedJobs
            jobListAdapter.notifyDataSetChanged()
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