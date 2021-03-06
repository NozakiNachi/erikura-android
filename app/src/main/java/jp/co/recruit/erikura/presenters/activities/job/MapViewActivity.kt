package jp.co.recruit.erikura.presenters.activities.job

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.graphics.contains
import androidx.core.os.bundleOf
import androidx.core.view.marginRight
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
        // SearchJob ????????????ID
        const val REQUEST_SEARCH_CONDITIONS = 1
        // ????????????????????????
        const val defaultZoom = 15.0f
    }

    private val viewModel: MapViewViewModel by lazy {
        ViewModelProvider(this).get(MapViewViewModel::class.java)
    }
    private val coachViewModel: MapViewCoachViewModel by lazy {
        ViewModelProvider(this).get(MapViewCoachViewModel::class.java)
    }

    private val locationManager: LocationManager = ErikuraApplication.locationManager

    /** GoogleMap ??????????????????????????????????????????????????? (true ????????????) */
    private var cameraMoving: Boolean = false
    private var displaySearchBar: Boolean = true
    private var resetCameraPosition: Boolean = false

    private lateinit var mMap: GoogleMap
    private var carouselView: RecyclerView? = null
    private var adapter: ErikuraCarouselAdapter? = null
    private var tutorialAdapter: ErikuraCarouselAdapter? = null
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

        intent.getParcelableExtra<JobQuery>(SearchJobActivity.EXTRA_SEARCH_CONDITIONS)?.let { query ->
            // ??????????????????????????????????????????????????????????????? (ERIKURA-1051)
            // query.sortBy = SortType.DISTANCE_ASC
            viewModel.apply(query)
        }

        isChangeUserInformationOnlyPhone = intent.getBooleanExtra("onClickChangeUserInformationOnlyPhone", false)

        carouselView = findViewById(R.id.map_view_carousel)

        val carouselLayoutManager = LinearLayoutManager(this)
        carouselLayoutManager.orientation = RecyclerView.HORIZONTAL

        carouselView?.setHasFixedSize(false)
        carouselView?.layoutManager = carouselLayoutManager
        carouselView?.addItemDecoration(ErikuraCarouselCellDecoration())

        (carouselView?.layoutManager as? LinearLayoutManager)?.let {
            it.stackFromEnd = true
        }

        carouselView?.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    animateCamera()

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val position = layoutManager.findFirstVisibleItemPosition()
                    if (position != ((adapter?.itemCount ?: 0) - 1)) {
                        // ???????????????
                        adapter?.notifyItemRangeChanged(position, 2)
                    }
                    else {
                        adapter?.notifyItemChanged(position)
                    }
                }
            }

            fun animateCamera() {
                val carouselView: RecyclerView = findViewById(R.id.map_view_carousel)
                val layoutManager: LinearLayoutManager = carouselView.layoutManager as LinearLayoutManager
                val position = layoutManager.findFirstCompletelyVisibleItemPosition()

                Log.v("ERIKURA", "Position: ${position}, length: ${adapter?.data?.size ?: 0}, FV=${layoutManager.findFirstVisibleItemPosition()}, LV=${layoutManager.findLastVisibleItemPosition()}, LC=${layoutManager.findLastCompletelyVisibleItemPosition()}")
                if (position >= 0 && (adapter?.data?.size ?: 0) > 0) {
                    adapter?.data?.let { data ->
                        data[position].let { job ->
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

        map_view_carousel_highlight.addItemDecoration(ErikuraCarouselCellDecoration())
        map_view_carousel_highlight.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val position = layoutManager.findFirstVisibleItemPosition()
                    if (position != ((tutorialAdapter?.itemCount ?: 0) - 1)) {
                        // ???????????????
                        tutorialAdapter?.notifyItemRangeChanged(position, 2)
                    } else {
                        tutorialAdapter?.notifyItemChanged(position)
                    }
                }
            }
        })
        LinearSnapHelper().attachToRecyclerView(map_view_carousel_highlight)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.jobs_map_view_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        gestureDetector = GestureDetector(this, GoogleMapGestureListener { e -> onMapSingleTap(e) })
        map_touchable_wrapper?.onTouch = { e ->
            gestureDetector?.onTouchEvent(e)
        }

        // ??????????????????????????????????????????????????????????????????
        viewModel.reSearchButtonVisible.value = View.GONE           // ????????????????????????????????????????????????????????????????????????
        viewModel.searchBarVisible.value = View.VISIBLE             // ???????????????????????????????????????????????????

        if (!locationManager.checkPermission(this)) {
            locationManager.requestPermission(this)
        }

        // FDL?????????map?????????????????????
        ErikuraApplication.instance.removePushUriFromFDL(intent, "/app/link/jobs/map")
    }

    override fun onStart() {
        super.onStart()

        carouselView = findViewById(R.id.map_view_carousel)

        carouselView?.let { carouselView ->
            adapter = ErikuraCarouselAdapter(this, carouselView, viewModel.jobs.value ?: listOf(), viewModel.jobsByLocation.value ?: mapOf())
            adapter?.onClickListener = object: ErikuraCarouselAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    onClickCarouselItem(job)
                }
            }
            carouselView?.adapter = adapter
        }

        val dummyJob = Job(
            latitude = LocationManager.defaultLatLng.latitude,
            longitude = LocationManager.defaultLatLng.longitude
        )

        tutorialAdapter = ErikuraCarouselAdapter(
            this,
            map_view_carousel_highlight,
            viewModel.jobs.value ?: listOf(dummyJob),
            viewModel.jobsByLocation.value ?: mapOf()
        )
        tutorialAdapter?.onClickListener = object : ErikuraCarouselAdapter.OnClickListener {
            override fun onClick(job: Job) {
                coachViewModel.next()
            }
        }
        map_view_carousel_highlight.adapter = tutorialAdapter

        if (isChangeUserInformationOnlyPhone) {
            isChangeUserInformationOnlyPhone = false
            val dialog = ChangeUserInformationOnlyPhoneFragment()
            dialog.show(supportFragmentManager, "ChangeUserInformationOnlyPhone")
        }

        // ??????????????????????????? ActiveMarker ???????????????????????????
        viewModel.activeMaker?.let { marker ->
            val index: Int = marker.marker.tag as Int
            Log.v(ErikuraApplication.LOG_TAG, "Marker index: ${index}")
            val layoutManager = carouselView?.layoutManager as LinearLayoutManager
            layoutManager.scrollToPosition(index)
        }
    }

    override fun onStop() {
        super.onStop()

        carouselView?.adapter = null
        carouselView = null
        adapter = null

        map_view_carousel_highlight.adapter = null
        tutorialAdapter = null
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
        // ?????????????????????????????????????????????????????????
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
                resetCameraPosition = true
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

        // ???????????????????????????????????????????????????????????????????????????????????????
        try {
            val styleOptions = MapStyleOptions.loadRawResourceStyle(this, R.raw.style)
            mMap.setMapStyle(styleOptions)
        }
        catch (e: Resources.NotFoundException) {
            Log.e("ERROR", e.message, e)
        }

        val mapPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 164.0f, resources.displayMetrics).toInt()
        mMap.setPadding(0, mapPadding, 0, mapPadding)

        // ??????????????????????????????????????????????????????
        mMap.setOnCameraMoveStartedListener { onCameraMoveStarted(it) }
        mMap.setOnCameraMoveCanceledListener { onCameraMoveCanceled() }
        mMap.setOnCameraIdleListener { onCameraIdle() }

        mMap.setOnMarkerClickListener {
            true
        }

        // ????????????????????????????????????????????????
        mapCameraPosition?.also {
            // ???????????????????????????????????????
            val updateRequest = CameraUpdateFactory.newCameraPosition(it)
            Log.v(ErikuraApplication.LOG_TAG, "GMS: moveCamera(ready): $updateRequest")
            resetCameraPosition = true
            mMap.moveCamera(updateRequest)
        } ?: run {
            // ?????????????????????????????????
            val updateRequest = CameraUpdateFactory.newLatLngZoom(locationManager.latLngOrDefault, defaultZoom)
            Log.v(ErikuraApplication.LOG_TAG, "GMS: moveCamera(ready): $updateRequest")
            resetCameraPosition = true
            mMap.moveCamera(updateRequest)
        }

        if (locationManager.checkPermission(this)) {
            mMap.isMyLocationEnabled = true
            hideGoogleMapMyLocationButton()
        }

        // ????????????????????????
        if (!firstFetchRequested) {
            if (viewModel.keyword.value.isNullOrBlank()) {
                // ???????????????????????????????????????????????????????????????????????????????????????
                (viewModel.latLng.value ?: locationManager.latLng)?.also {
                    // ?????????????????????????????????????????????????????????????????????
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
                    // ???????????????????????????
                    data?.getParcelableExtra<JobQuery>(SearchJobActivity.EXTRA_SEARCH_CONDITIONS)?.let { query ->
                        // ??????????????? viewModel ??????????????????
                        viewModel.apply(query)
                        // ???????????????????????????????????????
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
            synchronized(this@MapViewActivity) {
                if (jobs.isNotEmpty()) {
                    val summarizedJobs = JobUtils.summarizeJobsByLocation(jobs)
                    val nearestJob = jobs.sortedBy { SphericalUtil.computeDistanceBetween(it.latLng, query.latLng) }.first()

                    AndroidSchedulers.mainThread().scheduleDirect {
                        // viewModel ?????????????????????????????????
                        viewModel.jobs.value = jobs
                        viewModel.jobsByLocation.value = summarizedJobs

                        // ???????????????????????????????????????
                        val carouselView: RecyclerView = findViewById(R.id.map_view_carousel)
                        adapter?.data = jobs
                        adapter?.jobsByLocation = viewModel.jobsByLocation.value ?: mapOf()
                        adapter?.notifyDataSetChanged()
                        tutorialAdapter?.data = jobs
                        tutorialAdapter?.jobsByLocation = viewModel.jobsByLocation.value ?: mapOf()
                        tutorialAdapter?.notifyDataSetChanged()

                        try {
                            // ???????????????????????????
                            rebuildMarkers(jobs)
                        } catch(e: IllegalArgumentException) {
                            // ????????????????????????????????????????????????????????????????????????
                            // ????????????????????????????????????????????????????????????????????????????????????????????????
                            // ????????????????????????????????????
                            Log.e("ERROR", e.message, e)
                        }

                        // ?????????????????????????????????????????????
                        val nearestMarker: ErikuraMarkerView = viewModel.markerMap[nearestJob.id]!!
                        val nearestIndex: Int = nearestMarker.marker.tag as Int
                        // ????????????????????????????????????????????????
                        viewModel.activeMaker = nearestMarker
                        // ?????????????????????????????????????????????
                        resetCameraPosition = true
                        val updateRequest: CameraUpdate = CameraUpdateFactory.newLatLng(nearestJob.latLng)
                        Log.v(ErikuraApplication.LOG_TAG, "GMS: animateCamera(fetchJob): $updateRequest")
                        mMap.animateCamera(updateRequest)

                        // ??????????????????????????????????????????????????????
                        val layoutManager = carouselView.layoutManager as LinearLayoutManager
                        layoutManager.scrollToPosition(nearestIndex)

                        // ?????????????????????????????????????????????
                        val jobId = jobs.map { it.id }

                        Tracking.logEvent(event= "view_job_list_map", params= bundleOf())
                        Tracking.viewJobs(name= "/jobs/map", title= "??????????????????????????????", jobId= jobId)
                        // ??????????????????????????????????????????
                        Tracking.logEvent(event= "dispaly_job_list", params= bundleOf())
                        Tracking.viewJobs(name= "dispaly_job_list", title= "??????????????????????????????", jobId= jobId)
                    }
                }
                else {
                    AndroidSchedulers.mainThread().scheduleDirect {
                        val newQuery = JobQuery(
                            latitude = locationManager.latLngOrDefault.latitude,
                            longitude = locationManager.latLngOrDefault.longitude)
                        if (query != newQuery) {
                            MessageUtils.displayAlert(this, listOf("?????????????????????", "?????????????????????????????????????????????", "??????????????????????????????????????????")) {
                                // ?????????????????????????????????????????????????????????
                                viewModel.apply(newQuery)
                                fetchJobs(newQuery)
                            }
                        }
                        else {
                            MessageUtils.displayAlert(this, listOf("?????????????????????", "?????????????????????????????????????????????"))
                        }
                    }
                }
            }
        }
    }

    private fun rebuildMarkers(jobs: List<Job>) {
        // ??????????????????????????????????????????
        viewModel.markerMap.clear()
        viewModel.activeMaker = null
        // ??????????????????????????????????????????
        mMap.clear()

        // ??????????????????????????????
        jobs.forEachIndexed { i, job ->
            val erikuraMarker = ErikuraMarkerView.build(this, mMap, job) { marker ->
                // ?????????
                marker.zIndex = ErikuraMarkerView.BASE_ZINDEX - i
                if (job.isStartSoon) {
                    if (job.isPreEntry) {
                        // ????????????????????????
                        marker.zIndex += ErikuraMarkerView.PRE_ENTRING_OFFSET
                    } else {
                        // ????????????
                        marker.zIndex += ErikuraMarkerView.SOON_ZINDEX_OFFSET
                    }
                }
                else if (job.isFuture) {
                    // ????????????
                    marker.zIndex += ErikuraMarkerView.FUTURE_ZINDEX_OFFSET
                }

                if (job.isEntried) {
                    if (!job.isReported && job.isOwner) {
                        if (job.preEntryStartAt != null) {
                            // ?????????????????????????????????
                            marker.zIndex += ErikuraMarkerView.PRE_ENTRIED_OFFSET
                        } else {
                            //????????????????????????????????????????????????
                            marker.zIndex += ErikuraMarkerView.OWN_JOB_ZINDEX_OFFSET
                        }
                    }
                    else {
                        //????????????????????????????????????????????????
                        marker.zIndex += ErikuraMarkerView.ENTRIED_ZINDEX_OFFSET
                    }
                }
                else if (job.isPastOrInactive) {
                    // ????????????(??????)
                    marker.zIndex += ErikuraMarkerView.ENTRIED_ZINDEX_OFFSET
                }

                if (job.boost) {
                    marker.zIndex += ErikuraMarkerView.BOOST_ZINDEX_OFFSET
                }
                if (job.wanted) {
                    marker.zIndex += ErikuraMarkerView.WANTED_ZINDEX_OFFSET
                }

                // tag ????????? index ???????????????????????????
                marker.tag = i
            }
            viewModel.markerMap.put(job.id, erikuraMarker)
            if ( i == 0 ) {
                viewModel.activeMaker = erikuraMarker
            }
        }
    }

    private fun onCameraMoveStarted(@Suppress("UNUSED_PARAMETER") _reason: Int) {
        cameraMoving = true
        // ?????????????????????????????????????????????????????????
        if (!resetCameraPosition) {
            if (viewModel.reSearchButtonVisible.value != View.VISIBLE) {
                // ???????????????????????????

                map_view_re_search_button.run {
                    val animation = AnimationUtils.loadAnimation(this@MapViewActivity, R.anim.research_button_dropin)
                    postDelayed({
                        startAnimation(animation)
                        viewModel.reSearchButtonVisible.value = View.VISIBLE
                    }, 0)
                }
            }
            // ????????????????????????
            if (viewModel.searchBarVisible.value != View.GONE) {
                val currentWidth = map_view_search_bar.measuredWidth
                val currentMarginRight = map_view_search_bar.marginRight
                val animator = ValueAnimator.ofInt(currentWidth, (66 * resources.displayMetrics.density).toInt())
                animator.addUpdateListener { valueAnimator ->
                    Log.v(ErikuraApplication.LOG_TAG, "ANIMATE: ${valueAnimator.animatedValue}")
                    val mlp = map_view_search_bar.layoutParams as ViewGroup.MarginLayoutParams
                    mlp.width = valueAnimator.animatedValue as Int
                    mlp.rightMargin = currentMarginRight + (currentWidth -  mlp.width)
                    map_view_search_bar.layoutParams = mlp
                }
                animator.addListener(object: Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        viewModel.searchBarVisible.value = View.GONE
                        map_view_search_bar.postDelayed({
                            val mlp = map_view_search_bar.layoutParams as ViewGroup.MarginLayoutParams
                            mlp.width = ViewGroup.LayoutParams.MATCH_PARENT
                            mlp.rightMargin = currentMarginRight
                            map_view_search_bar.layoutParams = mlp
                        }, 10)
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        viewModel.searchBarVisible.value = View.GONE
                        map_view_search_bar.postDelayed({
                            val mlp = map_view_search_bar.layoutParams as ViewGroup.MarginLayoutParams
                            mlp.width = ViewGroup.LayoutParams.MATCH_PARENT
                            mlp.rightMargin = currentMarginRight
                            map_view_search_bar.layoutParams = mlp
                        }, 10)
                    }

                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                        viewModel.searchBarVisible.value = View.GONE
                        map_view_search_bar.postDelayed({
                            val mlp = map_view_search_bar.layoutParams as ViewGroup.MarginLayoutParams
                            mlp.width = ViewGroup.LayoutParams.MATCH_PARENT
                            mlp.rightMargin = currentMarginRight
                            map_view_search_bar.layoutParams = mlp
                        }, 10)
                    }

                    override fun onAnimationStart(animation: Animator?, isReverse: Boolean) {
                    }
                })
                animator.duration = 300
                animator.interpolator = LinearInterpolator()
                animator.start()
            }
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
            // ?????????????????????/???????????????????????????
            if (displaySearchBar) {
                viewModel.searchBarVisible.value = View.VISIBLE
            }

            // ??????????????????????????????????????????
            viewModel.reSearchButtonVisible.value = View.GONE

            displaySearchBar = false
            resetCameraPosition = false
        }
    }


    override fun onClickReSearch(view: View) {
        val position = mMap.cameraPosition
        // ?????????/???????????????????????????????????????????????????????????????
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

        // ????????????????????????????????????????????????????????????????????????????????????
        val position = mMap.cameraPosition
        val latLng = viewModel.latLng.value ?: position.target
        val query = viewModel.query(latLng)
        fetchJobs(query)
    }

    override fun onClickList(view: View) {
        // ??????????????????????????????????????????
        Tracking.logEvent(event= "push_toggle_dispaly", params= bundleOf())
        Tracking.track(name= "push_toggle_dispaly")

        Intent(this, ListViewActivity::class.java).let {
            it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            it.putExtra(SearchJobActivity.EXTRA_SEARCH_CONDITIONS, viewModel.query(viewModel.latLng.value ?: LocationManager.defaultLatLng))
            startActivity(it)
        }
    }

    // ??????????????????
    override fun onClickCurrentLocation(view: View) {
        // ?????????????????????????????????????????????
        Tracking.logEvent(event= "push_reload_location", params= bundleOf())
        Tracking.track(name= "push_reload_location")

        val updateRequest = CameraUpdateFactory.newLatLngZoom(locationManager.latLngOrDefault, defaultZoom)
        Log.v(ErikuraApplication.LOG_TAG, "GMS: animateCamera(currentLocation): $updateRequest")
        mMap.animateCamera(updateRequest)
    }

    // ???????????????????????????????????????
    override fun onClickCarouselItem(job: Job) {
        Log.v("ErikuraCarouselCel", "Click: ${job.toString()}")

        val jobsOnLocation = viewModel.jobsByLocation.value?.get(job.latLng) ?: listOf()
        if (jobsOnLocation.size > 1) {
            // ???????????????????????????????????????
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
//        val visibleBounds = mMap.projection.visibleRegion.latLngBounds

        viewModel.markerMap.forEach { _jobId, erikuraMarker ->
//            if (visibleBounds.contains(erikuraMarker.marker.position)) {
                val point = mMap.projection.toScreenLocation(erikuraMarker.marker.position)
                val topLeft = Point(
                    (point.x - (100 / 2 * displayMetrics.density)).toInt(),
                    (point.y - 47 * displayMetrics.density).toInt())
                val bottomRight = Point(
                    (point.x + (100 / 2 * displayMetrics.density)).toInt(),
                    point.y)

                val rect = Rect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
                if (rect.contains(tapPoint)) {
                    hitMarkers.add(erikuraMarker)
                    Log.v(ErikuraApplication.LOG_TAG, "tapPoint: (${tapPoint.x}, ${tapPoint.y}), point: (${point.x}, ${point.y}) => (${topLeft.x}, ${topLeft.y})-(${bottomRight.x}-${bottomRight.y})")
                }
//            }
        }

        hitMarkers.sortedByDescending { it.marker.zIndex }.let { sortedHitMarkers ->
            if (sortedHitMarkers.isNotEmpty()) {
                sortedHitMarkers.first().let { erikuraMarker ->
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

                    val layoutManager = carouselView?.layoutManager as LinearLayoutManager

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

    // ????????????????????????????????????????????????????????????????????????????????????????????????
    val searchBarVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    // ?????????????????????????????????????????????????????????????????????
    var reSearchButtonVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    // ?????????????????????????????????????????????????????????
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
        // ??????????????????????????????????????????????????????????????????
        if (!(coach.value ?: false)) return View.GONE
        // ????????????????????????????????????????????????????????????
        if (step.value == 0) return View.VISIBLE
        if (step.value == 1) return View.VISIBLE
        // ????????????????????????????????????????????????????????????????????????
        return View.GONE
    }

    fun tap(@Suppress("UNUSED_PARAMETER") view: View) {
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