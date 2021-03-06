package jp.co.recruit.erikura

//import com.gu.toolargetool.TooLargeTool
import android.Manifest
import android.app.Activity
import android.app.ActivityManager
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
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.iid.FirebaseInstanceId
import io.karte.android.tracker.Tracker
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
import jp.co.recruit.erikura.presenters.util.LocationManager
import jp.co.recruit.erikura.presenters.util.PedometerManager
import jp.co.recruit.erikura.presenters.util.setOnSafeClickListener
import jp.co.recruit.erikura.services.ErikuraMessagingService
import org.apache.commons.lang.builder.ToStringBuilder
import org.json.JSONObject
import java.lang.RuntimeException
import java.math.BigDecimal
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

        val realm: Realm get() = RealmManager.realm

        // ???????????????????????????????????????
        const val REQUEST_ACCESS_FINE_LOCATION_PERMISSION_ID = 0x0001
        const val REQUEST_ACTIVITY_RECOGNITION_PERMISSION_ID = 0x0002
        const val REQUEST_EXTERNAL_STORAGE_PERMISSION_ID = 0x0003
        const val REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_ID = 0x0004

        // SMS???????????????????????????
        const val REQUEST_DEFAULT_CODE = 0
        const val REQUEST_SIGN_UP_CODE = 1
        const val REQUEST_LOGIN_CODE = 2
        const val REQUEST_CHANGE_USER_INFORMATION = 3

        // ????????????????????????????????????
        const val REQUEST_RESIGHIN = 4

        // ????????????????????????????????????????????????
        const val ERROR_MESSAGE_KEY = "errors"

        var versionAlertModal: AlertDialog? = null

        // ????????????????????????(0????????????)?????????
        const val ID_DISABLE_DISPLAY_CODE = 0
        const val ID_UNCONFIRMED_CODE = 1
        const val ID_CONFIRMING_CODE = 2
        const val ID_CONFIRMED_CODE = 3
        const val FAILED_NEVER_APPROVED = 4
        const val FAILED_ONCE_APPROVED= 5

        //????????????????????????????????????
        const val JOB_APPLY_BUTTON_REQUEST = 0

        // ?????????????????????????????????
        const val GET_COMPARING_DATA = true
        const val NOT_GET_COMPARING_DATA = false

        // ???????????????????????????
        const val FROM_WHERE = "from"
        const val FROM_NOT_FOUND = 0
        const val FROM_REGISTER = 1
        const val FROM_CHANGE_USER = 2
        const val FROM_CHANGE_USER_FOR_CHANGE_INFO = 3
        const val FROM_ENTRY = 4

        // ??????????????????????????????px
        const val ID_IMAGE_MAX_SIZE = 2592
        // ????????????????????????????????????quality
        const val ID_IMAGE_QUALITY = 90

        val WEEK_DAYS: Array<String> = arrayOf("???", "???", "???", "???", "???", "???", "???")
    }

    //    var userSession: UserSession? = null
    val erikuraComponent: ErikuraComponent = DaggerErikuraComponent.create()

    var currentJob: Job? = null
        set(value) {
            if (value == null) {
                Log.v("REPORTED JOB", ToStringBuilder.reflectionToString(value))
                // FirebaseCrashlytics ????????????null?????????????????????????????????????????????
                val e = Throwable("ErikuraApplication.currentJob : ${value.toString()}")
                FirebaseCrashlytics.getInstance().recordException(e)
            }
            else {
                try {
                    throw RuntimeException()
                }
                catch (e: RuntimeException) {
                    Tracking.logEvent(event = "current_job_assigned", params = bundleOf(
                        Pair("stackTrace", e.stackTrace.joinToString("\n")),
                        Pair("jobId", value.id),
                        Pair("entryId", value.entryId),
                        Pair("entryAt", value.entry?.createdAt),
                        Pair("workingStartAt", value.workingStartAt)
                    ))
                }
            }
            field = value
        }

    // ?????????????????????URL
    var pushUri: Uri? = null

    override fun onCreate() {
        super.onCreate()

        instance = this

//        TooLargeTool.startLogging(this);

        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycle())

        // ??????????????????????????????
        Tracking.initTrackers(this)
        // ??????????????????????????????
        ErikuraMessagingService.createChannel(this)

        UserSession.retrieve()?.let {
            Api.userSession = it
        }

        Api(this).let { api ->
            // ErikuraConfig ?????????????????????
            ErikuraConfig.load(this)

            api.clientVersion() { requiredVersion ->
                Log.v("VERSION", ToStringBuilder.reflectionToString(requiredVersion))
            }
        }
    }

    // ?????????????????????????????????????????????

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

    // ???????????????????????????????????????
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
    private val acceptedExplainGetPedometerKey = "AcceptedExplainGetPedometer"

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

    //??????????????????????????????????????????????????????
    fun isAcceptedExplainGetPedometer(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(acceptedExplainGetPedometerKey, false)
    }

    fun setAcceptedExplainGetPedometer(displayed: Boolean = true) {
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putBoolean(acceptedExplainGetPedometerKey, displayed)
            .apply()
    }

    fun checkVersion() {
        Api(this).clientVersion { requiredClientVersion ->
            // ????????????????????????????????????????????????????????????
            if (!requiredClientVersion.isMinimumSatisfied(versionName)) {
                //  ??????????????????????????????????????????????????? => ????????????????????????????????????????????????
                BaseActivity.currentActivity?.let { activity ->
                    val intent = Intent(activity, UpgradeRequiredActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    activity.startActivity(intent)
                }
            }

            // ?????????????????????????????????????????????????????????
            if (!requiredClientVersion.isCurrentSatisfied(versionName)) {
                // ???????????????????????????????????????????????? => ????????????????????????????????????????????????
                val ownerActivity = versionAlertModal?.ownerActivity
                if (ownerActivity != null && isEnableActivity(ownerActivity)) {
                    // modal???activity????????????????????????
                    versionAlertModal?.dismiss()
                    versionAlertModal = null
                }

                Log.v("DEBUG", BaseActivity.currentActivity.toString())
                BaseActivity.currentActivity?.let { activity ->
                    val dialog = AlertDialog.Builder(activity)
                        .setView(R.layout.dialog_update)
                        .setOnDismissListener {
                            ErikuraApplication.versionAlertModal = null
                        }
                        .create()
                    dialog.show()

                    versionAlertModal = dialog

                    val button: Button = dialog.findViewById(R.id.update_button)
                    button.setOnSafeClickListener {
                        val playURL = "https://play.google.com/store/apps/details?id=${packageName}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playURL))
                        activity.startActivity(intent)
                    }
                }
            }
        }
    }

    // pushUri???FDL???uri?????????????????????
    // ??????????????????????????????????????????????????????API??????????????????????????????????????????????????????????????????
    // ??????????????????pushUri??????FDL?????????????????????????????????API???????????????pushUri?????????????????????API???????????????pushUri??????????????????????????????????????????
    fun removePushUriFromFDL(intent: Intent, path: String){
        val appLinkData: Uri? = intent.data
        appLinkData?.path?.let{
            if (it.startsWith(path)) {
                // FDL????????????????????????pushUri??????????????????????????????
                instance.pushUri = null
            }
        }
    }

    // Activity??????????????????????????????
    fun isEnableActivity(activity: Activity): Boolean {
        return !(activity.isDestroyed)
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
    lateinit var application: Application
    lateinit var firebaseAnalytics: FirebaseAnalytics
    lateinit var appEventsLogger: AppEventsLogger
    var fcmToken: String? = null

    fun initTrackers(application: Application) {
        this.application = application
        firebaseAnalytics = FirebaseAnalytics.getInstance(application)

        // FCM??????????????????????????????
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

        // Crashlytics????????????
        // FIXME: Firebase ?????????????????????Crashlytics ?????????????????????
        // Fabric.with(application, Crashlytics())
        // ??????????????????????????????

        // Karte????????????????????????
        Tracker.init(application, BuildConfig.KARTE_APP_KEY)

        // Facebook
        FacebookSdk.setApplicationId(BuildConfig.FACEBOOK_APP_ID)
        FacebookSdk.sdkInitialize(application)
        AppEventsLogger.activateApp(application)
        appEventsLogger = AppEventsLogger.newLogger(application)

        // adjust ????????????
        val adjustConfig: AdjustConfig = AdjustConfig(application,
            BuildConfig.ADJUST_KEY,
            BuildConfig.ADJUST_ENVIRONMENT
        )
        Adjust.onCreate(adjustConfig)

        application.registerActivityLifecycleCallbacks(AdjustLifecycleCallbacks())
    }

    fun refreshFcmToken(token: String) {
        this.fcmToken = token
        // Karte ??? FCM ??????????????????????????????
        Tracker.getInstance().trackFcmToken(token)
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
            val birthday = user.parsedDateOfBirth
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

    fun viewCautions(name: String, title: String, jobId: Int, placeId: Int) {
        Log.v("ERIKURA", "Sending view tracking: ${name} (${title})")
        Tracker.getInstance().view(name, title, bundleOf(
            Pair("job_id", jobId),
            Pair("place_id", placeId)
        ))

    }

    fun viewGoodExamples(name: String, title: String, jobId: Int, jobKindId: Int, placeId: Int) {
        Log.v("ERIKURA", "Sending view tracking: ${name} (${title})")
        Tracker.getInstance().view(name, title, bundleOf(
            Pair("job_id", jobId),
            Pair("job_kind_id", jobKindId),
            Pair("place_id", placeId)
        ))

    }

    fun trackJobs(name: String, jobId: List<Int>) {
        Log.v("ERIKURA", "Sending view tracking: ${name}")
        Tracker.getInstance().view(name, bundleOf(
            Pair("job_id", jobId)
        ))
    }

    fun trackJobDetails(name: String, jobId: Int, steps: Int? = null) {
        Log.v("ERIKURA", "Sending view tracking: ${name}")
        val bundle = if (steps != null) {
            bundleOf(
                Pair("job_id", jobId),
                Pair("steps", steps)
            )
        } else {
            bundleOf(
                Pair("job_id", jobId)
            )
        }
        Tracker.getInstance().view(name, bundle)
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

    fun logSubmitApplicationFB() {
        try {
            appEventsLogger.logEvent(AppEventsConstants.EVENT_NAME_SUBMIT_APPLICATION)
        } catch (e: Exception) {
            Log.e("ERIKKURAR", "Facebook logSubmitApplication Failed", e)
        }
    }

    fun logPurchaseFB(fee: Int) {
        try {
            appEventsLogger.logPurchase(BigDecimal(fee), Currency.getInstance(Locale.JAPAN))
        } catch (e: Exception) {
            Log.e("ERIKKURAR", "Facebook logSubmitApplication Failed", e)
        }
    }

    fun smsVerify(name: String, user: User) {
        try {
            Log.v("ERIKURA", "Sending view tracking: ${name})")
            val values = bundleOf(
                Pair("user_id", user.id)
            )
            Tracker.getInstance().track(name, values)
        } catch (e: Exception) {
            Log.e("ERIKURA", "Karte identify error", e)
        }
    }

    fun skipSmsVerify(name: String, user: User) {
        try {
            Log.v("ERIKURA", "Sending view tracking: ${name})")
            val values = bundleOf(
                Pair("user_id", user.id)
            )
            Tracker.getInstance().track(name, values)
        } catch (e: Exception) {
            Log.e("ERIKURA", "Karte identify error", e)
        }
    }

    fun pushEntryThroughIdentityVerification(name: String, user_id: Int) {
        try {
            Log.v("ERIKURA", "Sending view tracking: ${name})")
            val values = bundleOf(
                Pair("user_id", user_id)
            )
            Tracker.getInstance().track(name, values)
        } catch (e: Exception) {
            Log.e("ERIKURA", "Karte identify error", e)
        }
    }

    fun trackUserId(name: String, user: User) {
        try {
            Log.v("ERIKURA", "Sending view tracking: ${name})")
            val values = bundleOf(
                Pair("user_id", user.id)
            )
            Tracker.getInstance().track(name, values)
        } catch (e: Exception) {
            Log.e("ERIKURA", "Karte identify error", e)
        }
    }

    fun trackWebReport(name: String, job_kind_id: Int, user_id: Int) {
        try {
            Log.v("ERIKURA", "Sending view tracking: ${name})")
            val values = bundleOf(
                Pair("job_kind_id", job_kind_id),
                Pair("user_id", user_id)
            )
            Tracker.getInstance().track(name, values)
        } catch (e: Exception) {
            Log.e("ERIKURA", "Karte identify error", e)
        }
    }
}

class AdjustLifecycleCallbacks() : Application.ActivityLifecycleCallbacks {

    override fun onActivityResumed(activity: Activity) {
        Adjust.onResume()
    }

    override fun onActivityPaused(activity: Activity) {
        Adjust.onPause()
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }
}

//class MemoryTraceException(klassName: String, memoryInfo: ActivityManager.MemoryInfo) : Exception("class=${klassName}, total=${memoryInfo.totalMem / 1024.0 / 1024.0}, avail=${memoryInfo.availMem / 1024.0 / 1024.0},threshold=${memoryInfo.threshold / 1024.0 / 1024.0},lowMemory=${memoryInfo.lowMemory}") {
//}