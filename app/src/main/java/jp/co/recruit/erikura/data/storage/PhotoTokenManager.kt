package jp.co.recruit.erikura.data.storage

import io.realm.Realm
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.business.models.Job


object PhotoTokenManager {
    val realm: Realm get() = ErikuraApplication.realm

    fun addToken(job: Job, url: String, token: String) {
        realm.executeTransaction { realm ->
            var photo = realm.createObject(PhotoToken::class.java, token)
            photo.url = url
            photo.jobId = job.id
        }
    }


    /**
     * URL に対応するトークンを取得します
     */
    fun getToken(job: Job, url: String): String? {
        var token: String? = null
        realm.executeTransaction { realm ->
            // トークンが Realm に登録されているか確認します
            var photo = realm.where(PhotoToken::class.java)
                .equalTo("url", url)
                .equalTo("jobId", job.id)
                .findFirst()
            token = photo?.token
        }
        return token
    }

    /**
     * 案件に対応するトークンデータをクリアします
     */
    fun clearToken(job: Job) {
        realm.executeTransaction { realm ->
            val tokens = realm.where(PhotoToken::class.java).equalTo("jobId", job.id).findAll()
            for(i in 1 .. tokens.count()) {
                tokens.get(i - 1)?.let {
                    it.deleteFromRealm()
                }
            }
        }
    }
}