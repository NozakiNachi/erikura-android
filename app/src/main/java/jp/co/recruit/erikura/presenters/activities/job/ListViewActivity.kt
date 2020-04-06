package jp.co.recruit.erikura.presenters.activities.job

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomnavigation.BottomNavigationView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.business.models.PeriodType
import jp.co.recruit.erikura.business.models.SortType
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityListViewBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.OwnJobsActivity
import jp.co.recruit.erikura.presenters.activities.mypage.MypageActivity
import jp.co.recruit.erikura.presenters.util.LocationManager
import jp.co.recruit.erikura.presenters.util.MessageUtils
import jp.co.recruit.erikura.presenters.view_models.BaseJobQueryViewModel

class ListViewActivity : BaseActivity(), ListViewHandlers {
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
            if (jobs.isNotEmpty()) {
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
            else {
                // クリアした検索条件での再検索を行います
                val newQuery = JobQuery(
                    latitude = locationManager.latLngOrDefault.latitude,
                    longitude = locationManager.latLngOrDefault.longitude)

                if (query != newQuery) {
                    MessageUtils.displayAlert(this, listOf("検索した地域で", "仕事が見つからなかったため、", "一番近くの仕事を表示します")) {
                        viewModel.apply(newQuery)
                        fetchJobs(newQuery)
                    }
                }
                else {
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
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityListViewBinding = DataBindingUtil.setContentView(this, R.layout.activity_list_view)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        // 下部のタブの選択肢を仕事を探すに変更
        val nav: BottomNavigationView = findViewById(R.id.list_view_navigation)
        nav.selectedItemId = R.id.tab_menu_search_jobs

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode) {
            ErikuraApplication.REQUEST_ACCESS_FINE_LOCATION_PERMISSION_ID -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    locationManager.start(this)
                }
                else {
                    MessageUtils.displayLocationAlert(this)
                }
            }
        }
    }

    override fun onClickSearch(view: View) {
        viewModel.searchBarVisible.value = View.VISIBLE
    }

    override fun onClickSearchBar(view: View) {
        val intent = Intent(this, SearchJobActivity::class.java)
        intent.putExtra(SearchJobActivity.EXTRA_SEARCH_CONDITIONS, viewModel.query(locationManager.latLngOrDefault))
        startActivityForResult(intent, REQUEST_SEARCH_CONDITIONS, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onClickMap(view: View) {
        val intent = Intent(this, MapViewActivity::class.java)
        // FIXME: 検索条件の引き継ぎについて検討する
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
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
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
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
        Log.v("MENU ITEM SELECTED: ", item.toString())
        when(item.itemId) {
            R.id.tab_menu_search_jobs -> {
                // 何も行いません
            }
            R.id.tab_menu_applied_jobs -> {
                Intent(this, OwnJobsActivity::class.java).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                }
                finish()
            }
            R.id.tab_menu_mypage -> {
                Intent(this, MypageActivity::class.java).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(
                        intent,
                        ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
                    )
                }
            }
        }
        return true
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

            if(value.isEmpty()) {
                notFoundVisibility.value = View.VISIBLE
            }
            else {
                notFoundVisibility.value = View.GONE
            }

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

    val notFoundVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

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

    fun onNavigationItemSelected(item: MenuItem): Boolean
}
