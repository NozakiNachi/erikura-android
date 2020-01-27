package jp.co.recruit.erikura.presenters.activities.job

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.CalendarView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.*
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityListViewBinding
import jp.co.recruit.erikura.presenters.view_models.BaseJobQueryViewModel

class ListViewActivity : AppCompatActivity(), ListViewHandlers {
    companion object {
        val REQUEST_SEARCH_CONDITIONS = 1
    }

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
        val binding: ActivityListViewBinding = DataBindingUtil.setContentView(this, R.layout.activity_list_view)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        activeJobsAdapter = JobListAdapter(this, listOf(), null).also {
            it.onClickListner =  object: JobListAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    onJobSelected(job)
                }
            }
        }
        futureJobsAdapter = JobListAdapter(this, listOf(), null).also {
            it.onClickListner =  object: JobListAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    onJobSelected(job)
                }
            }
        }
        pastJobsAdapter = JobListAdapter(this, listOf(), null).also {
            it.onClickListner =  object: JobListAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    onJobSelected(job)
                }
            }
        }

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

        // FIXME: キーワードが指定されている場合の対策を検討する
        locationManager.latLng?.let {
            firstFetchRequested = true
            val query = viewModel.query(it)
            fetchJobs(query)
        }

        Log.d("SORT TYPES", viewModel.sortLabels.toString())
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
                // FIXME: キーワードが指定されている場合の対策を検討する
                val query = viewModel.query(it)
                fetchJobs(query)
            }
        }
        locationManager.start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when(requestCode) {
                REQUEST_SEARCH_CONDITIONS -> {
                    // 検索条件を受け取る
                    data?.getParcelableExtra<JobQuery>(SearchJobActivity.EXTRA_SEARCH_CONDITIONS)?.let { query ->
                        // 検索条件を viewModel へ反映します
                        viewModel.apply(query)
                        // 案件の検索処理を実施します
                        fetchJobs(query)
                    }
                }
            }
        }
    }

    override fun onClickSearch(view: View) {
        viewModel.searchBarVisible.value = View.VISIBLE
    }

    override fun onClickSearchBar(view: View) {
        val intent = Intent(this, SearchJobActivity::class.java)
        // FIXME: 緯度経度については要検討
        intent.putExtra(SearchJobActivity.EXTRA_SEARCH_CONDITIONS, viewModel.query(locationManager.latLng ?: locationManager.defaultLatLng))
        startActivityForResult(intent, REQUEST_SEARCH_CONDITIONS)
    }

    override fun onClickMap(view: View) {
        val intent = Intent(this, MapViewActivity::class.java)
        // FIXME: 検索条件の引き継ぎについて検討する
        startActivity(intent)
        // リストビューは破棄しておきます
        finish()
    }

    override fun onToggleActiveOnly(view: View) {
        when(viewModel.periodType.value) {
            PeriodType.ALL -> {
                viewModel.periodType.value = PeriodType.ACTIVE
            }
            PeriodType.ACTIVE -> {
                viewModel.periodType.value = PeriodType.ALL
            }
        }
        onQueryChanged()
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val sortType = viewModel.sortTypes[position]
        viewModel.sortType.value = sortType
        onQueryChanged()
    }

    fun onJobSelected(job: Job) {
        val intent= Intent(this, JobDetailsActivity::class.java)
        intent.putExtra("job", job)
        startActivity(intent)
    }

    fun onQueryChanged() {
        // FIXME: キーワードが指定されている場合の対策を検討する
        locationManager.latLng?.let {
            val query = viewModel.query(it)
            fetchJobs(query)
        }
    }

    override fun onScrollChange(
        v: View,
        scrollX: Int,
        scrollY: Int,
        oldScrollX: Int,
        oldScrollY: Int
    ) {
        viewModel.searchBarVisible.value = View.GONE
    }
}

class ListViewViewModel : BaseJobQueryViewModel() {
    val resources: Resources get() = ErikuraApplication.instance.applicationContext.resources

    var jobs: List<Job> = listOf()
        set(value) {
            field = value
            activeJobs = value.filter { job -> job.isActive }
            activeListVisible.value = if(activeJobs.isEmpty()) { View.GONE } else { View.VISIBLE }
            futureJobs = value.filter { job -> job.isFuture }
            futureListVisible.value = if(futureJobs.isEmpty()) { View.GONE } else { View.VISIBLE }
            pastJobs = value.filter { job -> job.isPastOrInactive }
            pastListVisible.value = if(pastJobs.isEmpty())   { View.GONE } else { View.VISIBLE }

        }
    var activeJobs: List<Job> = listOf()
    var futureJobs: List<Job> = listOf()
    var pastJobs: List<Job> = listOf()

    val sortTypes = SortType.values()
    val sortLabels: List<String> = sortTypes.map { ErikuraApplication.applicationContext.getString(it.resourceId) }

    val activeListVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val futureListVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val pastListVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)

    val searchBarVisible: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

    val activeOnlyButtonBackground = MediatorLiveData<Drawable>().also { result ->
        result.addSource(periodType) {
            result.value = when (it) {
                PeriodType.ALL -> resources.getDrawable(R.drawable.before_open_2x, null)
                PeriodType.ACTIVE -> resources.getDrawable(R.drawable.before_entry_2x_on, null)
                else -> resources.getDrawable(R.drawable.before_open_2x, null)
            }
        }
    }

    init {
        periodType.value = PeriodType.ALL
        sortType.value = SortType.DISTANCE_ASC
    }
}

interface ListViewHandlers {
    fun onClickSearch(view: View)
    fun onClickSearchBar(view: View)
    fun onToggleActiveOnly(view: View)
    fun onClickMap(view: View)
    fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)

    fun onScrollChange(v: View, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int)
}