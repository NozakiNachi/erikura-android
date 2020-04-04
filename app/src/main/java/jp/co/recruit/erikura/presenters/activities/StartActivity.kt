package jp.co.recruit.erikura.presenters.activities

import android.Manifest
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.VideoView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityStartBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.registration.RegisterEmailActivity
import jp.co.recruit.erikura.presenters.activities.tutorial.PermitLocationActivity
import jp.co.recruit.erikura.presenters.util.LocationManager
import jp.co.recruit.erikura.presenters.util.MessageUtils
import jp.co.recruit.erikura.services.NotificationData
import org.apache.commons.lang.builder.ToStringBuilder

class StartActivity : BaseActivity(), StartEventHandlers {
    lateinit var video: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

//        val pedometerServiceIntent = Intent(this, PedometerService::class.java)
//        startService(pedometerServiceIntent)

        val intent = getIntent()
        if (intent != null) {
            intent.getStringExtra("extra")?.let { data ->
                NotificationData.fromJSON(data)?.openURI?.let { uri ->
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                    return
                }
            }
        }

        // エラーメッセージを受け取る
        val errorMessages = intent.getStringArrayExtra("errorMessages")
        if(errorMessages != null){
            Api(this).displayErrorAlert(errorMessages.asList())
        }

        val binding: ActivityStartBinding = DataBindingUtil.setContentView(this, R.layout.activity_start)
        binding.lifecycleOwner = this
        binding.handlers = this

        video = findViewById(R.id.v)

        // ビデオの設定
        video.setVideoURI(Uri.parse("android.resource://" + this.packageName + "/" + R.raw.movie))
        video.start()
        // ループ再生処理
        video.setOnCompletionListener {
            video.seekTo(0)
            video.start()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                ErikuraApplication.REQUEST_ACTIVITY_RECOGNITION_PERMISSION_ID
            )
        }
        else {
            setupSensor()
        }

        if (Api.isLogin) {
            // すでにログイン済の場合には以降の処理はスキップして、地図画面に遷移します
            Intent(this, MapViewActivity::class.java).let { intent ->
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            }
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        video.resume()
        video.start()
    }

    override fun onPause() {
        super.onPause()
        video.stopPlayback()
    }

    override fun onClickRegisterButton(view: View) {
        val intent = Intent(this, RegisterEmailActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onClickLoginButton(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onClickStartWithoutLogin(view: View) {
        if (ErikuraApplication.instance.isOnboardingDisplayed()) {
            // 地図画面へ遷移
            val intent = Intent(this, MapViewActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
        else {
            // 位置情報の許諾、オンボーディングを表示します
            Intent(this, PermitLocationActivity::class.java).let { intent ->
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
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
            ErikuraApplication.REQUEST_ACTIVITY_RECOGNITION_PERMISSION_ID -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    setupSensor()
                }
            }
        }
    }

    fun setupSensor() {
        val sensorManager: SensorManager = getSystemService(Activity.SENSOR_SERVICE) as SensorManager
        Log.v("SENSOR MANAGER", ToStringBuilder.reflectionToString(sensorManager))

        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        sensors.forEach { sensor ->
            Log.v("SENSOR LIST: ", sensor.name)
        }


        val listener = object: SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                Log.v("SENSOR_ACCURACY", "CHANGED")
            }

            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                    Log.v("SENSOR CHANGED", ToStringBuilder.reflectionToString(event))
                }
            }
        }
        val sensorTypes = arrayOf(
            Sensor.TYPE_STEP_COUNTER
        )
        Log.v("FEATURE: ", "step counter: ${packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)}")
        Log.v("FEATURE: ", "step detector: ${packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR)}")

        sensorTypes.forEach { type ->
            sensorManager.getDefaultSensor(type)?.let { sensor ->
                Log.v("SENSOR GET", sensor.name)
                val result = sensorManager.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    500
                )
                if (result ?: false) {
                    Log.v("SENSOR REGISTER: ", "${sensor.name} success")
                }
                else {
                    Log.v("SENSOR REGISTER: ", "${sensor.name} failed")
                }
            }
        }
        sensorManager.flush(listener)
    }

}

interface StartEventHandlers {
    fun onClickRegisterButton(view: View)
    fun onClickLoginButton(view: View)
    fun onClickStartWithoutLogin(view: View)
}