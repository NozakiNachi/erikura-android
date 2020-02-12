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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.maps.android.SphericalUtil
import io.realm.Realm
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.business.models.PeriodType
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityMapViewBinding
import jp.co.recruit.erikura.presenters.activities.OwnJobsActivity
import jp.co.recruit.erikura.presenters.activities.MypageActivity
import jp.co.recruit.erikura.presenters.fragments.ErikuraMarkerView
import jp.co.recruit.erikura.presenters.util.LocationManager
import jp.co.recruit.erikura.presenters.util.MessageUtils
import jp.co.recruit.erikura.presenters.view_models.BaseJobQueryViewModel

class MapViewActivity : AppCompatActivity(), OnMapReadyCallback, MapViewEventHandlers {
    companion object {
        // SearchJob にわたすID
        const val REQUEST_SEARCH_CONDITIONS = 1
        // デフォルトズーム
        const val defaultZoom = 15.0f
    }

    private val viewModel: MapViewViewModel by lazy {
        ViewModelProvider(this).get(MapViewViewModel::class.java)
    }

    private val locationManager: LocationManager = ErikuraApplication.locationManager

    /** GoogleMap のカメラ位置を移動中かを保持します (true は移動中) */
    private var cameraMoving: Boolean = false
    private var zoomInitialized: Boolean = false
    private var displaySearchBar: Boolean = true
    private var resetCameraPosition: Boolean = false

    private lateinit var mMap: GoogleMap
    private lateinit var carouselView: RecyclerView
    private lateinit var adapter: ErikuraCarouselAdapter
    private var firstFetchRequested: Boolean = false

