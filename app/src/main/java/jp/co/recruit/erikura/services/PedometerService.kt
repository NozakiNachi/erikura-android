package jp.co.recruit.erikura.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import jp.co.recruit.erikura.R

class PedometerService : Service(), SensorEventListener {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = this.getString(R.string.default_notification_channel_id)

        val notification = NotificationCompat.Builder(this, channelId)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
        startForeground(100, notification)

        // 歩数センサーの初期化
        val sensorManager: SensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)?.let { stepDetectSensor ->
            sensorManager.registerListener(this, stepDetectSensor, SensorManager.SENSOR_DELAY_FASTEST)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let { stepCountSensor ->
            sensorManager.registerListener(this, stepCountSensor, SensorManager.SENSOR_DELAY_FASTEST)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Accuracy の変更時
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { event ->
            val sensor: Sensor = event.sensor
            val values: FloatArray = event.values
            val timestamp: Long = event.timestamp

            if (sensor.type == Sensor.TYPE_STEP_COUNTER) {
                values.forEach {
                    Log.v("SENSOR", String.format("VAL: %f", it))
                }
            }

        }
    }
}
