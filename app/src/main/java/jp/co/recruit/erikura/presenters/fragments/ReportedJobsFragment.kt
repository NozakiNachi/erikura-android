package jp.co.recruit.erikura.presenters.fragments

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView

import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.OwnJobQuery
import jp.co.recruit.erikura.business.util.DateUtils
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

        fetchReportedJobs()
    }

    override fun onTargetMonthSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.months.value?.let {
            val item = it[position]
            viewModel.targetMonth.value = item

            // 取り直しを行います
            fetchReportedJobs()
        }
    }


    private fun fetchReportedJobs() {
        val targetMonth = viewModel.targetMonth.value ?: Date()
        val startDate = DateUtils.beginningOfMonth(targetMonth)
        val endDate = DateUtils.endOfMonth(targetMonth)

        Api(context!!).ownJob(OwnJobQuery(status = OwnJobQuery.Status.REPORTED, reportedFrom = startDate, reportedTo = endDate)) { jobs ->
            viewModel.reportedJobs.value = jobs
            jobListAdapter.jobs = viewModel.reportedJobs.value ?: listOf()
            jobListAdapter.notifyDataSetChanged()

            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_entried_job_list_reported", params= bundleOf())
            Tracking.viewJobs(name= "/jobs/own/reported", title= "応募した仕事画面（報告済み）", jobId= jobListAdapter.jobs.map { it.id })
        }
    }
}

class ReportedJobsViewModel: ViewModel() {
    val targetMonth: MutableLiveData<Date> = MutableLiveData()
    val reportedJobs: MutableLiveData<List<Job>> = MutableLiveData()
    val months: MutableLiveData<List<Date>> = MutableLiveData()
    val monthsLabels = MediatorLiveData<List<String>>().also { result ->
        result.addSource(months) {
            val sdf = SimpleDateFormat("yyyy年MM月")
            result.value = months.value?.map { sdf.format(it) }
        }
    }

    init {
        // 選択可能な年月を取得します
        val currentMonth = DateUtils.truncate(Date(), Calendar.MONTH)
        val cal = Calendar.getInstance()
        cal.time = currentMonth
        cal.add(Calendar.YEAR, -3)
        val monthsList = mutableListOf<Date>()
        while (cal.time <= currentMonth) {
            monthsList.add(cal.time)
            cal.add(Calendar.MONTH, 1)
        }
        months.value = monthsList.reversed()
        targetMonth.value = currentMonth
    }
}

interface ReportedJobsHandler {
    fun onTargetMonthSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
}