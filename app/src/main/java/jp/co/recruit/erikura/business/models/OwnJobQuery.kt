package jp.co.recruit.erikura.business.models

import java.util.*

//enum Status {
//    case entried        // 応募済み、未実施
//    case started        // 開始済み、未完了
//    case finished       // 完了済み、未報告
//    case reported       // 報告済み
//}
//
//var status: Status?
//var reportedAtFrom: Date?
//var reportedAtTo: Date?

data class OwnJobQuery(
    val status: Status,
    val reportedFrom: Date? = null,
    val reportedTo: Date? = null
) {
    enum class Status(val code: String) {
        ENTRIED("entried"),     // 応募済み、未実施
        STARTED("started"),     // 開始済み、未完了
        FINISHED("finished"),   // 完了済み、未報告
        REPORTED("reported")    // 報告済み
    }
}
