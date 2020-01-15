package jp.co.recruit.erikura.presenters.activities.job

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import io.realm.Realm
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.business.models.JobStatus
import jp.co.recruit.erikura.business.models.PeriodType
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.storage.Asset
import jp.co.recruit.erikura.data.storage.AssetsManager
import jp.co.recruit.erikura.databinding.ActivityMapViewBinding
import jp.co.recruit.erikura.databinding.ErikuraCarouselCellBinding
import jp.co.recruit.erikura.databinding.FragmentMarkerBinding
import jp.co.recruit.erikura.presenters.view_models.MarkerViewModel
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class MapViewActivity : AppCompatActivity(), OnMapReadyCallback, MapViewEventHandlers {
    companion object {
        // デフォルト位置情報
        val defaultLatLng = LatLng(35.658322, 139.70163)
        val defaultZoom = 15.0f
    }

    private val viewModel: MapViewViewModel by lazy {
        ViewModelProvider(this).get(MapViewViewModel::class.java)
    }

    private lateinit var mMap: GoogleMap
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var clientSettings: SettingsClient
    private var latLng: LatLng? = null
        set(value) {
            val isFirst: Boolean = (field == null)
            field = value
            if (isFirst) {
                field?.let {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(it.latitude, it.longitude), defaultZoom
                    ))
                    val query = JobQuery(
                        latitude = latLng?.latitude,
                        longitude = latLng?.longitude
                    )
                    fetchJobs(query)
                }
            }
        }
    private lateinit var carouselView: RecyclerView

    private fun fetchJobs(query: JobQuery) {
        Api(this@MapViewActivity).searchJobs(query) { jobs ->
            Log.d("JOBS: ", jobs.toString())
            viewModel.jobs.value = jobs
            viewModel.markerMap.clear()

            jobs.forEachIndexed { i, job ->
                // FIXME: マーカー画像のキャッシュの仕組みを作成
                // FIXME: マーカーがタップされた場合の処理の実装
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
            jobs.first().let {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), defaultZoom))
            }
            val carouselView: RecyclerView = findViewById(R.id.map_view_carousel)
            if (carouselView.adapter is ErikuraCarouselAdaptor) {
                var adapter = carouselView.adapter as ErikuraCarouselAdaptor
                adapter.data = jobs
                adapter.notifyDataSetChanged()
            }
        }
    }

    private val locCallback = object: LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            // 自己位置の結果を取得して、地図を移動します
            locationResult?.let {
                val loc: Location = it.lastLocation
                this@MapViewActivity.latLng = LatLng(loc.latitude, loc.longitude)
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

        val adapter = ErikuraCarouselAdaptor(this, listOf())
        adapter.onClickListner = object: ErikuraCarouselAdaptor.OnClickListener {
            override fun onClick(job: Job) {
                // FIXME: 同一地点に複数の案件があれば案件選択モーダルを表示する
                // FIXME: 同一地点に案件がなければ、案件詳細画面に遷移する
                Log.v("ErikuraCarouselCel", "Click: ${job.toString()}")
            }
        }

        carouselView = findViewById(R.id.map_view_carousel)
        carouselView.setHasFixedSize(true)
        carouselView.addItemDecoration(ErikuraCarouselCellDecoration())
        carouselView.adapter = ErikuraCarouselAdaptor(this, listOf())

        carouselView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val adapter = recyclerView.adapter as ErikuraCarouselAdaptor

                val layoutManager: LinearLayoutManager = carouselView.layoutManager as LinearLayoutManager
                val position = layoutManager.findFirstCompletelyVisibleItemPosition()

                Log.v("INDEX:", "Position: ${position}, length: ${adapter.data.size}")
                if (position >= 0 && adapter.data.size > 0) {
                    val job = adapter.data[position]
                    Log.v("VISIBLE JOB: ", job.toString())

                    this@MapViewActivity.runOnUiThread{
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(
                            LatLng(job.latitude, job.longitude)
                        ))

                        viewModel.activeMaker = viewModel.markerMap[job.id]
                    }
                }
            }
        })

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(carouselView)

        // Client の準備
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        clientSettings = LocationServices.getSettingsClient(this)

        // パーミッションの確認 & 要求
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // FIXME: onRequestPermissionsResult での対応は？
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.jobs_map_view_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        startWatchLocation()
    }

    private fun startWatchLocation() {
        // 位置リクエストの作成
        val locRequest: LocationRequest = LocationRequest()
        locRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER)
            .setInterval(5000)
            .setFastestInterval(1000)
        // 位置情報に関する設定リクエスト情報を作成
        val locationSettingsRequest: LocationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locRequest)
            .build()

        clientSettings.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener { locationSettingsResponse ->
                if (ActivityCompat.checkSelfPermission(this@MapViewActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return@addOnSuccessListener
                }

                fusedClient.requestLocationUpdates(locRequest, locCallback, Looper.myLooper())
            }
            .addOnFailureListener { e ->
                // FIXME: GPSが有効になっていない場合の対応など
                //        https://qiita.com/nbkn/items/41b3dd5a86be6e2b57bf
                Log.d("MapView: Error", e.message, e)
            }
    }

    override fun onPause() {
        super.onPause()
        fusedClient.removeLocationUpdates(locCallback)
    }

    override fun onResume() {
        super.onResume()
        startWatchLocation()
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
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng ?: defaultLatLng, defaultZoom))

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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // 現在地に戻る
    override fun onClickCurrentLocation(view: View) {
        mMap.animateCamera(CameraUpdateFactory.newLatLng(this.latLng))
    }
}

