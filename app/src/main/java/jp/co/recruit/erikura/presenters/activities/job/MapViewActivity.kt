package jp.co.recruit.erikura.presenters.activities.job

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.graphics.contains
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.SphericalUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.business.models.PeriodType
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityMapViewBinding
import jp.co.recruit.erikura.presenters.activities.BaseTabbedActivity
import jp.co.recruit.erikura.presenters.activities.TabEventHandlers
import jp.co.recruit.erikura.presenters.fragments.ErikuraMarkerView
import jp.co.recruit.erikura.presenters.util.LocationManager
import jp.co.recruit.erikura.presenters.util.MessageUtils
import jp.co.recruit.erikura.presenters.view_models.BaseJobQueryViewModel
import kotlinx.android.synthetic.main.activity_map_view.*
import kotlin.math.abs

class MapViewActivity : BaseTabbedActivity(R.id.tab_menu_search_jobs, finishByBackButton = true), OnMapReadyCallback, MapViewEventHandlers {
    companion object {
        // SearchJob にわたすID
        const val REQUEST_SEARCH_CONDITIONS = 1
        // デフォルトズーム
        const val defaultZoom = 15.0f
    }

    private val viewModel: MapViewViewModel by lazy {
        ViewModelProvider(this).get(MapViewViewModel::class.java)
    }
    private val coachViewModel: MapViewCoachViewModel by lazy {
        ViewModelProvider(this).get(MapViewCoachViewModel::class.java)
    }

    private val locationManager: LocationManager = ErikuraApplication.locationManager

    /** GoogleMap のカメラ位置を移動中かを保持します (true は移動中) */
    private var cameraMoving: Boolean = false
    private var displaySearchBar: Boolean = true
    private var resetCameraPosition: Boolean = false

    private lateinit var mMap: GoogleMap
    private lateinit var carouselView: RecyclerView
    private lateinit var adapter: ErikuraCarouselAdapter
    private lateinit var tutorialAdapter: ErikuraCarouselAdapter
    private var firstFetchRequested: Boolean = false
    var isChangeUserInformationOnlyPhone: Boolean = false

    private var gestureDetector: GestureDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMapViewBinding = DataBindingUtil.setContentView(this, R.layout.activity_map_view)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
        binding.coachViewModel = coachViewModel

        val appLinkData: Uri? = intent.data
        appLinkData?.let{
            if (appLinkData.path == "/app/link/jobs/map/") {
                // FDLで遷移した場合、空にセットしておく
                ErikuraApplication.instance.pushUri = null
            }
        }

        intent.getParcelableExtra<JobQuery>(SearchJobActivity.EXTRA_SEARCH_CONDITIONS)?.let { query ->
            // リストからの切替時に、ソート条件も引き継ぐ (ERIKURA-1051)
            // query.sortBy = SortType.DISTANCE_ASC
            viewModel.apply(query)
        }

        isChangeUserInformationOnlyPhone = intent.getBooleanExtra("onClickChangeUserInformationOnlyPhone", false)

        val dummyJob = Job(
            latitude = LocationManager.defaultLatLng.latitude,
            longitude = LocationManager.defaultLatLng.longitude
        )

        carouselView = findViewById(R.id.map_view_carousel)

        adapter = ErikuraCarouselAdapter(this, carouselView, listOf(), viewModel.jobsByLocation.value ?: mapOf())
        adapter.onClickListener = object: ErikuraCarouselAdapter.OnClickListener {
            override fun onClick(job: Job) {
                onClickCarouselItem(job)
            }
        }

        val carouselLayoutManager = LinearLayoutManager(this)
        carouselLayoutManager.orientation = RecyclerView.HORIZONTAL

        carouselView.setHasFixedSize(false)
        carouselView.layoutManager = carouselLayoutManager
        carouselView.addItemDecoration(ErikuraCarouselCellDecoration())
        carouselView.adapter = adapter

        (carouselView.layoutManager as? LinearLayoutManager)?.let {
            it.stackFromEnd = true
        }

