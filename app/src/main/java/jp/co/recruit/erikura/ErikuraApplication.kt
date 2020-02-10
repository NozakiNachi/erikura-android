package jp.co.recruit.erikura

import android.app.Application
import android.content.Context
import android.util.Log
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
}


