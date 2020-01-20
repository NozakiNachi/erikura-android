package jp.co.recruit.erikura.presenters.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
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
    }

    val defaultLatLng =
        LatLng(35.658322, 139.70163)
    private var fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(
            ErikuraApplication.instance.applicationContext
        )
    private var clientSettings: SettingsClient =
        LocationServices.getSettingsClient(
            ErikuraApplication.instance.applicationContext
        )
    var latLng: LatLng? = null

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
            setPriority(LocationRequest.PRIORITY_LOW_POWER)
            setInterval(Interval)
            setFastestInterval(FastestInterval)
        }
        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

        clientSettings.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener { locationSettingsResponse ->
                if (!checkPermission(activity)) {
                    return@addOnSuccessListener
                }

                fusedClient.requestLocationUpdates(locationRequest, locationCallback,
                    Looper.myLooper()
                )
            }
            .addOnFailureListener { e ->
                // FIXME: GPSが有効になっていない場合の対応など
                //        https://qiita.com/nbkn/items/41b3dd5a86be6e2b57bf
                Log.d("MapView: Error", e.message, e)
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