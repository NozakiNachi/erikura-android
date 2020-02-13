package jp.co.recruit.erikura.presenters.fragments

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView

import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.OwnJobQuery
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentReportedJobsBinding
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.job.JobListAdapter
import jp.co.recruit.erikura.presenters.activities.job.JobListItemDecorator
import java.text.SimpleDateFormat
import java.util.*

class ReportedJobsFragment : Fragment(), ReportedJobsHandler{
    private val viewModel: ReportedJobsViewModel by lazy {
        ViewModelProvider(this).get(ReportedJobsViewModel::class.java)
    }

    private lateinit var jobListView: RecyclerView
    private lateinit var jobListAdapter: JobListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentReportedJobsBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_reported_jobs, container, false
        )
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handlers = this

        jobListAdapter = JobListAdapter(activity!!, listOf(), currentPosition = null, timeLabelType = JobUtil.TimeLabelType.OWNED).also {
            it.onClickListner = object: JobListAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    Intent(activity, JobDetailsActivity::class.java).let { intent ->
                        intent.putExtra("job", job)
                        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity!!).toBundle())
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

        fetchReportedJobs()
    }

    private fun fetchReportedJobs() {
        // FIXME: 年月の変更時の再fetch
        // FIXME: レポート期間を ViewModel から取得する
        Api(context!!).ownJob(OwnJobQuery(status = OwnJobQuery.Status.REPORTED)) { jobs ->
            viewModel.reportedJobs.value = jobs
            jobListAdapter.jobs = viewModel.reportedJobs.value ?: listOf()
            jobListAdapter.notifyDataSetChanged()
            // FIXME: 0件になる場合の表示内容の更新
            // FIXME: トラッキングタグの送出(jobIdが必要)
        }
    }
}

class ReportedJobsViewModel: ViewModel() {
    val reportedJobs: MutableLiveData<List<Job>> = MutableLiveData()
    val months: MutableLiveData<List<Date>> = MutableLiveData()
    val monthsLabels = MediatorLiveData<List<String>>().also { result ->
        result.addSource(months) {
            val sdf = SimpleDateFormat("yyyy年MM月")
            result.value = months.value?.map { sdf.format(it) }
        }
    }

//    val jobKindsItems = MediatorLiveData<List<JobKindItem>>().also { result ->
//        result.addSource(jobKinds) {
//            val items = mutableListOf<JobKindItem>(JobKindItem.Nothing)
//            it.forEach { jobKind -> items.add(JobKindItem.Item(jobKind)) }
//            result.value = items
//        }
//    }
//
//    var workingTimes: List<Int> = listOf()
//        set(value) {
//            field = value
//            minimumWorkingTimeItems.value = (listOf(JobQuery.MIN_WORKING_TIME) + value).filterNotNull().map {
//                Log.v("TEST", "${formatWorkingTime(it)}, ${it}")
//                PickerItem(formatWorkingTime(it), it)
//            }
//            maximumWorkingTimeItems.value = (value + listOf(JobQuery.MAX_WORKING_TIME)).filterNotNull().map {
//                Log.v("TEST", "${formatWorkingTime(it)}, ${it}")
//                PickerItem(formatWorkingTime(it), it)
//            }
//        }
//    var rewards: List<Int> = listOf()
//        set(value) {
//            field = value
//            minimumRewardItems.value = (listOf(JobQuery.MIN_REWARD) + value).map { PickerItem(formatReward(it), it) }
//            maximumRewardItems.value = (value + listOf(JobQuery.MAX_REWARD)).map { PickerItem(formatReward(it), it) }
//        }
//
//    init {
//        this.workingTimes = ErikuraConfig.workingTimeRange
//        this.rewards = ErikuraConfig.rewardRange
//        this.workingTimeLabel.value = formatWorkingTimeText()
//        this.rewardLabel.value = formatRewardText()
//    }
}

interface ReportedJobsHandler {
}