    private fun fetchJobs(query: JobQuery) {
        Api(this@MapViewActivity).searchJobs(query) { jobs ->
            Log.d("JOBS: ", jobs.toString())
            if (jobs.isNotEmpty()) {
                // viewModel の案件情報を更新します
                viewModel.jobs.value = jobs
                viewModel.jobsByLocation.value = JobUtils.summarizeJobsByLocation(jobs)

                // マーカーの情報をクリアします
                viewModel.markerMap.clear()
                viewModel.activeMaker = null
                // マーカーを地図から削除します
                mMap.clear()

                // カルーセルの更新を行います
                val carouselView: RecyclerView = findViewById(R.id.map_view_carousel)
                if (carouselView.adapter is ErikuraCarouselAdapter) {
                    var adapter = carouselView.adapter as ErikuraCarouselAdapter
                    adapter.data = jobs
                    adapter.jobsByLocation = viewModel.jobsByLocation.value ?: mapOf()
                    adapter.notifyDataSetChanged()
                }

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

                // 最も距離が近い案件を取得します
                val nearest = jobs.sortedBy { SphericalUtil.computeDistanceBetween(it.latLng, query.latLng) }.first()
                var nearestMarker: ErikuraMarkerView = viewModel.markerMap[nearest.id]!!
                var nearestIndex: Int = nearestMarker.marker.tag as Int
                // マーカーをアクティブに変更します
                viewModel.activeMaker = nearestMarker
                // 最も近い案件に地図を移動します
                resetCameraPosition = true
                val updateRequest: CameraUpdate = if (zoomInitialized)
                    CameraUpdateFactory.newLatLng(nearest.latLng)
                else
                    CameraUpdateFactory.newLatLngZoom(nearest.latLng, defaultZoom)
                mMap.animateCamera(updateRequest)
                // カルーセルを最も近い案件に変更します
                var layoutManager = carouselView.layoutManager as LinearLayoutManager
                layoutManager.scrollToPosition(nearestIndex)

                Log.v("Fetch Job", "Nearest: ${nearestIndex}, ${layoutManager.toString()}")
            }
            else {
                MessageUtils.displayAlert(this, listOf("検索した地域で", "仕事が見つからなかったため、", "一番近くの仕事を表示します")) {
                    // クリアした検索条件での再検索を行います
                    val newQuery = JobQuery(
                        latitude = locationManager.latLngOrDefault.latitude,
                        longitude = locationManager.latLngOrDefault.longitude)
                    if (query != newQuery) {
                        viewModel.apply(newQuery)
                        fetchJobs(newQuery)
                    }
                }
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

        // 下部のタブの選択肢を仕事を探すに変更
        val nav: BottomNavigationView = findViewById(R.id.map_view_navigation)
        nav.selectedItemId = R.id.tab_menu_search_jobs

        adapter = ErikuraCarouselAdapter(this, listOf(), viewModel.jobsByLocation.value ?: mapOf())
        adapter.onClickListener = object: ErikuraCarouselAdapter.OnClickListener {
            override fun onClick(job: Job) {
                onClickCarouselItem(job)
            }
        }

        carouselView = findViewById(R.id.map_view_carousel)
//        carouselView.setHasFixedSize(true)
        carouselView.addItemDecoration(ErikuraCarouselCellDecoration())
        carouselView.adapter = adapter

        (carouselView.layoutManager as? LinearLayoutManager)?.let {
            it.stackFromEnd = true
        }

        carouselView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

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

        // 各種ボタンの表示・非表示の切り替えを行います
        viewModel.reSearchButtonVisible.value = View.GONE           // 初期表示ではこの地点で再検索ボタンを非表示とする
        viewModel.searchBarVisible.value = View.VISIBLE             // 初期表示では検索条件バーを表示する

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
                mMap.animateCamera(CameraUpdateFactory.newLatLng(it))
                firstFetchRequested = true
                val query = viewModel.query(it)
                fetchJobs(query)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // ズームの初期設定を行っておきます
        mMap.moveCamera(CameraUpdateFactory.zoomBy(defaultZoom))

        // FIXME: ロゴの位置を変更する
        // MEMO: 下記のやり方だと地図の中心位置がずれる
        //mMap.setPadding(0, 0, 0, 300)

        // 地図上のアイコンをグレーにするためにスタイル設定を行います
        try {
            val styleOptions = MapStyleOptions.loadRawResourceStyle(this, R.raw.style)
            mMap.setMapStyle(styleOptions)
        }
        catch (e: Resources.NotFoundException) {
            Log.e("ERROR", e.message, e)
        }

        // カメラ移動に関するコールバックの登録
        mMap.setOnCameraMoveStartedListener { onCameraMoveStarted(it) }
        mMap.setOnCameraMoveCanceledListener { onCameraMoveCanceled() }
        mMap.setOnCameraIdleListener { onCameraIdle() }

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
            LocationManager.REQUEST_ACCESS_FINE_LOCATION_ID -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    locationManager.start(this)
                }
                else {
                    MessageUtils.displayLocationAlert(this)
                }
            }
        }
    }

    fun onCameraMoveStarted(_reason: Int) {
        cameraMoving = true

        // 案件取得によるカメラリセット以外の場合
        if (!resetCameraPosition) {
            // 再検索ボタンを表示
            viewModel.reSearchButtonVisible.value = View.VISIBLE
            // 検索バーを非表示
            viewModel.searchBarVisible.value = View.GONE
        }
    }

    fun onCameraMoveCanceled() {
        cameraMoving = false
    }

    fun onCameraIdle() {
        if (cameraMoving) {
            zoomInitialized = true
            cameraMoving = false
            onCameraMoveFinished()
        }
    }

    fun onCameraMoveFinished() {
        cameraMoving = false

        if (resetCameraPosition) {
            // 検索バーの表示/非表示を設定します
            if (displaySearchBar) {
                viewModel.searchBarVisible.value = View.VISIBLE
                // FIXME: displaySearchBar = false の場合の振る舞いを確認
            }

            // 再検索ボタンは非表示とします
            viewModel.reSearchButtonVisible.value = View.GONE

            displaySearchBar = false
            resetCameraPosition = false
        }
    }


    override fun onClickReSearch(view: View) {
        val position = mMap.cameraPosition
        fetchJobs(viewModel.query(position.target))
    }

    override fun onClickSearch(view: View) {
        viewModel.searchBarVisible.value = View.VISIBLE
    }

    override fun onClickSearchBar(view: View) {
        val intent = Intent(this, SearchJobActivity::class.java)
        // FIXME: 緯度経度については要検討
        intent.putExtra(SearchJobActivity.EXTRA_SEARCH_CONDITIONS, viewModel.query(locationManager.latLngOrDefault))
        startActivityForResult(intent, REQUEST_SEARCH_CONDITIONS, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
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
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        // 地図画面は閉じておきます
        finish()
    }

    // 現在地に戻る
    override fun onClickCurrentLocation(view: View) {
        mMap.animateCamera(CameraUpdateFactory.newLatLng(locationManager.latLngOrDefault))
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
            intent.putExtra("job", job)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.v("MENU ITEM SELECTED: ", item.toString())
        when(item.itemId) {
            R.id.tab_menu_search_jobs -> {
                // 何も行いません
            }
            R.id.tab_menu_applied_jobs -> {
                Intent(this, OwnJobsActivity::class.java).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                }
            }
            R.id.tab_menu_mypage -> {
                // MEMO: マイページ画面遷移コードを動作確認のため実装
                // FIXME: 画面遷移の実装
                // Toast.makeText(this, "マイページ画面に遷移", Toast.LENGTH_LONG).show()
                Intent(this, MypageActivity::class.java).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                }
            }
        }
        return true
    }
}

class MapViewViewModel: BaseJobQueryViewModel() {
    val resources: Resources get() = ErikuraApplication.instance.applicationContext.resources

    val jobs: MutableLiveData<List<Job>> = MutableLiveData()
    val jobsByLocation: MutableLiveData<Map<LatLng, List<Job>>> = MutableLiveData()
    val markerMap: MutableMap<Int, ErikuraMarkerView> = HashMap()

    // FIXME: 初期状態では検索バーは表示し、なにか操作が行われたら非表示とする
    val searchBarVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    // FIXME: 初期状態では非表示、位置を変更すると表示される
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
                PeriodType.ALL -> resources.getDrawable(R.drawable.before_open_2x, null)
                PeriodType.ACTIVE -> resources.getDrawable(R.drawable.before_entry_2x_on, null)
                else -> resources.getDrawable(R.drawable.before_open_2x, null)
            }
        }
    }

    init {
        periodType.value = PeriodType.ALL
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

    fun onNavigationItemSelected(item: MenuItem): Boolean
}
