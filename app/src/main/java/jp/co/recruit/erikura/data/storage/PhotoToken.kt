package jp.co.recruit.erikura.data.storage

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required

open class PhotoToken : RealmObject() {
    @PrimaryKey
    @Required
    open lateinit var url: String
    open var jobId: Int = 0
    open lateinit var token: String
}