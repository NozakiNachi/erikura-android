package jp.co.recruit.erikura.presenters.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.PlaceJobType
import jp.co.recruit.erikura.databinding.FragmentJobsListBinding
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.job.JobListAdapter
import jp.co.recruit.erikura.presenters.activities.job.JobListItemDecorator

class JobsListFragment(private val jobs: Map<PlaceJobType, List<Job>>) : Fragment() {
    private val viewModel: JobsListFragmentViewModel by lazy {
        ViewModelProvider(this).get(JobsListFragmentViewModel::class.java)
    }

    private lateinit var activeJobsAdapter: JobListAdapter
    private lateinit var futureJobsAdapter: JobListAdapter
    private lateinit var pastJobsAdapter: JobListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding = FragmentJobsListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        viewModel.activeJobs = jobs[PlaceJobType.ACTIVE]?: listOf()
        viewModel.futureJobs = jobs[PlaceJobType.FUTURE]?: listOf()
        viewModel.pastJobs = jobs[PlaceJobType.PAST]?: listOf()
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activeJobsAdapter = JobListAdapter(activity!!, listOf(), null).also {
            it.onClickListner =  object: JobListAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    onJobSelected(job)
                }
            }
        }
        activeJobsAdapter.jobs = viewModel.activeJobs
        activeJobsAdapter.notifyDataSetChanged()

        futureJobsAdapter = JobListAdapter(activity!!, listOf(), null).also {
            it.onClickListner =  object: JobListAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    onJobSelected(job)
                }
            }
        }
        futureJobsAdapter.jobs = viewModel.futureJobs
        futureJobsAdapter.notifyDataSetChanged()

        pastJobsAdapter = JobListAdapter(activity!!, listOf(), null).also {
            it.onClickListner =  object: JobListAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    onJobSelected(job)
                }
            }
        }
        pastJobsAdapter.jobs = viewModel.pastJobs
        pastJobsAdapter.notifyDataSetChanged()

        val activeJobList: RecyclerView = activity!!.findViewById(R.id.jobsList_activeJobs)
        activeJobList.setHasFixedSize(true)
        activeJobList.adapter = activeJobsAdapter
        activeJobList.addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL))
        activeJobList.addItemDecoration(JobListItemDecorator())

        val futureJobList: RecyclerView = activity!!.findViewById(R.id.jobsList_futureJobs)
        futureJobList.setHasFixedSize(true)
        futureJobList.adapter = futureJobsAdapter
        futureJobList.addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL))
        futureJobList.addItemDecoration(JobListItemDecorator())

        val pastJobList: RecyclerView = activity!!.findViewById(R.id.jobsList_pastJobs)
        pastJobList.setHasFixedSize(true)
        pastJobList.adapter = pastJobsAdapter
        pastJobList.addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL))
        pastJobList.addItemDecoration(JobListItemDecorator())
    }

    fun onJobSelected(job: Job) {
        val intent= Intent(activity, JobDetailsActivity::class.java)
        intent.putExtra("job", job)
        startActivity(intent)
    }

}

class JobsListFragmentViewModel: ViewModel() {
    var activeJobs: List<Job> = listOf()
        set(value) {
            field = value
            activeListVisible.value = if(field.isEmpty()) { View.GONE } else { View.VISIBLE }
        }
    var futureJobs: List<Job> = listOf()
        set(value) {
            field = value
            futureListVisible.value = if(field.isEmpty()) { View.GONE } else { View.VISIBLE }
        }
    var pastJobs: List<Job> = listOf()
        set(value) {
            field = value
            pastListVisible.value = if(field.isEmpty())   { View.GONE } else { View.VISIBLE }
        }

    val activeListVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val futureListVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val pastListVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
}
