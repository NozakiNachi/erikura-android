package jp.co.recruit.erikura.presenters.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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

class JobsListFragment : Fragment() {
    companion object {
        const val ACTIVE_JOBS_ARGUMENT = "activeJobs"
        const val PRE_ENTRY_JOBS_ARGUMENT = "preEntryJobs"
        const val FUTURE_JOBS_ARGUMENTS = "futureJobs"
        const val PAST_JOBS_ARGUMENTS = "pastJobs"

        fun newInstance(jobs: Map<PlaceJobType, List<Job>>): JobsListFragment {
            return JobsListFragment().also {
                it.arguments = Bundle().also { args ->
                    args.putParcelableArrayList(ACTIVE_JOBS_ARGUMENT, ArrayList(jobs[PlaceJobType.ACTIVE] ?: listOf()))
                    args.putParcelableArrayList(PRE_ENTRY_JOBS_ARGUMENT, ArrayList(jobs[PlaceJobType.PRE_ENTRY] ?: listOf()))
                    args.putParcelableArrayList(FUTURE_JOBS_ARGUMENTS, ArrayList(jobs[PlaceJobType.FUTURE] ?: listOf()))
                    args.putParcelableArrayList(PAST_JOBS_ARGUMENTS, ArrayList(jobs[PlaceJobType.PAST] ?: listOf()))
                }
            }
        }
    }

    private val viewModel: JobsListFragmentViewModel by lazy {
        ViewModelProvider(this).get(JobsListFragmentViewModel::class.java)
    }

    private lateinit var jobs: Map<PlaceJobType, List<Job>>
    private lateinit var activeJobsAdapter: JobListAdapter
    private lateinit var preEntryJobsAdapter: JobListAdapter
    private lateinit var futureJobsAdapter: JobListAdapter
    private lateinit var pastJobsAdapter: JobListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.also { args ->
            val activeJobs: List<Job> = args.getParcelableArrayList<Job>(ACTIVE_JOBS_ARGUMENT)?.toList() ?: listOf()
            val preEntryJobs: List<Job> = args.getParcelableArrayList<Job>(PRE_ENTRY_JOBS_ARGUMENT)?.toList() ?: listOf()
            val futureJobs: List<Job> = args.getParcelableArrayList<Job>(FUTURE_JOBS_ARGUMENTS)?.toList() ?: listOf()
            val pastJobs: List<Job> = args.getParcelableArrayList<Job>(PAST_JOBS_ARGUMENTS)?.toList() ?: listOf()
            jobs = mapOf(
                PlaceJobType.ACTIVE to activeJobs,
                PlaceJobType.PRE_ENTRY to preEntryJobs,
                PlaceJobType.FUTURE to futureJobs,
                PlaceJobType.PAST   to pastJobs
            )
        } ?: run {
            jobs = mapOf()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding = FragmentJobsListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        viewModel.activeJobs = jobs[PlaceJobType.ACTIVE]?: listOf()
        viewModel.preEntryJobs = jobs[PlaceJobType.PRE_ENTRY]?: listOf()
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

        preEntryJobsAdapter = JobListAdapter(activity!!, listOf(), null).also {
            it.onClickListner =  object: JobListAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    onJobSelected(job)
                }
            }
        }
        preEntryJobsAdapter.jobs = viewModel.preEntryJobs
        preEntryJobsAdapter.notifyDataSetChanged()

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

        val preEntryJobList: RecyclerView = activity!!.findViewById(R.id.jobsList_preEntryJobs)
        preEntryJobList.setHasFixedSize(true)
        preEntryJobList.adapter = preEntryJobsAdapter
        preEntryJobList.addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL))
        preEntryJobList.addItemDecoration(JobListItemDecorator())

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
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
    }

}

class JobsListFragmentViewModel: ViewModel() {
    var activeJobs: List<Job> = listOf()
        set(value) {
            field = value
            activeListVisible.value = if(field.isEmpty()) { View.GONE } else { View.VISIBLE }
        }
    var preEntryJobs: List<Job> = listOf()
        set(value) {
            field = value
            preEntryListVisible.value = if(field.isEmpty()) { View.GONE } else { View.VISIBLE }
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
    val preEntryListVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val futureListVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val pastListVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
}
