package jp.co.recruit.erikura

import android.app.Application
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.di.DaggerErikuraComponent
import jp.co.recruit.erikura.di.ErikuraComponent

class ErikuraApplication : Application() {
    companion object {
//        lateinit var context: Context private set
        lateinit var instance: ErikuraApplication private set
    }

    //    var userSession: UserSession? = null
    val erikuraComponent: ErikuraComponent = DaggerErikuraComponent.create()

    override fun onCreate() {
        super.onCreate()
        instance = this
//        context = applicationContext

        UserSession.retrieve()?.let {
            Api.userSession = it
        }
    }
}
