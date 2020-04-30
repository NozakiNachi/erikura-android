package jp.co.recruit.erikura

import android.Manifest
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.iid.FirebaseInstanceId
import io.fabric.sdk.android.Fabric
import io.karte.android.tracker.Tracker
import io.karte.android.tracker.TrackerConfig
import io.realm.Realm
import jp.co.recruit.erikura.business.models.*
import jp.co.recruit.erikura.business.util.DateUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.storage.AssetsManager
import jp.co.recruit.erikura.data.storage.RealmManager
import jp.co.recruit.erikura.di.DaggerErikuraComponent
import jp.co.recruit.erikura.di.ErikuraComponent
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.errors.UpgradeRequiredActivity
import jp.co.recruit.erikura.presenters.util.GoogleFitApiManager
import jp.co.recruit.erikura.presenters.util.LocationManager
import jp.co.recruit.erikura.presenters.util.PedometerManager
import jp.co.recruit.erikura.services.ErikuraMessagingService
import org.apache.commons.lang.builder.ToStringBuilder
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ErikuraApplication : Application() {
    companion object {
        const val LOG_TAG = "ERIKURA"

        lateinit var instance: ErikuraApplication private set

        val versionCode : Int = BuildConfig.VERSION_CODE
        val versionName : String = BuildConfig.VERSION_NAME

        val applicationContext: Context get() = instance.applicationContext
        val assetsManager: AssetsManager get() = instance.erikuraComponent.assetsManager()
        val locationManager: LocationManager get() = instance.erikuraComponent.locationManger()
        val pedometerManager: PedometerManager get() = instance.erikuraComponent.pedometerManager()
        val fitApiManager: GoogleFitApiManager get() = instance.erikuraComponent.googleFitApiManager()

        val realm: Realm get() = RealmManager.realm

        // バーミッション取得用の定数
        const val REQUEST_ACCESS_FINE_LOCATION_PERMISSION_ID = 0x0001
        const val REQUEST_ACTIVITY_RECOGNITION_PERMISSION_ID = 0x0002
        const val REQUEST_EXTERNAL_STORAGE_PERMISSION_ID = 0x0003
    }

    //    var userSession: UserSession? = null
    val erikuraComponent: ErikuraComponent = DaggerErikuraComponent.create()

    var reportingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycle())

        // トラッキングの初期化
        Tracking.initTrackers(this)
        // 通知チャネルの初期化
        ErikuraMessagingService.createChannel(this)

        UserSession.retrieve()?.let {
            Api.userSession = it
        }

        Api(this).let { api ->
            // ErikuraConfig を読み込みます
            ErikuraConfig.load(this)

            api.clientVersion() { requiredVersion ->
                Log.v("VERSION", ToStringBuilder.reflectionToString(requiredVersion))
            }
        }

    }

    // ギャラリーへのアクセス許可関連

    fun hasStoragePermission(activity: FragmentActivity): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return permissions.all { ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }
    }

    fun requestStoragePermission(activity: FragmentActivity) {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_EXTERNAL_STORAGE_PERMISSION_ID)
    }

    // 画像アップロード終了判定用
    var uploadMonitor = Object()

    fun notifyUpload() {
        synchronized(uploadMonitor) {
            uploadMonitor.notifyAll()
        }
    }

    fun waitUpload() {
        synchronized(uploadMonitor) {
            uploadMonitor.wait(15000)
        }
    }


    private val onboardingDisplayedKey = "OnboardingDisplayed"
    private val coachMarkDisplayedKey = "CoachMarkDisplayed"

    fun isOnboardingDisplayed(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(onboardingDisplayedKey, false)
    }

    fun setOnboardingDisplayed(displayed: Boolean = true) {
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putBoolean(onboardingDisplayedKey, displayed)
            .apply()
    }

    fun isCoachMarkDisplayed(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(coachMarkDisplayedKey, false)
    }

    fun setCoachMarkDisplayed(displayed: Boolean = true) {
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putBoolean(coachMarkDisplayedKey, displayed)
            .apply()
    }

    fun checkVersion() {
        Api(this).clientVersion { requiredClientVersion ->
            // 最低バージョンを満たしているか確認します
            if (!requiredClientVersion.isMinimumSatisfied(versionName)) {
                //  最低バージョンを満たしていない場合 => バージョンアップ画面を表示します
                BaseActivity.currentActivity?.let { activity ->
                    val intent = Intent(activity, UpgradeRequiredActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    activity.startActivity(intent)
                }
            }

            // 最新バージョンになっているか確認します
            if (!requiredClientVersion.isCurrentSatisfied(versionName)) {
                // 最新バージョンになっていない場合 => アップデートを促すモーダルを表示
                Log.v("DEBUG", BaseActivity.currentActivity.toString())
                BaseActivity.currentActivity?.let { activity ->
                    val dialog = AlertDialog.Builder(activity)
                        .setView(R.layout.dialog_update)
                        .create()
                    dialog.show()

                    val button: Button = dialog.findViewById(R.id.update_button)
                    button.setOnClickListener {
                        val playURL = "http://play.google.com/store/apps/details?id=${packageName}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playURL))
                        activity.startActivity(intent)
                    }
                }
            }
        }
    }
}

