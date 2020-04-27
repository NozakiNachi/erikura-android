package jp.co.recruit.erikura.presenters.activities.tutorial

import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.databinding.ActivityPermitLocationBinding
import jp.co.recruit.erikura.presenters.util.LocationManager
import jp.co.recruit.erikura.presenters.util.MessageUtils

class PermitLocationActivity : AppCompatActivity(), PermitLocationHandlers {
    private val locationManager: LocationManager = ErikuraApplication.locationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityPermitLocationBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_permit_location)
        binding.lifecycleOwner = this
        binding.handlers = this
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_introduction", params= bundleOf())
        Tracking.view(name= "/intro/enable_settings", title= "アプリ紹介画面（位置情報ON/OFF）")
    }

    override fun onClickPermitLocation(view: View) {
        if (!locationManager.checkPermission(this)) {


            locationManager.requestPermission(this)
        }
        else {
            startNextActivity()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        locationManager.onRequestPermissionResult(this, requestCode, permissions, grantResults,
            onPermissionNotGranted = {
                startNextActivity()
            },
            onPermissionGranted = {
                // 位置情報許諾のトラッキングの送出
                Tracking.logEvent(event= "accpet_geo_setting", params= bundleOf())
                Tracking.acceptGeoSetting()

                AndroidSchedulers.mainThread().scheduleDirect {
                    startNextActivity()
                }
            })
    }

    private fun startNextActivity() {
        Intent(this, Onboarding0Activity::class.java).let { intent ->
            startActivity(intent)
        }
    }
}

interface PermitLocationHandlers {
    fun onClickPermitLocation(view: View)
}