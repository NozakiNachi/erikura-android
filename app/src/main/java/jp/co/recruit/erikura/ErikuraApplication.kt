package jp.co.recruit.erikura

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import io.realm.Realm
import io.realm.RealmConfiguration
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.storage.AssetsManager
import jp.co.recruit.erikura.data.storage.RealmManager
import jp.co.recruit.erikura.di.DaggerErikuraComponent
import jp.co.recruit.erikura.di.ErikuraComponent
import jp.co.recruit.erikura.presenters.util.GoogleFitApiManager
import jp.co.recruit.erikura.presenters.util.LocationManager

class ErikuraApplication : Application() {


    companion object {
        lateinit var instance: ErikuraApplication private set

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

        UserSession.retrieve()?.let {
            Api.userSession = it
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
}


