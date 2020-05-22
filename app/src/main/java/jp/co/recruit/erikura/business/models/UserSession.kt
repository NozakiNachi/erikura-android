package jp.co.recruit.erikura.business.models

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import jp.co.recruit.erikura.ErikuraApplication
import java.util.*

data class UserSession(
    val userId: Int,
    val token: String,
    val resignInExpiredAt: Date? = null,
    var user: User? = null,
    var smsVerifyCheck: Boolean = false
) {
    companion object {
        const val preferencesFilename = "erikuraPreferences"
        const val userIdKey = "jp.co.recruit.erikura.userId"
        const val tokenKey = "jp.co.recruit.erikura.token"

        val preferences: SharedPreferences get() =
            ErikuraApplication.instance.applicationContext.getSharedPreferences(
                preferencesFilename, Context.MODE_PRIVATE
            )

        /**
         * 永続化された UserSession の情報を取り出します
         */
        fun retrieve(): UserSession? {
            val userId: Int = preferences.getInt(userIdKey, -1)
            val token: String? = preferences.getString(tokenKey, null)

            if (userId > 0 && token != null) {
                return UserSession(userId, token)
            }
            return null
        }

        /**
         * 永続化されているセッション情報をクリアします
         */
        fun clear() {
            preferences.edit {
                remove(userIdKey)
                remove(tokenKey)
            }
        }
    }

    /**
     * UserSessionの情報を永続化します
     */
    fun store() {
        preferences.edit {
            putInt(userIdKey, this@UserSession.userId)
            putString(tokenKey, this@UserSession.token)
        }
    }
}