class MapViewViewModel: ViewModel() {
    val resources: Resources get() = ErikuraApplication.instance.applicationContext.resources

    val jobs: MutableLiveData<List<Job>> = MutableLiveData()
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
}

class ErikuraCarouselViewHolder(private val activity: AppCompatActivity, val binding: ErikuraCarouselCellBinding): RecyclerView.ViewHolder(binding.root) {
    var timeLimit: TextView = itemView.findViewById(R.id.erikura_carousel_cell_timelimit)
    var title: TextView = itemView.findViewById(R.id.erikura_carousel_cell_title)
    var image: ImageView = itemView.findViewById(R.id.erikura_carousel_cell_image)
    var reward: TextView = itemView.findViewById(R.id.erikura_carousel_cell_reward)
    var workingTime: TextView = itemView.findViewById(R.id.erikura_carousel_cell_working_time)
    var workingFinishAt: TextView = itemView.findViewById(R.id.erikura_carousel_cell_working_finish_at)
    var workingPlace: TextView = itemView.findViewById(R.id.erikura_carousel_cell_working_place)

    fun setup(context: Context, job: Job) {
        // 受付終了：応募済みの場合、now > working_finish_at の場合, gray, 12pt
        // 作業実施中: working 状態の場合, green, 12pt
        // 実施済み(未報告): finished の場合, green, 12pt
        // 作業報告済み: reported の場合, gray, 12pt
        // 募集開始までn日とn時間: 開始前(now < working_start_at)、
        // 作業終了までn日とn時間
        if (job.isPastOrInactive) {
            timeLimit.setTextColor(ContextCompat.getColor(context, R.color.warmGrey))
            timeLimit.text = "受付終了"     // FIXME: リソース化
        }
        else if (job.isFuture) {
            timeLimit.setTextColor(ContextCompat.getColor(context, R.color.waterBlue))
            val now = Date()
            val diff = job.workingStartAt.time - now.time
            val diffHours = diff / (60 * 60 * 1000)
            val diffDays = diffHours / 24
            val diffRestHours = diffHours % 24

            val sb = SpannableStringBuilder()
            sb.append("募集開始まで")
            if (diffDays > 0) {
                val start = sb.length
                sb.append(diffDays.toString())
                sb.setSpan(RelativeSizeSpan(16.0f / 12.0f), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.append("日")
            }
            if (diffDays > 0 && diffRestHours > 0) {
                sb.append("と")
            }
            if (diffRestHours > 0) {
                val start = sb.length
                sb.append(diffRestHours.toString())
                sb.setSpan(RelativeSizeSpan(16.0f / 12.0f), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.append("時間")
            }
            timeLimit.text = sb
        }
        when(job.status) {
            JobStatus.Working -> {
                timeLimit.setTextColor(ContextCompat.getColor(context, R.color.vibrantGreen))
                timeLimit.text = "作業実施中"
            }
            JobStatus.Finished -> {
                timeLimit.setTextColor(ContextCompat.getColor(context, R.color.vibrantGreen))
                timeLimit.text = "実施済み(未報告)"
            }
            JobStatus.Reported -> {
                timeLimit.setTextColor(ContextCompat.getColor(context, R.color.warmGrey))
                timeLimit.text = "作業報告済み"
            }
            else -> {
                timeLimit.setTextColor(ContextCompat.getColor(context, R.color.coral))
                val now = Date()
                val diff = job.workingFinishAt.time - now.time
                val diffHours = diff / (60 * 60 * 1000)
                val diffDays = diffHours / 24
                val diffRestHours = diffHours % 24

                val sb = SpannableStringBuilder()
                sb.append("作業終了まで")
                if (diffDays > 0) {
                    val start = sb.length
                    sb.append(diffDays.toString())
                    sb.setSpan(RelativeSizeSpan(16.0f / 12.0f), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.append("日")
                }
                if (diffDays > 0 && diffRestHours > 0) {
                    sb.append("と")
                }
                if (diffRestHours > 0) {
                    val start = sb.length
                    sb.append(diffRestHours.toString())
                    sb.setSpan(RelativeSizeSpan(16.0f / 12.0f), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.append("時間")
                }
                timeLimit.text = sb
            }
        }

        title.text = job.title
        reward.text = job.fee.toString() + "円"
        workingTime.text = job.workingTime.toString() + "分"
        val sd = SimpleDateFormat("YYYY/MM/dd HH:mm")
        workingFinishAt.text = "〜" + sd.format(job.workingFinishAt)
        workingPlace.text = job.workingPlace

        // ダウンロード
        job.thumbnailUrl?.let { url ->
            val assetsManager = ErikuraApplication.instance.erikuraComponent.assetsManager()

            assetsManager.fetchImage(activity, url) { bitmap ->
                activity.runOnUiThread {
                    image.setImageBitmap(bitmap)
                }
            }
        }
    }
}

class ErikuraCarouselAdaptor(val activity: AppCompatActivity, var data: List<Job>): RecyclerView.Adapter<ErikuraCarouselViewHolder>() {
    var onClickListner: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ErikuraCarouselViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding: ErikuraCarouselCellBinding = ErikuraCarouselCellBinding.inflate(layoutInflater, parent, false)

        return ErikuraCarouselViewHolder(activity, binding)
    }

    override fun onBindViewHolder(holder: ErikuraCarouselViewHolder, position: Int) {
        val job = data[position]

        holder.title.text = job.title
        holder.setup(ErikuraApplication.instance.applicationContext, job)

        holder.binding.root.setOnClickListener {
            onClickListner?.apply {
                onClick(job)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    interface OnClickListener {
        fun onClick(job: Job)
    }
}

class ErikuraCarouselCellDecoration: RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.left = view.resources.getDimensionPixelSize(R.dimen.erikura_carousel_cell_spacing)
        outRect.right = view.resources.getDimensionPixelSize(R.dimen.erikura_carousel_cell_spacing)
    }
}

// マーカーの作成が終わった場合に呼び出されるコールバック
typealias MarkerSetupCallback = (Marker) -> Unit

class ErikuraMarkerView(private val activity: AppCompatActivity, private val map: GoogleMap, private val job: Job) {
    companion object {
        const val BASE_ZINDEX: Float            = 5000f
        const val ACTIVE_ZINDEX_OFFSET: Float   = 10000f
        const val BOOST_ZINDEX_OFFSET: Float    = 1000f
        const val WANTED_ZINDEX_OFFSET: Float   = 2000f
        const val SOON_ZINDEX_OFFSET: Float     = -1000f
        const val FUTURE_ZINDEX_OFFSET: Float   = -2000f
        const val ENTRIED_ZINDEX_OFFSET: Float  = -6000f

        val assetsManager: AssetsManager get() = ErikuraApplication.instance.erikuraComponent.assetsManager()

        fun build(activity: AppCompatActivity, map: GoogleMap, job: Job, callback: MarkerSetupCallback?): ErikuraMarkerView {
            val markerView = ErikuraMarkerView(activity, map, job)
            callback?.invoke(markerView.marker)
            return markerView
        }
    }

    private val markerViewModel: MarkerViewModel = MarkerViewModel(job)
    lateinit var marker: Marker

    var active: Boolean
        get() = markerViewModel.active.value ?: false
        set(value) {
            markerViewModel.active.value = value
            updateMarkerIcon()
        }

    init {
        buildMarker()
    }

    private fun buildMarker(): Marker {
        val markerUrl = markerViewModel.markerUrl

        return assetsManager.lookupAsset(markerUrl)?.let {
            val bitmap = BitmapFactory.decodeFile(it.path)
            marker = map.addMarker(
                MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                    .position(LatLng(job.latitude, job.longitude))
            )
            marker
        } ?: run {
            marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(job.latitude, job.longitude))
            ).also {
                marker = it
                updateMarkerIcon()
            }
            marker
        }
    }

    private fun updateMarkerIcon() {
        val markerUrl = markerViewModel.markerUrl

        return assetsManager.lookupAsset(markerUrl)?.let {
            activity.run {
                val bitmap = BitmapFactory.decodeFile(it.path)
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap))
            }
        } ?: run {
            buildMarkerImage() {
                activity.run {
                    val bitmap = BitmapFactory.decodeFile(it.path)
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap))
                }
            }
        }
    }

    private fun buildMarkerImage(callback: (Asset) -> Unit) {
        val markerUrl: String = markerViewModel.markerUrl
        val binding = FragmentMarkerBinding.inflate(activity.layoutInflater, null, false)
        binding.lifecycleOwner = activity
        binding.viewModel = markerViewModel

        val build: () -> Unit = {
            val downloadHandler: (AppCompatActivity, String, Asset.AssetType, (Asset) -> Unit) -> Unit = { activity, urlString, type, onComplete ->
                binding.executePendingBindings()

                val markerView = binding.root
                markerView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)

                val markerImage = markerView.drawToBitmap(Bitmap.Config.ARGB_8888)

                val dest = assetsManager.generateDownloadFile()
                try {
                    FileOutputStream(dest).use { out ->
                        markerImage.compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()
                    }

                    assetsManager.removeExpiredCache(type)
                    assetsManager.realm.executeTransaction { realm ->
                        val asset = realm.createObject(Asset::class.java, markerUrl)
                        asset.path = dest.path
                        asset.lastAccessedAt = Date()
                        asset.type = type
                        onComplete(asset)
                    }
                } catch (e: IOException) {
                    Log.e("Error", e.message, e)
                    // FIXME: エラー処理として何をするべきか?
                }
            }
            assetsManager.downloadAsset(activity, markerUrl, Asset.AssetType.Marker, downloadHandler) { asset ->
                callback(asset)
            }
        }

        markerViewModel.iconUrl?.also { url ->
            assetsManager.fetchImage(activity, url.toString(), Asset.AssetType.Marker) { icon ->
                markerViewModel.icon.value = icon
                build()
            }
        } ?: run{
            build()
        }
    }
}
