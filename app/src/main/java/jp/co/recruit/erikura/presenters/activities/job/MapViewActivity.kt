package jp.co.recruit.erikura.presenters.activities.job

import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import io.realm.Realm
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.business.models.PeriodType
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityMapViewBinding
import jp.co.recruit.erikura.presenters.fragments.ErikuraMarkerView
import jp.co.recruit.erikura.presenters.util.LocationManager

class MapViewActivity : AppCompatActivity(), OnMapReadyCallback, MapViewEventHandlers {
    companion object {
        // デフォルト位置情報
        val defaultLatLng = LatLng(35.658322, 139.70163)
        val defaultZoom = 15.0f
    }

    private val viewModel: MapViewViewModel by lazy {
        ViewModelProvider(this).get(MapViewViewModel::class.java)
    }

    val locationManager: LocationManager = ErikuraApplication.locationManager

    private lateinit var mMap: GoogleMap
    private lateinit var carouselView: RecyclerView
    private var firstFetchRequested: Boolean = false

    private fun summarizeJobsByLocation(jobs: List<Job>): Map<LatLng, List<Job>> {
        val jobsByLocation = HashMap<LatLng, MutableList<Job>>()
        jobs.forEach { job ->
            val latLng = job.latLng
            if (!jobsByLocation.containsKey(latLng)) {
                jobsByLocation.put(latLng, mutableListOf(job))
            }
            else {
                val list = jobsByLocation[latLng] ?: mutableListOf()
                list.add(job)
            }
        }
        return jobsByLocation
    }

    private fun fetchJobs(query: JobQuery) {
        Api(this@MapViewActivity).searchJobs(query) { jobs ->
            Log.d("JOBS: ", jobs.toString())
            viewModel.jobs.value = jobs
            viewModel.jobsByLocation.value = summarizeJobsByLocation(jobs)
            // マーカー所の情報をクリアします
            viewModel.markerMap.clear()
            viewModel.activeMaker = null
            // マーカーを地図から削除します
            mMap.clear()

            jobs.forEachIndexed { i, job ->
                val erikuraMarker = ErikuraMarkerView.build(this, mMap, job) { marker ->
                    // 表示順
                    marker.zIndex = ErikuraMarkerView.BASE_ZINDEX - i
                    if (job.isStartSoon) {
                        marker.zIndex += ErikuraMarkerView.SOON_ZINDEX_OFFSET
                    }
                    else if (job.isFuture) {
                        marker.zIndex += ErikuraMarkerView.FUTURE_ZINDEX_OFFSET
                    }
                    else if (job.isPastOrInactive) {
                        marker.zIndex += ErikuraMarkerView.ENTRIED_ZINDEX_OFFSET
                    }
                    else if (job.boost) {
                        marker.zIndex += ErikuraMarkerView.BOOST_ZINDEX_OFFSET
                    }
                    else if (job.wanted) {
                        marker.zIndex += ErikuraMarkerView.WANTED_ZINDEX_OFFSET
                    }
                    // tag として index を保存しておきます
                    marker.tag = i
                }
                viewModel.markerMap.put(job.id, erikuraMarker)
                if ( i == 0 ) {
                    viewModel.activeMaker = erikuraMarker
                }
            }
            // FIXME: 先頭ではなく最も近いマーカにする
            jobs.first().let {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), defaultZoom))
            }
            val carouselView: RecyclerView = findViewById(R.id.map_view_carousel)
            if (carouselView.adapter is ErikuraCarouselAdapter) {
                var adapter = carouselView.adapter as ErikuraCarouselAdapter
                adapter.data = jobs
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMapViewBinding = DataBindingUtil.setContentView(this, R.layout.activity_map_view)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        Realm.init(this)

        val adapter = ErikuraCarouselAdapter(this, listOf())
        adapter.onClickListner = object: ErikuraCarouselAdapter.OnClickListener {
            override fun onClick(job: Job) {
                onClickCarouselItem(job)
            }
        }

        carouselView = findViewById(R.id.map_view_carousel)
        carouselView.setHasFixedSize(true)
        carouselView.addItemDecoration(ErikuraCarouselCellDecoration())
        carouselView.adapter = adapter

        carouselView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val adapter = recyclerView.adapter as ErikuraCarouselAdapter

                val layoutManager: LinearLayoutManager = carouselView.layoutManager as LinearLayoutManager
                val position = layoutManager.findFirstCompletelyVisibleItemPosition()

                Log.v("INDEX:", "Position: ${position}, length: ${adapter.data.size}")
                if (position >= 0 && adapter.data.size > 0) {
                    val job = adapter.data[position]
                    Log.v("VISIBLE JOB: ", job.toString())

                    this@MapViewActivity.runOnUiThread{
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(job.latLng))

                        viewModel.activeMaker = viewModel.markerMap[job.id]
                    }
                }
            }
        })

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(carouselView)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.jobs_map_view_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

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
        locationManager.start(this)
        locationManager.addLocationUpdateCallback {
            if (!firstFetchRequested) {
                mMap?.animateCamera(CameraUpdateFactory.newLatLng(it))
                firstFetchRequested = true
                val query = viewModel.query(it)
                fetchJobs(query)
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationManager.latLng ?: defaultLatLng, defaultZoom))

        try {
            val styleOptions = MapStyleOptions.loadRawResourceStyle(this, R.raw.style)
            mMap.setMapStyle(styleOptions)
        }
        catch (e: Resources.NotFoundException) {
            Log.e("ERROR", e.message, e)
        }

        mMap.setOnMarkerClickListener { marker ->
            val index: Int = marker.tag as Int
            val jobs = viewModel.jobs.value ?: listOf()
            if (index >= 0) {
                val job: Job? = jobs[index]
                viewModel.activeMaker = viewModel.markerMap[job?.id]
            }
            else {
                viewModel.activeMaker = null
            }

            var layoutManager = carouselView.layoutManager as LinearLayoutManager
            layoutManager.smoothScrollToPosition(carouselView, RecyclerView.State(), index)

            true
        }
    }

    override fun onClickReSearch(view: View) {
        val position = mMap.cameraPosition
        fetchJobs(viewModel.query(position.target))
    }

    override fun onClickSearch(view: View) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onClickSearchBar(view: View) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        // 現在位置をもとに検索し直します
        val position = mMap.cameraPosition
        fetchJobs(viewModel.query(position.target))
    }

    override fun onClickList(view: View) {
        val intent = Intent(this, ListViewActivity::class.java)
        // FIXME: 検索条件の引き継ぎについて検討する
        startActivity(intent)
        // 地図画面は閉じておきます
        finish()
    }

    // 現在地に戻る
    override fun onClickCurrentLocation(view: View) {
        mMap.animateCamera(CameraUpdateFactory.newLatLng(locationManager.latLng))
    }

    // カルーセルクリック時の処理
    override fun onClickCarouselItem(job: Job) {
        Log.v("ErikuraCarouselCel", "Click: ${job.toString()}")

        val jobsOnLocation = viewModel.jobsByLocation.value?.get(job.latLng) ?: listOf()
        if (jobsOnLocation.size > 1) {
            // FIXME: 案件選択モーダルを表示する
            val dialog = JobSelectDialogFragment(jobsOnLocation)
            dialog.show(supportFragmentManager, "JobSelector")
        }
        else {
            val intent= Intent(this, JobDetailsActivity::class.java)
            startActivity(intent)
        }
    }
}

