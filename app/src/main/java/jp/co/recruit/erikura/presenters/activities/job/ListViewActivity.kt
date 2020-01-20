package jp.co.recruit.erikura.presenters.activities.job

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.business.models.PeriodType
import jp.co.recruit.erikura.business.models.SortType
import jp.co.recruit.erikura.data.network.Api

class ListViewActivity : AppCompatActivity(), ListViewHandlers {
    private val locationManager = ErikuraApplication.locationManager
    private var firstFetchRequested: Boolean = false
    private lateinit var activeJobsAdapter: JobListAdapter
    private lateinit var futureJobsAdapter: JobListAdapter
    private lateinit var pastJobsAdapter: JobListAdapter

    private val viewModel: ListViewViewModel by lazy {
        ViewModelProvider(this).get(ListViewViewModel::class.java)
    }

    fun fetchJobs(query: JobQuery) {
        Api(this).searchJobs(query) { jobs ->
            viewModel.jobs = jobs

            val position = LatLng(query.latitude!!, query.longitude!!)

            activeJobsAdapter.jobs = viewModel.activeJobs
            activeJobsAdapter.currentPosition = position
            activeJobsAdapter.notifyDataSetChanged()

            futureJobsAdapter.jobs = viewModel.futureJobs
            futureJobsAdapter.currentPosition = position
            futureJobsAdapter.notifyDataSetChanged()

            pastJobsAdapter.jobs = viewModel.pastJobs
            pastJobsAdapter.currentPosition = position
            pastJobsAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_view)

        activeJobsAdapter = JobListAdapter(this, listOf(), null)
        futureJobsAdapter = JobListAdapter(this, listOf(), null)
        pastJobsAdapter = JobListAdapter(this, listOf(), null)

        val activeJobList: RecyclerView = findViewById(R.id.list_view_active_job_list)
        activeJobList.setHasFixedSize(true)
        activeJobList.adapter = activeJobsAdapter
        activeJobList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        activeJobList.addItemDecoration(JobListItemDecorator())

        val futureJobList: RecyclerView = findViewById(R.id.list_view_future_job_list)
        futureJobList.setHasFixedSize(true)
        futureJobList.adapter = futureJobsAdapter
        futureJobList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        futureJobList.addItemDecoration(JobListItemDecorator())

        val pastJobList: RecyclerView = findViewById(R.id.list_view_past_job_list)
        pastJobList.setHasFixedSize(true)
        pastJobList.adapter = pastJobsAdapter
        pastJobList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        pastJobList.addItemDecoration(JobListItemDecorator())

        if (!locationManager.checkPermission(this)) {
            locationManager.requestPermission(this)
        }

        locationManager.latLng?.let {
            firstFetchRequested = true
            val query = viewModel.query(it)
            fetchJobs(query)
        }
    }

    override fun onPause() {
        super.onPause()
        locationManager.stop()
        locationManager.clearLocationUpdateCallback()
    }

    override fun onResume() {
        super.onResume()
        locationManager.addLocationUpdateCallback {
            if (!firstFetchRequested) {
                firstFetchRequested = true
                val query = viewModel.query(it)
                fetchJobs(query)
            }
        }
        locationManager.start(this)
    }

}

class ListViewViewModel : ViewModel() {
    var jobs: List<Job> = listOf()
        set(value) {
            field = value
            activeJobs = value.filter { job -> job.isActive }
            futureJobs = value.filter { job -> job.isFuture }
            pastJobs = value.filter { job -> job.isPastOrInactive }

        }
    var activeJobs: List<Job> = listOf()
    var futureJobs: List<Job> = listOf()
    var pastJobs: List<Job> = listOf()
    val periodType: MutableLiveData<PeriodType> = MutableLiveData()

    val sortTypes = SortType.values()
    val sortLabels = sortTypes.map { ErikuraApplication.applicationContext.getString(it.resourceId) }

////    val keyword: MutableLiveData<String> = MutableLiveData()
////    val minimumReward: MutableLiveData<Int> = MutableLiveData()
////    val maximumReward: MutableLiveData<Int> = MutableLiveData()
////    val minimumWorkingTime: MutableLiveData<Int> = MutableLiveData()
////    val maximumWorkingTime: MutableLiveData<Int> = MutableLiveData()
////    val jobKind: MutableLiveData<JobKind> = MutableLiveData()
//
//    val activeOnlyButtonBackground = MediatorLiveData<Drawable>().also { result ->
//        result.addSource(periodType) {
//            result.value = when (it) {
//                PeriodType.ALL -> resources.getDrawable(R.drawable.before_open_2x, null)
//                PeriodType.ACTIVE -> resources.getDrawable(R.drawable.before_entry_2x_on, null)
//                else -> resources.getDrawable(R.drawable.before_open_2x, null)
//            }
//        }
//    }
//val wantedVisibility: Int get() {
//    if (job.wanted) {
//        return View.VISIBLE
//    }
//    else {
//        return View.GONE
//    }
//}

    val activeListVisible: Int get() = if(activeJobs.isEmpty()) { View.GONE } else { View.VISIBLE }
    val futureListVisible: Int get() = if(futureJobs.isEmpty()) { View.GONE } else { View.VISIBLE }
    val pastListVisible:   Int get() = if(pastJobs.isEmpty())   { View.GONE } else { View.VISIBLE }

    init {
        periodType.value = PeriodType.ALL
    }

    fun query(latLng: LatLng): JobQuery {
        val query = JobQuery(
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            period = this.periodType.value ?: PeriodType.ALL
        )
        return query
    }
}

interface ListViewHandlers {
}
