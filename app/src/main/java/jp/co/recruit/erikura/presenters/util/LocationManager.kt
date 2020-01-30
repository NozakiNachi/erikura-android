package jp.co.recruit.erikura.presenters.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.ErikuraApplication
import javax.inject.Singleton

typealias LocationUpdateCallback = (LatLng) -> Unit

@Singleton
class LocationManager {
    companion object {
        const val REQUEST_ACCESS_FINE_LOCATION_ID = 1

        const val Interval: Long = 5000
        const val FastestInterval: Long = 1000

        // 自己位置が取得できない場合のデフォルト値は渋谷駅
        val defaultLatLng = LatLng(35.658322, 139.70163)
    }

    private var fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(ErikuraApplication.applicationContext)
    private var clientSettings: SettingsClient =
        LocationServices.getSettingsClient(ErikuraApplication.applicationContext)
    var latLng: LatLng? = null

    val latLngOrDefault: LatLng get() = latLng ?: defaultLatLng

    private var locationUpdateCallbacks: MutableList<LocationUpdateCallback> = ArrayList()

    private var locationCallback = object: LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)

            locationResult?.let {
                val location = LatLng(
                    it.lastLocation.latitude,
                    it.lastLocation.longitude
                )
                latLng = location
                locationUpdateCallbacks.forEach {
                    it.invoke(location)
                }
            }
        }
    }

    fun checkPermission(activity: FragmentActivity): Boolean {
        return ActivityCompat.checkSelfPermission(activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(activity: FragmentActivity) {
        if (!checkPermission(activity)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_ACCESS_FINE_LOCATION_ID
            )
        }
    }

    fun start(activity: FragmentActivity) {
        val locationRequest = LocationRequest().apply {
            setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
            setInterval(Interval)
            setFastestInterval(FastestInterval)
        }
        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

        val responseTask = clientSettings.checkLocationSettings(locationSettingsRequest)
        responseTask.addOnSuccessListener {
            if (!checkPermission(activity)) {
                return@addOnSuccessListener
            }
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        }
        responseTask.addOnFailureListener { e ->
            when(e) {
                is ApiException -> {
                    when (e.statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            val e2 = e as ResolvableApiException
                            e2.startResolutionForResult(activity, REQUEST_ACCESS_FINE_LOCATION_ID)
                            Log.d("ERROR", "Dialog Displayed")
                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            Log.d("ERROR", "Unable to turn on location service", e)
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        fusedClient.removeLocationUpdates(locationCallback)
    }

    fun addLocationUpdateCallback(callback: LocationUpdateCallback) {
        locationUpdateCallbacks.add(callback)
    }

    fun removeLocationUpdateCallback(callback: LocationUpdateCallback) {
        locationUpdateCallbacks.remove(callback)
    }

    fun clearLocationUpdateCallback() {
        locationUpdateCallbacks.clear()
    }
}