        carouselView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    animateCamera()

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val position = layoutManager.findFirstVisibleItemPosition()
                    if (position != (adapter.itemCount - 1)) {
                        // 末尾の要素
                        adapter.notifyItemRangeChanged(position, 2)
                    }
                    else {
                        adapter.notifyItemChanged(position)
                    }
                }
            }

            fun animateCamera() {
                val layoutManager: LinearLayoutManager = carouselView.layoutManager as LinearLayoutManager
                val position = layoutManager.findFirstCompletelyVisibleItemPosition()

                Log.v("ERIKURA", "Position: ${position}, length: ${adapter.data.size}, FV=${layoutManager.findFirstVisibleItemPosition()}, LV=${layoutManager.findLastVisibleItemPosition()}, LC=${layoutManager.findLastCompletelyVisibleItemPosition()}")
                if (position >= 0 && adapter.data.size > 0) {
                    val job = adapter.data[position]
                    Log.v("VISIBLE JOB: ", job.toString())

                    this@MapViewActivity.runOnUiThread {
                        if (::mMap.isInitialized) {
                            val updateRequest = CameraUpdateFactory.newLatLng(job.latLng)
                            Log.v(ErikuraApplication.LOG_TAG, "GMS: animateCamera(carousel): $updateRequest")
                            mMap.animateCamera(updateRequest)
                        }
                        viewModel.activeMaker = viewModel.markerMap[job.id]
                    }
                }
            }
        })

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(carouselView)

        val coachMarkDisplayed: Boolean = ErikuraApplication.instance.isCoachMarkDisplayed()
        coachViewModel.coach.value = !coachMarkDisplayed
        coachViewModel.onCoachFinished {
            ErikuraApplication.instance.setCoachMarkDisplayed(true)
        }

        tutorialAdapter = ErikuraCarouselAdapter(this, map_view_carousel_highlight, listOf(dummyJob), viewModel.jobsByLocation.value ?: mapOf())
        tutorialAdapter.onClickListener = object: ErikuraCarouselAdapter.OnClickListener {
            override fun onClick(job: Job) {
                coachViewModel.next()
            }
        }
        map_view_carousel_highlight.adapter = tutorialAdapter
        map_view_carousel_highlight.addItemDecoration(ErikuraCarouselCellDecoration())
        LinearSnapHelper().attachToRecyclerView(map_view_carousel_highlight)
        map_view_carousel_highlight.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val position = layoutManager.findFirstVisibleItemPosition()
                    if (position != (tutorialAdapter.itemCount - 1)) {
                        // 末尾の要素
                        tutorialAdapter.notifyItemRangeChanged(position, 2)
                    }
                    else {
                        tutorialAdapter.notifyItemChanged(position)
                    }
                }
            }
        })

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.jobs_map_view_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        gestureDetector = GestureDetector(this, GoogleMapGestureListener { e -> onMapSingleTap(e) })
        map_touchable_wrapper?.onTouch = { e ->
            gestureDetector?.onTouchEvent(e)
        }

        // 各種ボタンの表示・非表示の切り替えを行います
        viewModel.reSearchButtonVisible.value = View.GONE           // 初期表示ではこの地点で再検索ボタンを非表示とする
        viewModel.searchBarVisible.value = View.VISIBLE             // 初期表示では検索条件バーを表示する

        if (!locationManager.checkPermission(this)) {
            locationManager.requestPermission(this)
        }
    }

    override fun onStart() {
        super.onStart()
        if (isChangeUserInformationOnlyPhone) {
            isChangeUserInformationOnlyPhone = false
            val dialog = ChangeUserInformationOnlyPhoneFragment()
            dialog.show(supportFragmentManager, "ChangeUserInformationOnlyPhone")
        }
    }

    override fun onPause() {
        super.onPause()
        locationManager.stop()
        locationManager.clearLocationUpdateCallback()

        searchJobQuery = viewModel.query(viewModel.latLng.value ?: LocationManager.defaultLatLng)
        if (::mMap.isInitialized) {
            mapCameraPosition = mMap.cameraPosition
        }
    }

    override fun onResume() {
        super.onResume()
        // 仕事を探すタブの画面を保存しておきます
        searchJobCurrentActivity = this.javaClass

        if (locationManager.checkPermission(this) && ::mMap.isInitialized) {
            mMap.isMyLocationEnabled = true
            hideGoogleMapMyLocationButton()
        }

        locationManager.start(this)
        locationManager.addLocationUpdateCallback {
            if (!firstFetchRequested && ::mMap.isInitialized && viewModel.keyword.value.isNullOrBlank()) {
                val updateRequest = CameraUpdateFactory.newLatLngZoom(it, defaultZoom)
                Log.v(ErikuraApplication.LOG_TAG, "GMS: moveCamera(resume): $updateRequest")
                mMap.moveCamera(updateRequest)
                firstFetchRequested = true
                val query = viewModel.query(viewModel.latLng.value ?: it)
                fetchJobs(query)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.isIndoorEnabled = false

        // 地図上のアイコンをグレーにするためにスタイル設定を行います
        try {
            val styleOptions = MapStyleOptions.loadRawResourceStyle(this, R.raw.style)
            mMap.setMapStyle(styleOptions)
        }
        catch (e: Resources.NotFoundException) {
            Log.e("ERROR", e.message, e)
        }

        val mapPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 164.0f, resources.displayMetrics).toInt()
        mMap.setPadding(0, mapPadding, 0, mapPadding)

        // カメラ移動に関するコールバックの登録
        mMap.setOnCameraMoveStartedListener { onCameraMoveStarted(it) }
        mMap.setOnCameraMoveCanceledListener { onCameraMoveCanceled() }
        mMap.setOnCameraIdleListener { onCameraIdle() }

        mMap.setOnMarkerClickListener {
            true
        }

        // ズームの初期設定を行っておきます
        mapCameraPosition?.also {
            // 保存済みの位置に移動します
            val updateRequest = CameraUpdateFactory.newCameraPosition(it)
            Log.v(ErikuraApplication.LOG_TAG, "GMS: moveCamera(ready): $updateRequest")
            mMap.moveCamera(updateRequest)
        } ?: run {
            // 初期位置に移動させます
            val updateRequest = CameraUpdateFactory.newLatLngZoom(locationManager.latLngOrDefault, defaultZoom)
            Log.v(ErikuraApplication.LOG_TAG, "GMS: moveCamera(ready): $updateRequest")
            mMap.moveCamera(updateRequest)
        }

        if (locationManager.checkPermission(this)) {
            mMap.isMyLocationEnabled = true
            hideGoogleMapMyLocationButton()
        }

        // 最初のタスク取得
        if (!firstFetchRequested) {
            if (viewModel.keyword.value.isNullOrBlank()) {
                // 検索キーワードが指定されていないので、現在値より検索します
                (viewModel.latLng.value ?: locationManager.latLng)?.also {
                    // 位置情報が取得可能なので、位置情報を返却します
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
            } else {
                viewModel.latLng.value?.let {
                    firstFetchRequested = true
                    val query = viewModel.query(it)
                    fetchJobs(query)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.let{
            isChangeUserInformationOnlyPhone = data.getBooleanExtra("onClickChangeUserInformationOnlyPhone", false)
        }
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

    private fun fetchJobs(query: JobQuery) {
        Api(this@MapViewActivity).searchJobs(query, runCompleteOnUIThread = false) { jobs ->
            Log.v(ErikuraApplication.LOG_TAG, "Fetched Jobs: ${jobs.size}, ${jobs.toString()}")
            if (jobs.isNotEmpty()) {
                val summarizedJobs = JobUtils.summarizeJobsByLocation(jobs)
                val nearestJob = jobs.sortedBy { SphericalUtil.computeDistanceBetween(it.latLng, query.latLng) }.first()

                AndroidSchedulers.mainThread().scheduleDirect {
                    // viewModel の案件情報を更新します
                    viewModel.jobs.value = jobs
                    viewModel.jobsByLocation.value = summarizedJobs

                    // カルーセルの更新を行います
                    val carouselView: RecyclerView = findViewById(R.id.map_view_carousel)
                    adapter.data = jobs
                    adapter.jobsByLocation = viewModel.jobsByLocation.value ?: mapOf()
                    adapter.notifyDataSetChanged()
                    tutorialAdapter.data = jobs
                    tutorialAdapter.jobsByLocation = viewModel.jobsByLocation.value ?: mapOf()
                    tutorialAdapter.notifyDataSetChanged()

                    // マーカを設定します
                    rebuildMarkers(jobs)

                    // 最も距離が近い案件を取得します
                    var nearestMarker: ErikuraMarkerView = viewModel.markerMap[nearestJob.id]!!
                    var nearestIndex: Int = nearestMarker.marker.tag as Int
                    // マーカーをアクティブに変更します
                    viewModel.activeMaker = nearestMarker
                    // 最も近い案件に地図を移動します
                    resetCameraPosition = true
                    val updateRequest: CameraUpdate = CameraUpdateFactory.newLatLng(nearestJob.latLng)
                    Log.v(ErikuraApplication.LOG_TAG, "GMS: animateCamera(fetchJob): $updateRequest")
                    mMap.animateCamera(updateRequest)

                    // カルーセルを最も近い案件に変更します
                    var layoutManager = carouselView.layoutManager as LinearLayoutManager
                    layoutManager.scrollToPosition(nearestIndex)

                    // ページ参照のトラッキングの送出
                    val jobId = jobs.map { it.id }

                    Tracking.logEvent(event= "view_job_list_map", params= bundleOf())
                    Tracking.viewJobs(name= "/jobs/map", title= "仕事一覧画面（地図）", jobId= jobId)
                    // 仕事表示のトラッキングの送出
                    Tracking.logEvent(event= "dispaly_job_list", params= bundleOf())
                    Tracking.viewJobs(name= "dispaly_job_list", title= "仕事一覧表示（地図）", jobId= jobId)
                }
            }
            else {
                AndroidSchedulers.mainThread().scheduleDirect {
                    val newQuery = JobQuery(
                        latitude = locationManager.latLngOrDefault.latitude,
                        longitude = locationManager.latLngOrDefault.longitude)
                    if (query != newQuery) {
                        MessageUtils.displayAlert(this, listOf("検索した地域で", "お仕事が見つからなかったため、", "一番近くのお仕事を表示します")) {
                            // クリアした検索条件での再検索を行います
                            viewModel.apply(newQuery)
                            fetchJobs(newQuery)
                        }
                    }
                    else {
                        MessageUtils.displayAlert(this, listOf("検索した地域で", "お仕事が見つかりませんでした。"))
                    }
                }
            }
        }
    }

    private fun rebuildMarkers(jobs: List<Job>) {
        // マーカーの情報をクリアします
        viewModel.markerMap.clear()
        viewModel.activeMaker = null
        // マーカーを地図から削除します
        mMap.clear()

        // マーカーを作成します
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

                if (job.isEntried) {
                    if (!job.isReported && job.isOwner) {
                        marker.zIndex += ErikuraMarkerView.OWN_JOB_ZINDEX_OFFSET
                    }
                    else {
                        marker.zIndex += ErikuraMarkerView.ENTRIED_ZINDEX_OFFSET
                    }
                }
                else if (job.isPastOrInactive) {
                    marker.zIndex += ErikuraMarkerView.ENTRIED_ZINDEX_OFFSET
                }

                if (job.boost) {
                    marker.zIndex += ErikuraMarkerView.BOOST_ZINDEX_OFFSET
                }
                if (job.wanted) {
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
    }

    private fun onCameraMoveStarted(_reason: Int) {
        cameraMoving = true
        // 案件取得によるカメラリセット以外の場合
        if (!resetCameraPosition) {
            // 再検索ボタンを表示
            viewModel.reSearchButtonVisible.value = View.VISIBLE
            // 検索バーを非表示
            viewModel.searchBarVisible.value = View.GONE
        }
    }

    private fun onCameraMoveCanceled() {
        cameraMoving = false
    }

    private fun onCameraIdle() {
        if (cameraMoving) {
            cameraMoving = false
            onCameraMoveFinished()
        }
    }

    private fun onCameraMoveFinished() {
        cameraMoving = false

        if (resetCameraPosition) {
            // 検索バーの表示/非表示を設定します
            if (displaySearchBar) {
                viewModel.searchBarVisible.value = View.VISIBLE
            }

            // 再検索ボタンは非表示とします
            viewModel.reSearchButtonVisible.value = View.GONE

            displaySearchBar = false
            resetCameraPosition = false
        }
    }


    override fun onClickReSearch(view: View) {
        val position = mMap.cameraPosition
        // 募集中/すべてのフラグ以外は検索条件をクリアします
        viewModel.clearWithoutPeriod()
        fetchJobs(viewModel.query(position.target))
    }

    override fun onClickSearch(view: View) {
        viewModel.searchBarVisible.value = View.VISIBLE
    }

    override fun onClickSearchBar(view: View) {
        val intent = Intent(this, SearchJobActivity::class.java)
        intent.putExtra(SearchJobActivity.EXTRA_SEARCH_CONDITIONS, viewModel.query(locationManager.latLngOrDefault))
        startActivityForResult(intent, REQUEST_SEARCH_CONDITIONS)
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

        viewModel.searchBarVisible.value = View.GONE

        // 検索キーワードがない場合は現在位置をもとに検索し直します
        val position = mMap.cameraPosition
        val latLng = viewModel.latLng.value ?: position.target
        val query = viewModel.query(latLng)
        fetchJobs(query)
    }

    override fun onClickList(view: View) {
        // 表示変更のトラッキングの送出
        Tracking.logEvent(event= "push_toggle_dispaly", params= bundleOf())
        Tracking.track(name= "push_toggle_dispaly")

        Intent(this, ListViewActivity::class.java).let {
            it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            it.putExtra(SearchJobActivity.EXTRA_SEARCH_CONDITIONS, viewModel.query(viewModel.latLng.value ?: LocationManager.defaultLatLng))
            startActivity(it)
        }
    }

    // 現在地に戻る
    override fun onClickCurrentLocation(view: View) {
        // 現在地押下のトラッキングの送出
        Tracking.logEvent(event= "push_reload_location", params= bundleOf())
        Tracking.track(name= "push_reload_location")

        val updateRequest = CameraUpdateFactory.newLatLngZoom(locationManager.latLngOrDefault, defaultZoom)
        Log.v(ErikuraApplication.LOG_TAG, "GMS: animateCamera(currentLocation): $updateRequest")
        mMap.animateCamera(updateRequest)
    }

    // カルーセルクリック時の処理
    override fun onClickCarouselItem(job: Job) {
        Log.v("ErikuraCarouselCel", "Click: ${job.toString()}")

        val jobsOnLocation = viewModel.jobsByLocation.value?.get(job.latLng) ?: listOf()
        if (jobsOnLocation.size > 1) {
            // 案件選択モーダルを表示する
            val dialog = JobSelectDialogFragment.newInstance(JobUtils.sortJobs(jobsOnLocation), viewModel.latLng.value)
            dialog.show(supportFragmentManager, "JobSelector")
        }
        else {
            val intent= Intent(this, JobDetailsActivity::class.java)
            intent.putExtra("job", job)
            startActivity(intent)
        }
    }

    private fun hideGoogleMapMyLocationButton() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.jobs_map_view_map)
        mapFragment?.view?.let { mapView ->
            val locationButton: View = mapView.findViewWithTag("GoogleMapMyLocationButton")
            locationButton.visibility = View.GONE
        }
    }

    private fun onMapSingleTap(e: MotionEvent?) {
        val tapPoint = Point(e?.x?.toInt() ?: 0, e?.y?.toInt() ?: 0)
        val displayMetrics = resources.displayMetrics
        val hitMarkers = mutableListOf<ErikuraMarkerView>()
        val visibleBounds = mMap.projection.visibleRegion.latLngBounds

        viewModel.markerMap.forEach { _jobId, erikuraMarker ->
//            if (visibleBounds.contains(erikuraMarker.marker.position)) {
                val point = mMap.projection.toScreenLocation(erikuraMarker.marker.position)
                val topLeft = Point(
                    (point.x - (100 / 2 * displayMetrics.density)).toInt(),
                    (point.y - 47 * displayMetrics.density).toInt())
                val bottomRight = Point(
                    (point.x + (100 / 2 * displayMetrics.density)).toInt(),
                    (point.y).toInt())

                val rect = Rect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
                if (rect.contains(tapPoint)) {
                    hitMarkers.add(erikuraMarker)
                    Log.v(ErikuraApplication.LOG_TAG, "tapPoint: (${tapPoint.x}, ${tapPoint.y}), point: (${point.x}, ${point.y}) => (${topLeft.x}, ${topLeft.y})-(${bottomRight.x}-${bottomRight.y})")
                }
//            }
        }

        hitMarkers.sortedByDescending { it.marker.zIndex }?.let { sortedHitMarkers ->
            if (sortedHitMarkers.isNotEmpty()) {
                sortedHitMarkers.first()?.let { erikuraMarker ->
                    val index: Int = erikuraMarker.marker.tag as Int
                    Log.v("ERIKURA", "Marker index: ${index}")
                    val jobs = viewModel.jobs.value ?: listOf()
                    if (index >= 0) {
                        val job: Job? = jobs[index]
                        viewModel.activeMaker = viewModel.markerMap[job?.id]
                    }
                    else {
                        viewModel.activeMaker = null
                    }

                    var layoutManager = carouselView.layoutManager as LinearLayoutManager

                    val current = layoutManager.findFirstCompletelyVisibleItemPosition()
                    if (current != index) {
                        if (current < index) {
                            if (abs(index - current) > 10) {
                                layoutManager.scrollToPosition(index - 10)
                            }
                            layoutManager.smoothScrollToPosition(carouselView, RecyclerView.State(), index)
                        }
                        else {
                            if (abs(index - current) > 10) {
                                layoutManager.scrollToPosition(index + 10)
                            }
                            layoutManager.smoothScrollToPosition(carouselView, RecyclerView.State(), index)
                        }
                    }
                }
            }
        }
    }
}

class MapViewViewModel: BaseJobQueryViewModel() {
    val resources: Resources get() = ErikuraApplication.instance.applicationContext.resources

    val jobs: MutableLiveData<List<Job>> = MutableLiveData()
    val jobsByLocation: MutableLiveData<Map<LatLng, List<Job>>> = MutableLiveData()
    val markerMap: MutableMap<Int, ErikuraMarkerView> = HashMap()

    // 初期状態では検索バーは表示し、なにか操作が行われたら非表示とする
    val searchBarVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    // 初期状態では非表示、位置を変更すると表示される
    var reSearchButtonVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    // 現在地ボタンの表示状態：常に表示される
    var currentLocationButtonVisible: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

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

    val activeOnlyButtonBackground = MediatorLiveData<Drawable>().also { result ->
        result.addSource(periodType) {
            result.value = when (it) {
                PeriodType.ALL -> resources.getDrawable(R.drawable.before_open_500w, null)
                PeriodType.ACTIVE -> resources.getDrawable(R.drawable.before_entry_500w_on, null)
                else -> resources.getDrawable(R.drawable.before_open_500w, null)
            }
        }
    }

    val carouselVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(jobs) {
            result.value = if (it.isEmpty()) { View.GONE } else { View.VISIBLE }
        }
    }

    init {
        periodType.value = PeriodType.ALL
    }
}

interface MapViewEventHandlers: TabEventHandlers {
    fun onClickReSearch(view: View)
    fun onClickSearch(view: View)
    fun onClickSearchBar(view: View)
    fun onToggleActiveOnly(view: View)
    fun onClickList(view: View)
    fun onClickCurrentLocation(view: View)

    fun onClickCarouselItem(job: Job)
}

class MapViewCoachViewModel: ViewModel() {
    val coach = MutableLiveData<Boolean>()
    val step = MutableLiveData<Int>()

    val step0Visibility = MediatorLiveData<Int>().also { result ->
        result.addSource(step) { result.value = if (it == 0) { View.VISIBLE } else { View.GONE } }
    }
    val step1Visibility = MediatorLiveData<Int>().also { result ->
        result.addSource(step) { result.value = if (it == 1) { View.VISIBLE } else { View.GONE } }
    }
    val coachVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(coach) { result.value = decideCoachVisibility() }
        result.addSource(step) { result.value = decideCoachVisibility() }
    }

    var onCoachFinishedHandler: (() -> Unit)? = null

    init {
        coach.value = false
        step.value = 0
    }

    fun next() {
        step.value = (step.value ?: 0) + 1

        if ((step.value ?: 0) > 1) {
            onCoachFinishedHandler?.invoke()
        }
    }

    fun onCoachFinished(handler: () -> Unit) {
        onCoachFinishedHandler = handler
    }

    fun decideCoachVisibility(): Int {
        // コーチマークが無効になっていれば、表示しない
        if (!(coach.value ?: false)) return View.GONE
        // 各ステップの表示が有効な場合は、表示する
        if (step.value == 0) return View.VISIBLE
        if (step.value == 1) return View.VISIBLE
        // 全ステップが非表示なので、コーチマークは非表示に
        return View.GONE
    }

    fun tap(view: View) {
        this.next()
    }
}

class TouchableWrapper: FrameLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var onTouch: ((event: MotionEvent?) -> Unit)? = null

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        onTouch?.invoke(ev)
        return super.dispatchTouchEvent(ev)
    }
}

class GoogleMapGestureListener(private val onSingleTap: (MotionEvent?) -> Unit): GestureDetector.SimpleOnGestureListener() {
    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        super.onSingleTapConfirmed(e)
        e?.let { onSingleTap(it) }
        return true
    }
}