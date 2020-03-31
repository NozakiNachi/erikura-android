package jp.co.recruit.erikura

import android.Manifest
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.iid.FirebaseInstanceId
import io.fabric.sdk.android.Fabric
import io.karte.android.tracker.Tracker
import io.karte.android.tracker.TrackerConfig
import io.realm.Realm
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.storage.AssetsManager
import jp.co.recruit.erikura.data.storage.RealmManager
import jp.co.recruit.erikura.di.DaggerErikuraComponent
import jp.co.recruit.erikura.di.ErikuraComponent
import jp.co.recruit.erikura.presenters.util.GoogleFitApiManager
import jp.co.recruit.erikura.presenters.util.LocationManager
import jp.co.recruit.erikura.services.ErikuraMessagingService
import org.json.JSONObject
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import jp.co.recruit.erikura.business.models.ErikuraConfig
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.errors.UpgradeRequiredActivity
import org.apache.commons.lang.builder.ToStringBuilder


class ErikuraApplication : Application() {
    companion object {
        lateinit var instance: ErikuraApplication private set

        val versionCode : Int = BuildConfig.VERSION_CODE
        val versionName : String = BuildConfig.VERSION_NAME

        val applicationContext: Context get() = instance.applicationContext
        val assetsManager: AssetsManager get() = instance.erikuraComponent.assetsManager()
        val locationManager: LocationManager get() = instance.erikuraComponent.locationManger()
        val fitApiManager: GoogleFitApiManager get() = instance.erikuraComponent.googleFitApiManager()
        val realm: Realm get() = RealmManager.realm
    }

    //    var userSession: UserSession? = null
    val erikuraComponent: ErikuraComponent = DaggerErikuraComponent.create()

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
    val REQUEST_PERMISSION = 2
    val REQUEST_CODE_CHOOSE = 1

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
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_PERMISSION)
    }

    // 画像アップロード終了判定用
    var uploadMonitor = Object()

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
    }
}

object Tracking {
    private val TAG = Tracking::class.java.name
    lateinit var firebaseAnalytics: FirebaseAnalytics
    private var fcmToken: String? = null

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

        // FIXME: adjust の初期化
    }

    fun refreshFcmToken(token: String) {
        this.fcmToken = token
        if (Api.isLogin) {
            Api(ErikuraApplication.applicationContext).pushEndpoint(token) {
                Log.v("Erikura", "push_endpoint: result=${it}, token=${token}, userId=${Api.userSession?.userId ?: ""}")
            }
        }
    }

    fun identify(userId: Int) {
        Log.v(TAG, "Sending user identity: $userId")

        firebaseAnalytics.setUserId(userId.toString())
        Tracker.getInstance().identify(JSONObject(mapOf(Pair("user_id", userId))))
    }

    /*
    class func identify(user: User, status: String){
        do {
            let userId = user.id ?? API.sharedInstance.userSession?.userId
            let name = (user.lastName ?? "") + (user.firstName ?? "")
            let component = Calendar.current.dateComponents(in: TimeZone.current, from: user.dateOfBirth!)
            let birthYear = component.year
            let birthDate = component.date
            let now = Calendar.current.dateComponents(in: TimeZone.current, from: Date())
            let age = now.year! - birthYear!
            let address = (user.prefecture ?? "") + (user.city ?? "")
            var gender: String = ""
            if user.gender == .male {
                gender = "m"
            }else if user.gender == .female {
                gender = "f"
            }
            let jobStatus = user.jobStatus ?? ""

            Analytics.setUserID(userId?.description)

            let values: [AnyHashable : Any] = [
                "user_id": userId ?? 0,
                "name" : userId?.string ?? "0",
                "create_date" : Date(),
                "age" : age,
                "gender" : gender,
                "birth_year" : birthYear ?? 0,
                "birth_date" : birthDate ?? Date(),
                "address" : address,
                "job" : jobStatus,
                "login_status" : status
            ]

            KarteTracker.shared.identify(values)
        }
        catch {
            print("KARTE identify Error!")
        }
    }

    class func view(name: String, title: String) {
        print("Sending view tracking: " + name + "(" + title + ")")
        KarteTracker.shared.view(name, title: title)
    }

    class func view(name: String) {
        print("Sending view tracking: " + name )
        KarteTracker.shared.view(name)
    }

    class func viewJobs(name: String, title: String, jobId: [Int]) {
        print("Sending view tracking: " + name + "(" + title + ")")
        KarteTracker.shared.view(name, title: title, values: ["job_id": jobId])
    }

    class func viewJobDetails(name: String, title: String, jobId: Int) {
        print("Sending view tracking: " + name + "(" + title + ")")
        KarteTracker.shared.view(name, title: title, values: ["job_id": jobId])
    }

    class func viewPlaceDetails(name: String, title: String, placeId: Int) {
        print("Sending view tracking: " + name + "(" + title + ")")
        KarteTracker.shared.view(name, title: title, values: ["place_id": placeId])
    }

    class func trackJobs(name: String, jobId: [Int]) {
        print("Sending view tracking: " + name)
        KarteTracker.shared.track(name, values: ["job_id": jobId])
    }

    class func trackJobDetails(name: String, jobId: Int) {
        print("Sending view tracking: " + name)
        KarteTracker.shared.track(name, values: ["job_id": jobId])
    }

    class func currentLocation(name: String, latitude: Double, longitude: Double){
        print("Sending view tracking: " + name)
        KarteTracker.shared.track(name, values: ["latlng": [longitude, latitude]])
    }

    class func jobEntry(name: String, title: String, job: Job) {
        do {
            print("Sending view tracking: " + name + "(" + title + ")")
            let jobKindId = job.jobKind!.id
            let jobKindName = job.jobKind!.name
            let jobId = job.id
            let jobName = job.title
            let workingPlace = job.workingPlace
            let workingStartAt: Date = job.workingStartAt!
            let workingFinishAt: Date = job.workingFinishAt!

            let values: [AnyHashable : Any] = [
                "job_kind_id": jobKindId ?? 0,
                "job_kind_name": jobKindName ?? "",
                "job_id": jobId ?? 0,
                "job_name": jobName ?? "",
                "working_place": workingPlace ?? "",
                "working_starts_at": workingStartAt ,
                "working_finish_at": workingFinishAt
            ]

            KarteTracker.shared.track(name, values: values)
//            KarteTracker.shared.view(name, title: title, values: values)
        }catch {
            print("KARTE jobEntry Error!")
        }
    }

    class func track(name: String){
        do {
            print("Sending view tracking: " + name)

            KarteTracker.shared.track(name, values: [:])

        }catch {
            print("KARTE jobEntry Error!")
        }
    }

    class func accpetGeoSetting(){
        do {
            KarteTracker.shared.track("accpet_geo_setting", values: [:])
        }catch {
            print("KARTE accpet_geo_setting Error!")
        }
    }

    class func accpetPushNotification(){
        do {
            KarteTracker.shared.track("accpet_push_notification", values: [:])
        }catch {
            print("KARTE accpet_push_notification Error!")
        }
    }

    class func logEvent(event: String, params: [String: String]){
        do {
            Analytics.logEvent(event, parameters: params)
        }catch {
            print("FireBase Error!")
        }
    }

    class func logCompleteRegistrationEvent() {
        do {
            FBSDKAppEvents.logEvent(FBSDKAppEventNameCompletedRegistration)
        }catch {

        }
    }

    class func logEventFB(event: String){
        do {
            FBSDKAppEvents.logEvent(event)
        }catch {

        }
    }
     */
}


