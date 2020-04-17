package jp.co.recruit.erikura.presenters.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import jp.co.recruit.erikura.ErikuraApplication
import java.util.*
import javax.inject.Singleton

@Singleton
class PedometerManager: SensorEventListener {
    val activityRecognitionCheckedNotAskAgainKey = "ACTIVITY_RECOGNITION_CHECKED_NOT_ASK_AGAIN"

    val sensorDelayMillis = 300
    /** センサマネージャ */
    val sensorManager = ErikuraApplication.instance.getSystemService(Activity.SENSOR_SERVICE) as SensorManager

    var stepCount: Int = 0

    var checkedNotAskAgain: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(ErikuraApplication.instance).getBoolean(activityRecognitionCheckedNotAskAgainKey, false)
        set(value) {
            PreferenceManager.getDefaultSharedPreferences(ErikuraApplication.instance)
                .edit()
                .putBoolean(activityRecognitionCheckedNotAskAgainKey, value)
                .apply()
        }

    /**
     * パーミッションがあるかを確認します
     */
    fun checkPermission(activity: FragmentActivity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            // Android 10 未満の端末は ACTIVITY_RECOGNITION 権限を持たない
            return true
        }
        else {
            return ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * ACTIVITY_RECOGNITION パーミッションを要求します
     */
    fun requestPermission(activity: FragmentActivity) {
        if (!checkPermission(activity)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                ErikuraApplication.REQUEST_ACTIVITY_RECOGNITION_PERMISSION_ID)
        }
    }

    fun requestPermission(fragment: Fragment) {
        fragment.activity?.let { activity ->
            if (!checkPermission(activity)) {
                fragment.requestPermissions(
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    ErikuraApplication.REQUEST_ACTIVITY_RECOGNITION_PERMISSION_ID)
            }
        }
    }

    fun onRequestPermissionResult(activity: FragmentActivity,
                                  requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
                                  onPermissionNotGranted: (() -> Unit)? = null,
                                  onPermissionGranted: (() -> Unit)? = null) {
        //　ACTIVITY_RECOGNITION 以外の場合にはスキップします
        if (requestCode != ErikuraApplication.REQUEST_ACTIVITY_RECOGNITION_PERMISSION_ID)
            return

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkedNotAskAgain = false
            // センサの読み取りを開始します
            start()
            onPermissionGranted?.invoke()
        }
        else {
            // 次回以降表示しないに回答しているか
            checkedNotAskAgain = !activity.shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION)

            onPermissionNotGranted?.invoke()
        }
    }

    fun onRequestPermissionResult(fragment: Fragment,
                                  requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
                                  onPermissionNotGranted: (() -> Unit)? = null,
                                  onPermissionGranted: (() -> Unit)? = null) {
        //　ACTIVITY_RECOGNITION 以外の場合にはスキップします
        if (requestCode != ErikuraApplication.REQUEST_ACTIVITY_RECOGNITION_PERMISSION_ID)
            return

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkedNotAskAgain = false
            // センサの読み取りを開始します
            start()
            onPermissionGranted?.invoke()
        }
        else {
            // 次回以降表示しないに回答しているか
            checkedNotAskAgain = !fragment.shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION)

            onPermissionNotGranted?.invoke()
        }
    }

    fun readStepCount(): Int {
        sensorManager.flush(this)
        return this.stepCount
    }

    fun start() {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
            val result = sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, sensorDelayMillis)
            if (result) {
                Log.v("SENSOR MANAGER", "${it.name} listener registered")
            }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        Log.v("SENSOR MANAGER", "listener unregistered")
    }

    /**
     * 精度が変更された場合に呼び出されるハンドラ
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 特に何も行いません
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { event ->
            val sensor: Sensor = event.sensor
            val values: FloatArray = event.values
            val timestamp: Long = event.timestamp

            if (sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val stepCount: Float = values.first()
                Log.v("TYPE_STEP_COUNTER: ", String.format("value = %f, timestamp=%s", stepCount, Date(timestamp)))
                this.stepCount = stepCount.toInt()
            }
        }
    }
}
