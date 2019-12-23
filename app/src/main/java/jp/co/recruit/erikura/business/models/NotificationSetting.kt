package jp.co.recruit.erikura.business.models

data class NotificationSetting(
    var allowRemindMailReception: Boolean = false,
    var allowInfoMailReception: Boolean = false,
    var allowReopenMailReception: Boolean = false,
    var allowLikedMailReception: Boolean = false,
    var allowCommentedMailReception: Boolean = false,
    var allowRemindPushReception: Boolean = false,
    var allowInfoPushReception: Boolean = false,
    var allowReopenPushReception: Boolean = false,
    var allowLikedPushReception: Boolean = false,
    var allowCommentedPushReception: Boolean = false
)