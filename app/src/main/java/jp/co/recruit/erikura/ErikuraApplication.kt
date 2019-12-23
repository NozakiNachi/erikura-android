package jp.co.recruit.erikura

import android.app.Application

class ErikuraApplication : Application() {
    companion object {
        lateinit var instance: ErikuraApplication private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}

//class ErikuraApplication: Application() {
//    companion object {
//        lateinit var instance: ErikuraApplication private set
//        lateinit var context: Context private set
//    }
//
//    var userSession: UserSession? = null
//    val erikuraComponent: ErikuraComponent = DaggerErikuraComponent.create()
//
//    override fun onCreate() {
//        super.onCreate()
//        instance = this
//        context = applicationContext
//    }
//}
