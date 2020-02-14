package jp.co.recruit.erikura.presenters.fragments

import JobUtil
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
import jp.co.recruit.erikura.databinding.FragmentFinishedJobsBinding
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.job.JobListAdapter
import jp.co.recruit.erikura.presenters.activities.job.JobListItemDecorator

class FinishedJobsFragment : Fragment(), FinishedJobsHandlers {
    private val viewModel: FinishedJobsViewModel by lazy {
        ViewModelProvider(this).get(FinishedJobsViewModel::class.java)
    }
    private lateinit var jobListView: RecyclerView
    private lateinit var jobListAdapter: JobListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding: FragmentFinishedJobsBinding = DataBindingUtil.inflate(
            inflater,R.layout.fragment_finished_jobs, container, false
        )
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handlers = this

        jobListAdapter = JobListAdapter(activity!!, listOf(), currentPosition = null, timeLabelType = JobUtil.TimeLabelType.OWNED).also{
            it.onClickListner = object: JobListAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    Intent(activity, JobDetailsActivity::class.java).let {
                        it.putExtra("job", job)
                        startActivity(it, ActivityOptions.makeSceneTransitionAnimation(activity!!).toBundle())
                    }
                }
            }
        }
        jobListView = binding.root.findViewById(R.id.finished_jobs_recycler_view)
        jobListView.adapter = jobListAdapter
        jobListView.addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL))
        jobListView.addItemDecoration(JobListItemDecorator())

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        fetchFinishedJobs()
    }

    private fun fetchFinishedJobs() {
        Api(context!!).ownJob(OwnJobQuery(status = OwnJobQuery.Status.FINISHED)) { jobs ->
            viewModel.finishedJobs.value = jobs.filter { !it.isExpired }
            jobListAdapter.jobs = viewModel.unreportedJobs
            jobListAdapter.notifyDataSetChanged()
        }
    }
}

class FinishedJobsViewModel: ViewModel(){
    val finishedJobs: MutableLiveData<List<Job>> = MutableLiveData(listOf())

    val unreportedJobs: List<Job> get() {
        val unreported = finishedJobs.value ?: listOf()
        return unreported
    }
}

interface FinishedJobsHandlers {

}