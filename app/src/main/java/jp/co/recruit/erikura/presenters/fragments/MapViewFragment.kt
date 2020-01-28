package jp.co.recruit.erikura.presenters.fragments

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentMapViewBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity

class MapViewFragment(private val activity: AppCompatActivity, val job: Job?) : Fragment(), OnMapReadyCallback {
    private val viewModel: MapViewFragmentViewModel by lazy {
        ViewModelProvider(this).get(MapViewFragmentViewModel::class.java)
    }

    private lateinit var mMap: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentMapViewBinding.inflate(inflater, container, false)
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
        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(job?.latitude?: 0.0, job?.longitude?: 0.0),
                MapViewActivity.defaultZoom)
            )
        try {
            val styleOptions = MapStyleOptions.loadRawResourceStyle(ErikuraApplication.instance.applicationContext, R.raw.style)
            mMap.setMapStyle(styleOptions)
        }
        catch (e: Resources.NotFoundException) {
            Log.e("ERROR", e.message, e)
        }

        if (job != null) {
            val erikuraMarker = ErikuraMarkerView.build(activity, mMap, job) {  }
            erikuraMarker.active = true
            viewModel.marker.value = erikuraMarker
        }

    }
}

class MapViewFragmentViewModel: ViewModel() {
    val marker: MutableLiveData<ErikuraMarkerView> = MutableLiveData()
}