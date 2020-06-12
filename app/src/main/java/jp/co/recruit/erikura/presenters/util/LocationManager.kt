package jp.co.recruit.erikura.presenters.util

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.ErikuraApplication
import javax.inject.Singleton

typealias LocationUpdateCallback = (LatLng) -> Unit

@Singleton
class LocationManager {

    companion object {
        const val Interval: Long = 5000
        const val FastestInterval: Long = 1000

        // 自己位置が取得できない場合のデフォルト値は渋谷駅
        val defaultLatLng = LatLng(35.658322, 139.70163)
    }

    val fineLocationCheckedNotAskAgainKey = "ACCESS_FINE_LOCATION_CHECKED_NOT_ASK_AGAIN"
    var checkedNotAskAgain: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(ErikuraApplication.instance).getBoolean(fineLocationCheckedNotAskAgainKey, false)
        set(value) {
            PreferenceManager.getDefaultSharedPreferences(ErikuraApplication.instance)
                .edit()
                .putBoolean(fineLocationCheckedNotAskAgainKey, value)
                .commit()
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

    fun checkPermission(fragment: Fragment): Boolean {
        return fragment.activity?.let {
            checkPermission(it)
        } ?: false
    }

    fun requestPermission(activity: FragmentActivity) {
        if (!checkPermission(activity)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                ErikuraApplication.REQUEST_ACCESS_FINE_LOCATION_PERMISSION_ID
            )
        }
    }

    fun requestPermission(fragment: Fragment) {
        if (!checkPermission(fragment)) {
            fragment.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                ErikuraApplication.REQUEST_ACCESS_FINE_LOCATION_PERMISSION_ID
            )
        }
    }

    fun onRequestPermissionResult(activity: FragmentActivity,
                                  requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
                                  displayAlert: Boolean = true,
                                  onDisplayAlert: ((dialog: AlertDialog) -> Unit)? = null,
                                  onPermissionNotGranted: (() -> Unit)? = null,
                                  onPermissionGranted: (() -> Unit)? = null) {
        // ACCESS_FINE_LOCATION_PERMISSION 以外の場合にはスキップします
        if (requestCode != ErikuraApplication.REQUEST_ACCESS_FINE_LOCATION_PERMISSION_ID)
            return

        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            checkedNotAskAgain = false
            start(activity)
            onPermissionGranted?.invoke()
        }
        else {
            checkedNotAskAgain = activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
            if (displayAlert) {
                val dialog = MessageUtils.displayLocationAlert(activity) {
                    onPermissionNotGranted?.invoke()
                }
                onDisplayAlert?.invoke(dialog)
            }
            else {
                onPermissionNotGranted?.invoke()
            }
        }
    }

    fun onRequestPermissionResult(fragment: Fragment,
                                  requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
                                  displayAlert: Boolean = true,
                                  onDisplayAlert: ((dialog: AlertDialog) -> Unit)? = null,
                                  onPermissionNotGranted: (() -> Unit)? = null,
                                  onPermissionGranted: (() -> Unit)? = null) {
        onRequestPermissionResult(fragment.activity!!, requestCode, permissions, grantResults, displayAlert, onDisplayAlert, onPermissionNotGranted, onPermissionGranted)
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
                            e2.startResolutionForResult(activity, ErikuraApplication.REQUEST_ACCESS_FINE_LOCATION_PERMISSION_ID)
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