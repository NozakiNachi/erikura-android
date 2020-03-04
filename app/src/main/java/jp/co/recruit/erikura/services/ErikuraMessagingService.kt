package jp.co.recruit.erikura.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.presenters.activities.StartActivity

class ErikuraMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        /*
        if (remoteMessage.data.isNotEmpty()) {
            Log.v(TAG, "Message data payload: ${remoteMessage.data}")

            scheduleJob()
        }

        remoteMessage.notification?.let {
            Log.v(TAG, "Message Notification Body: ${it.body}")
        }
         */

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(ErikuraApplication.instance, remoteMessage.notification?.body ?: "通知がありました", Toast.LENGTH_LONG).show()
        }

        // FIXME: data["extra"] で JSON データが取得できそう

        // FIXME: URL をパースする必要があるのか？
        val intent = Intent(this, StartActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val channelId = "Default"
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(remoteMessage.notification?.title)
            .setContentText(remoteMessage.notification?.body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Default channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0, builder.build())
    }



    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Tracking.refreshFcmToken(token)
    }
}