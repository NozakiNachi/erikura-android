package jp.co.recruit.erikura.presenters.fragments

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MapStyleOptions
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.FragmentMapViewBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.util.LocationManager

class MapViewFragment: BaseJobDetailFragment, OnMapReadyCallback {
    companion object {
        fun newInstance(job: Job?, user: User?): MapViewFragment {
            return MapViewFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, job, user)
                }
            }
        }
    }

    constructor(): super()

    private val viewModel: MapViewFragmentViewModel by lazy {
        ViewModelProvider(this).get(MapViewFragmentViewModel::class.java)
    }

    private lateinit var mMap: GoogleMap

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
        // マーカーの再設定
        setupMarker()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentMapViewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapView_map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.isIndoorEnabled = false
        mMap.uiSettings.setScrollGesturesEnabled(false)
        mMap.uiSettings.setZoomControlsEnabled(false)
        mMap.uiSettings.setZoomGesturesEnabled(false)
        mMap.uiSettings.setTiltGesturesEnabled(false)
        mMap.uiSettings.setRotateGesturesEnabled(false)

        // マーカーがタップされても何も実行しないようにします
        mMap.setOnMarkerClickListener { marker ->
            true
        }

        setupMarker()
    }

    private fun setupMarker() {
        if (::mMap.isInitialized) {
            mMap.clear()
            viewModel.marker.value = null

            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    job?.latLng ?: LocationManager.defaultLatLng,
                    MapViewActivity.defaultZoom)
            )
            try {
                val styleOptions = MapStyleOptions.loadRawResourceStyle(ErikuraApplication.instance.applicationContext, R.raw.style)
                mMap.setMapStyle(styleOptions)
            }
            catch (e: Resources.NotFoundException) {
                Log.e("ERROR", e.message, e)
            }

            job?.let { job ->
                val erikuraMarker = ErikuraMarkerView.build(activity!!, mMap, job, false) {  }
                erikuraMarker.active = true
                viewModel.marker.value = erikuraMarker
            }
        }
    }
}

class MapViewFragmentViewModel: ViewModel() {
    val marker: MutableLiveData<ErikuraMarkerView> = MutableLiveData()
}