package jp.co.recruit.erikura.business.models

import java.util.*

data class UserSession(
    val userId: Int,
    val token: String,
    val resignInExpiredAt: Date? = null
)
