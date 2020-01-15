package jp.co.recruit.erikura.data.storage

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import java.util.*

open class Asset : RealmObject() {
    enum class AssetType {
        Pdf,
        Marker,
        Other
    }

    @PrimaryKey
    @Required
    open lateinit var url: String
    open lateinit var path: String
    open lateinit var lastAccessedAt: Date
    open lateinit var assetType: String

    var type: AssetType
        get() = AssetType.values().first { it.name == assetType }
        set(value) {
            assetType = value.name
        }
}

