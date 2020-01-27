package jp.co.recruit.erikura.data.storage

import io.realm.Realm
import io.realm.RealmConfiguration
import jp.co.recruit.erikura.ErikuraApplication

class RealmManager {
    companion object {
        const val schemaVersion: Long = 1
        val instance = RealmManager()

        val realm: Realm get() = instance.realm
    }

    private val realm: Realm

    init {
        // 初期化処理
        Realm.init(ErikuraApplication.instance.applicationContext)

        val realmConfig = RealmConfiguration.Builder()
            .schemaVersion(schemaVersion)
            .migration { realm, oldVersion, newVersion ->
                if (oldVersion < 1) {

                }
            }
            .deleteRealmIfMigrationNeeded()
            .build()
        Realm.setDefaultConfiguration(realmConfig)
        realm = Realm.getDefaultInstance()
    }
}