class AppLifecycle: LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        ErikuraApplication.instance.checkVersion()

        ErikuraApplication.locationManager.latLng?.let {
            Tracking.currentLocation("current_location", it.latitude, it.longitude)
        }
    }
}

object Tracking {
    private val TAG = Tracking::class.java.name
    lateinit var firebaseAnalytics: FirebaseAnalytics
    lateinit var appEventsLogger: AppEventsLogger
    var fcmToken: String? = null

    fun initTrackers(application: Application) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(application)

        // FCMトークンを取得します
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("erikura", "getInstanceId failed", task.exception)
            }
            else {
                task.result?.token?.let { token ->
                    refreshFcmToken(token)
                }
            }
        }

        // Crashlyticsの初期化
        // FIXME: Firebase コンソールで、Crashlytics の設定が必要？
        Fabric.with(application, Crashlytics())

        // Karteの初期を行います
        val config = TrackerConfig.Builder()
            // FIXME: iOS版であった isEnabledVisualTracking がないようなので、設定保留
            .build()
        Tracker.init(application, BuildConfig.KARTE_APP_KEY, config)

        // Facebook
        FacebookSdk.setApplicationId(BuildConfig.FACEBOOK_APP_ID)
        FacebookSdk.sdkInitialize(application)
        AppEventsLogger.activateApp(application)
        appEventsLogger = AppEventsLogger.newLogger(application)

        // FIXME: adjust の初期化
    }

    fun refreshFcmToken(token: String) {
        this.fcmToken = token
        if (Api.isLogin) {
            Api(ErikuraApplication.applicationContext).pushEndpoint(token) {
                Log.v(ErikuraApplication.LOG_TAG, "push_endpoint: result=${it}, token=${token}, userId=${Api.userSession?.userId ?: ""}")
            }
        }
    }

    fun identify(userId: Int) {
        Log.v(TAG, "Sending user identity: $userId")

        firebaseAnalytics.setUserId(userId.toString())
        Tracker.getInstance().identify(JSONObject(mapOf(Pair("user_id", userId))))
    }

    fun logEvent(event: String, params: Bundle?) {
        try {
            firebaseAnalytics.logEvent(event, params)
        }
        catch (e: Exception) {
            Log.e("ERIKURA", "Firebase error", e)
        }
    }

    fun identify(user: User, status: String) {
        try {
            val birthday = SimpleDateFormat("yyyy/MM/dd").parse(user.dateOfBirth)
            val now = Date()
            val gender = when (user.gender) {
                Gender.MALE -> "m"
                Gender.FEMALE -> "f"
                else -> ""
            }

            firebaseAnalytics.setUserId(user.id.toString())

            val values = bundleOf(
                Pair("user_id", user.id),
                Pair("name", user.id.toString()),
                Pair("create_date", Date()),
                Pair("age", DateUtils.diffYears(now, birthday)),
                Pair("gender", gender),
                Pair("birth_year", DateUtils.calendarFrom(birthday).get(Calendar.YEAR)),
                Pair("birth_date", birthday),
                Pair("address", (user.prefecture ?: "") + (user.city ?: "")),
                Pair("job", user.jobStatus ?: ""),
                Pair("login_status", status)
            )
            Tracker.getInstance().identify(values)
        } catch (e: Exception) {
            Log.e("ERIKURA", "Karte identify error", e)
        }
    }

    fun view(name: String) {
        Log.v("ERIKURA", "Sending view tracking: ${name}")
        Tracker.getInstance().view(name, bundleOf())
    }

    fun view(name: String, title: String) {
        Log.v("ERIKURA", "Sending view tracking: ${name} (${title})")
        Tracker.getInstance().view(name, title)
    }

    fun viewJobs(name: String, title: String, jobId: List<Int>) {
        Log.v("ERIKURA", "Sending view tracking: ${name} (${title})")
        Tracker.getInstance().view(name, title, bundleOf(
            Pair("job_id", jobId)
        ))
    }

    fun viewJobDetails(name: String, title: String, jobId: Int) {
        Log.v("ERIKURA", "Sending view tracking: ${name} (${title})")
        Tracker.getInstance().view(name, title, bundleOf(
            Pair("job_id", jobId)
        ))
    }

    fun viewPlaceDetails(name: String, title: String, placeId: Int) {
        Log.v("ERIKURA", "Sending view tracking: ${name} (${title})")
        Tracker.getInstance().view(name, title, bundleOf(
            Pair("place_id", placeId)
        ))
    }

    fun trackJobs(name: String, jobId: List<Int>) {
        Log.v("ERIKURA", "Sending view tracking: ${name}")
        Tracker.getInstance().view(name, bundleOf(
            Pair("job_id", jobId)
        ))
    }

    fun trackJobDetails(name: String, jobId: Int) {
        Log.v("ERIKURA", "Sending view tracking: ${name}")
        Tracker.getInstance().view(name, bundleOf(
            Pair("job_id", jobId)
        ))
    }

    fun currentLocation(name: String, latitude: Double, longitude: Double) {
        Log.v("ERIKURA", "Sending view tracking: ${name}")
        Tracker.getInstance().view(name, bundleOf(
            Pair("latlng", arrayOf(latitude, longitude))
        ))
    }

    fun jobEntry(name: String, title: String, job: Job) {
        try {
            Log.v("ERIKURA", "Sending view tracking: ${name} (${title})")
            val values = bundleOf(
                Pair("job_kind_id", job.jobKind?.id),
                Pair("job_kind_name", job.jobKind?.name),
                Pair("job_id", job.id),
                Pair("job_name", job.title),
                Pair("working_place", job.workingPlace),
                Pair("working_start_at", job.workingStartAt),
                Pair("working_finish_at", job.workingFinishAt)
            )
            Tracker.getInstance().track(name, values)
        } catch (e: Exception) {
            Log.e("ERIKURA", "Karte identify error", e)
        }
    }

    fun track(name: String) {
        try {
            Log.v("ERIKURA", "Sending view tracking: ${name}")
            Tracker.getInstance().track(name, bundleOf())
        } catch (e: Exception) {
            Log.e("ERIKURA", "Karte identify error", e)
        }
    }

    fun acceptGeoSetting() {
        try {
            Tracker.getInstance().track("accpet_geo_setting", bundleOf())
        } catch (e: Exception) {
            Log.e("ERIKURA", "Karte identify error", e)
        }
    }

    /*
    class func accpetPushNotification(){
        do {
            KarteTracker.shared.track("accpet_push_notification", values: [:])
        }catch {
            print("KARTE accpet_push_notification Error!")
        }
    }
     */

    fun logCompleteRegistrationEvent() {
        try {
            appEventsLogger.logEvent(AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION)
        } catch (e: Exception) {
            Log.e("ERIKURA", "Facebook LogEvent Failed", e)
        }
    }

    fun logEventFB(event: String) {
        try {
            appEventsLogger.logEvent(event)
        } catch (e: Exception) {
            Log.e("ERIKURA", "Facebook LogEvent Failed", e)
        }
    }
}


