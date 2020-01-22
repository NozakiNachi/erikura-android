package jp.co.recruit.erikura.data.storage

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import java.util.*

open class SearchHistory : RealmObject() {
    @PrimaryKey
    @Required
    open lateinit var keyword: String
    open lateinit var lastUpdatedAt: Date
}
