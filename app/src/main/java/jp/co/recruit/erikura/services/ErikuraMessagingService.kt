package jp.co.recruit.erikura.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.presenters.activities.StartActivity

class ErikuraMessagingService : FirebaseMessagingService() {
    companion object {
        fun createChannel(context: Context) {
            // Android 8.0 以降の場合のみチャンネル作成を行います
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            val channelId = context.getString(R.string.default_notification_channel_id)
            val channelName = context.getString(R.string.default_notification_channel_name)
            val channelDescription = context.getString(R.string.default_notification_channel_description)
            val groupId = "erikura_notification"
            val groupName = "エリクラ通知"

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val group = NotificationChannelGroup(groupId, groupName)
            notificationManager.createNotificationChannelGroup(group)

            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            channel.description = channelDescription
            channel.group = group.id
            channel.enableVibration(true)
            channel.enableLights(true)
            channel.setShowBadge(true)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 通知内のデータを取得しておきます
        val notificationData = NotificationData.fromJSON(remoteMessage.data["extra"])
        val openURI = notificationData?.openURI
        openURI?.also {
            // URL が指定されているので、URL をもとに開くための通知を行います
            val intent = Intent(Intent.ACTION_VIEW, it)
            notify(remoteMessage.notification?.title, remoteMessage.notification?.body, intent)
        } ?: run {
            // URL が指定されていないので、エリクラを起動するための通知を行います
            val intent = Intent(this, StartActivity::class.java)
            notify(remoteMessage.notification?.title, remoteMessage.notification?.body, intent)
        }
    }

    fun notify(title: CharSequence?, body: CharSequence?, intent: Intent) {
        val channelId = getString(R.string.default_notification_channel_id)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.erikura_icon_android_logo_1024)
            //.setLargeIcon(resources.getDrawable(R.mipmap.ic_launcher).toBitmap())
            .setColor(resources.getColor(R.color.orangeYellow, null))
            .setContentTitle(title)
            .setContentText(body)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Tracking.refreshFcmToken(token)
    }
}

data class NotificationData(val open: String? = null) {
    companion object {
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .serializeNulls()
            .create()

        fun fromJSON(jsonString: String?): NotificationData? {
            return jsonString?.let {
                gson.fromJson(it, NotificationData::class.java)
            }
        }
    }

    val openURI: Uri? get() {
        return open?.let {
            if (it.startsWith("/")) {
                return Uri.parse("erikura://${it}")
            }
            else {
                return Uri.parse(it)
            }
        }
    }
}