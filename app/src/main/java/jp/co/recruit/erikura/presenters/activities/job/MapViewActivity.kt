package jp.co.recruit.erikura.presenters.activities.job

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Rect
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
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.business.models.JobStatus
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityMapViewBinding
import jp.co.recruit.erikura.databinding.ErikuraCarouselCellBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.io.IOUtils
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

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
                    fetchJobs()
                }
            }
        }

    private fun fetchJobs() {
        val query = JobQuery(
            latitude = latLng?.latitude,
            longitude = latLng?.longitude
        )
        Api(this@MapViewActivity).searchJobs(query) { jobs ->
            Log.d("JOBS: ", jobs.toString())

            // FIXME: カルーセル表示

            jobs.forEach { job ->
                // FIXME: マーカー画像の差し替え
                // FIXME: マーカー画像のキャッシュの仕組みを作成
                // FIXME: マーカーがタップされた場合の処理の実装
                val markerOptions = MarkerOptions()
                    .position(LatLng(job.latitude, job.longitude))
                    .title(job.workingPlace)
                mMap.addMarker(markerOptions)
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

        val carouselView: RecyclerView = findViewById(R.id.map_view_carousel)
        carouselView.setHasFixedSize(true)
        carouselView.adapter = ErikuraCarouselAdaptor(this, listOf())
        carouselView.addItemDecoration(ErikuraCarouselCellDecoration())
        if (carouselView.adapter is ErikuraCarouselAdaptor) {
            var adapter = carouselView.adapter as ErikuraCarouselAdaptor
            adapter.onClickListner = object: ErikuraCarouselAdaptor.OnClickListener {
                override fun onClick(job: Job) {
                    // FIXME: 同一地点に複数の案件があれば案件選択モーダルを表示する
                    // FIXME: 同一地点に案件がなければ、案件詳細画面に遷移する
                    Log.v("ErikuraCarouselCel", "Click: ${job.toString()}")
                }
            }
        }

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
//        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }
}

class MapViewViewModel: ViewModel() {
//    val keyword: MutableLiveData<String> = MutableLiveData()
//    val minimumReward: MutableLiveData<Int> = MutableLiveData()
//    val maximumReward: MutableLiveData<Int> = MutableLiveData()
//    val minimumWorkingTime: MutableLiveData<Int> = MutableLiveData()
//    val maximumWorkingTime: MutableLiveData<Int> = MutableLiveData()
//    val jobKind: MutableLiveData<JobKind> = MutableLiveData()
//    val jobs: MutableLiveData<List<Job>> = MutableLiveData()
}

interface MapViewEventHandlers {
//    fun onClickLogin(view: View)
//    fun onClickReminderLink(view: View)
//    fun onClickUnreachLink(view: View)
}

class ErikuraCarouselViewHolder(val activity: AppCompatActivity, val binding: ErikuraCarouselCellBinding): RecyclerView.ViewHolder(binding.root) {
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
        job.thumbnailUrl?.let {
            Api(activity).downloadResource(URL(it), createTempFile()) { file ->
                // 画像読み込み
                val bitmap = BitmapFactory.decodeFile(file.path)
                image.setImageBitmap(bitmap)
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
