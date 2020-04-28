package jp.co.recruit.erikura.presenters.activities.job

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import io.reactivex.android.schedulers.AndroidSchedulers
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.business.models.PeriodType
import jp.co.recruit.erikura.business.models.SortType
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityListViewBinding
import jp.co.recruit.erikura.presenters.activities.BaseTabbedActivity
import jp.co.recruit.erikura.presenters.activities.TabEventHandlers
import jp.co.recruit.erikura.presenters.util.LocationManager
import jp.co.recruit.erikura.presenters.view_models.BaseJobQueryViewModel

class ListViewActivity : BaseTabbedActivity(R.id.tab_menu_search_jobs), ListViewHandlers {
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
        Api(this).searchJobs(query, runCompleteOnUIThread = false) { jobs ->
            val activeJobs = mutableListOf<Job>()
            val futureJobs = mutableListOf<Job>()
            val pastJobs = mutableListOf<Job>()
            jobs.forEach { job ->
                when {
                    job.isActive -> activeJobs.add(job)
                    job.isFuture -> futureJobs.add(job)
                    job.isPastOrInactive -> pastJobs.add(job)
                }
            }

            AndroidSchedulers.mainThread().scheduleDirect {
                viewModel.activeJobs = activeJobs
                viewModel.futureJobs = futureJobs
                viewModel.pastJobs = pastJobs

                val position = LatLng(query.latitude!!, query.longitude!!)

                activeJobsAdapter.jobs = viewModel.activeJobs
                activeJobsAdapter.currentPosition = position
                activeJobsAdapter.notifyDataSetChanged()
                viewModel.activeListVisible.value = if (viewModel.activeJobs.isEmpty()) { View.GONE } else { View.VISIBLE }

                futureJobsAdapter.jobs = viewModel.futureJobs
                futureJobsAdapter.currentPosition = position
                futureJobsAdapter.notifyDataSetChanged()
                viewModel.futureListVisible.value = if (viewModel.futureJobs.isEmpty()) { View.GONE } else { View.VISIBLE }

                pastJobsAdapter.jobs = viewModel.pastJobs
                pastJobsAdapter.currentPosition = position
                pastJobsAdapter.notifyDataSetChanged()
                viewModel.pastListVisible.value = if (viewModel.pastJobs.isEmpty()) { View.GONE } else { View.VISIBLE }

                if (jobs.isNotEmpty()) {
                    viewModel.notFoundVisibility.value = View.GONE
                }
                else {
                    viewModel.notFoundVisibility.value = View.VISIBLE
                }

                val jobId = jobs.map { it.id }
                Tracking.logEvent(event= "view_job_list", params= bundleOf())
                Tracking.viewJobs(name= "/jobs/list", title= "仕事一覧画面（リスト）", jobId= jobId)
                // 仕事表示のトラッキングの送出
                Tracking.logEvent(event= "dispaly_job_list", params= bundleOf())
                Tracking.viewJobs(name= "dispaly_job_list", title= "仕事一覧表示（リスト）", jobId= jobId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityListViewBinding = DataBindingUtil.setContentView(this, R.layout.activity_list_view)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        intent.getParcelableExtra<JobQuery>(SearchJobActivity.EXTRA_SEARCH_CONDITIONS)?.let { query ->
            viewModel.apply(query)
        }

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

        if (viewModel.keyword.value.isNullOrBlank()) {
            // 現在地からの検索の場合
            locationManager.latLng?.also {
                firstFetchRequested = true
                val query = viewModel.query(it)
                fetchJobs(query)
            } ?: run {
                if (!locationManager.checkPermission(this)) {
                    firstFetchRequested = true
                    val query = viewModel.query(LocationManager.defaultLatLng)
                    fetchJobs(query)
                }
            }
        }
        else {
            // キーワードをもとにした緯度経度からの検索
            viewModel.latLng.value?.let {
                firstFetchRequested = true
                val query = viewModel.query(it)
                fetchJobs(query)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        locationManager.stop()
        locationManager.clearLocationUpdateCallback()

        searchJobQuery = viewModel.query(viewModel.latLng.value ?: LocationManager.defaultLatLng)
    }

    override fun onResume() {
        super.onResume()
        // 仕事を探すタブの画面を保存します
        searchJobCurrentActivity = this.javaClass

        locationManager.addLocationUpdateCallback {
            if (!firstFetchRequested && !viewModel.keyword.value.isNullOrBlank()) {
                firstFetchRequested = true
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationManager.onRequestPermissionResult(this, requestCode, permissions, grantResults)
    }

    override fun onClickSearch(view: View) {
        viewModel.searchBarVisible.value = View.VISIBLE
    }

    override fun onClickSearchBar(view: View) {
        val intent = Intent(this, SearchJobActivity::class.java)
        intent.putExtra(SearchJobActivity.EXTRA_SEARCH_CONDITIONS, viewModel.query(locationManager.latLngOrDefault))
        startActivityForResult(intent, REQUEST_SEARCH_CONDITIONS)
    }

    override fun onClickMap(view: View) {
        navigateToMapView()
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
        // viewModel 側に実装を移すべきか検討すること
        val latLng = viewModel.keyword.value?.let { viewModel.latLng.value } ?: locationManager.latLngOrDefault
        val query = viewModel.query(latLng)
        fetchJobs(query)
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // 仕事を探すがタップされている場合は、地図画面に遷移させます
        if (item.itemId == R.id.tab_menu_search_jobs) {
            Log.v(ErikuraApplication.LOG_TAG, "Navigation Item Selected: ${item.toString()}")

            navigateToMapView()
            return true
        }

        // それ以外はデフォルトの動作を行います
        return super.onNavigationItemSelected(item)
    }

    private fun navigateToMapView() {
        // 表示変更のトラッキングの送出
        Tracking.logEvent(event= "push_toggle_dispaly", params= bundleOf())
        Tracking.track(name= "push_toggle_dispaly")

        Intent(this, MapViewActivity::class.java).let {
            it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            it.putExtra(SearchJobActivity.EXTRA_SEARCH_CONDITIONS, viewModel.query(viewModel.latLng.value ?: LocationManager.defaultLatLng))
            startActivity(it)
        }
    }
}

class ListViewViewModel : BaseJobQueryViewModel() {
    val resources: Resources get() = ErikuraApplication.instance.applicationContext.resources

    /*
    var jobs: List<Job> = listOf()
        set(value) {
            field = value
            activeJobs = value.filter { job -> job.isActive }
            activeListVisible.value = if(activeJobs.isEmpty()) { View.GONE } else { View.VISIBLE }
            futureJobs = value.filter { job -> job.isFuture }
            futureListVisible.value = if(futureJobs.isEmpty()) { View.GONE } else { View.VISIBLE }
            pastJobs = value.filter { job -> job.isPastOrInactive }
            pastListVisible.value = if(pastJobs.isEmpty())   { View.GONE } else { View.VISIBLE }

            if(value.isEmpty()) {
                notFoundVisibility.value = View.VISIBLE
            }
            else {
                notFoundVisibility.value = View.GONE
            }

        }
         */
    var activeJobs: List<Job> = listOf()
    var futureJobs: List<Job> = listOf()
    var pastJobs: List<Job> = listOf()

    val sortTypes = SortType.values()
    val sortLabels: List<String> = sortTypes.map { ErikuraApplication.applicationContext.getString(it.resourceId) }

    val sortTypeId = MediatorLiveData<Int>().also { result ->
        result.addSource(sortType) { result.value = sortTypes.indexOf(it) }
    }

    val activeListVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val futureListVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val pastListVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)

    val searchBarVisible: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

    val notFoundVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    val activeOnlyButtonBackground = MediatorLiveData<Drawable>().also { result ->
        result.addSource(periodType) {
            result.value = when (it) {
                PeriodType.ALL -> resources.getDrawable(R.drawable.before_open_500w, null)
                PeriodType.ACTIVE -> resources.getDrawable(R.drawable.before_entry_500w_on, null)
                else -> resources.getDrawable(R.drawable.before_open_500w, null)
            }
        }
    }

    init {
        periodType.value = PeriodType.ALL
        sortType.value = SortType.DISTANCE_ASC
    }
}

interface ListViewHandlers: TabEventHandlers {
    fun onClickSearch(view: View)
    fun onClickSearchBar(view: View)
    fun onToggleActiveOnly(view: View)
    fun onClickMap(view: View)
    fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)

    fun onScrollChange(v: View, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int)
}
