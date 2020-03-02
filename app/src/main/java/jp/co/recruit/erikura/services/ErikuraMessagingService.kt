package jp.co.recruit.erikura.services

import com.google.firebase.messaging.FirebaseMessagingService
import jp.co.recruit.erikura.Tracking

class ErikuraMessagingService : FirebaseMessagingService() {

//    override fun onBind(intent: Intent): IBinder {
//        TODO("Return the communication channel to the service.")
//    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Tracking.refreshFcmToken(token)
    }
}
/*
class MyFirebaseMessagingService: FirebaseMessagingService() {
    private val TAG = "MyFirebaseMessagingService"

    // FIXME: 実装を行う

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.v(TAG, "From: ${remoteMessage.from}")
        if (remoteMessage.data.isNotEmpty()) {
            Log.v(TAG, "Message data payload: ${remoteMessage.data}")

            scheduleJob()
        }

        remoteMessage.notification?.let {
            Log.v(TAG, "Message Notification Body: ${it.body}")
        }
    }

    fun scheduleJob() {
        // FIXME: メッセージに対する何らかの処理をする？
    }
}
*/