class MapViewViewModel: ViewModel() {
    val resources: Resources get() = ErikuraApplication.instance.applicationContext.resources

    val jobs: MutableLiveData<List<Job>> = MutableLiveData()
    val jobsByLocation: MutableLiveData<Map<LatLng, List<Job>>> = MutableLiveData()
    val markerMap: MutableMap<Int, ErikuraMarkerView> = HashMap()
    val periodType: MutableLiveData<PeriodType> = MutableLiveData()

    var activeMaker: ErikuraMarkerView? = null
        set(value) {
            field?.let {
                it.marker.zIndex -= ErikuraMarkerView.ACTIVE_ZINDEX_OFFSET
                it.active = false
            }
            field = value
            field?.let {
                it.marker.zIndex += ErikuraMarkerView.ACTIVE_ZINDEX_OFFSET
                it.active = true
            }
        }

//    val keyword: MutableLiveData<String> = MutableLiveData()
//    val minimumReward: MutableLiveData<Int> = MutableLiveData()
//    val maximumReward: MutableLiveData<Int> = MutableLiveData()
//    val minimumWorkingTime: MutableLiveData<Int> = MutableLiveData()
//    val maximumWorkingTime: MutableLiveData<Int> = MutableLiveData()
//    val jobKind: MutableLiveData<JobKind> = MutableLiveData()

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

interface MapViewEventHandlers {
    fun onClickReSearch(view: View)
    fun onClickSearch(view: View)
    fun onClickSearchBar(view: View)
    fun onToggleActiveOnly(view: View)
    fun onClickList(view: View)
    fun onClickCurrentLocation(view: View)

    fun onClickCarouselItem(job: Job)
}
