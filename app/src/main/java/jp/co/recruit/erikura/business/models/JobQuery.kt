package jp.co.recruit.erikura.business.models

//var keyword: String?            // キーワード: 地図画面などでの表示用
//var latitude: Double?           // 緯度
//var longitude: Double?          // 軽度
//var minimumWorkingTime: Int?    // 作業時間下限
//var maximumWorkingTime: Int?    // 作業時間上限
//var minimumReward: Int?         // 報酬下限
//var maximumReword: Int?         // 報酬上限
//var jobKind: JobKind?           // タスク種別

enum class PeriodType(val value: String) {
    // 全ての案件
    ALL("全て"),
    // 募集中の案件
    ACTIVE("募集中")
}

enum class SortType(val value: String) {
    // 距離(近い順)
    DISTANCE_ASC("distance asc"),
    // 距離(遠い順)
    DISTANCE_DESC("distance desc"),
    // 報酬(安い順)
    FEE_ASC("jobs.fee asc"),
    // 報酬(高い順)
    FEE_DESC("jobs.fee desc"),
    // 作業時間(短い順)
    WORKING_TIME_ASC("jobs.working_time asc"),
    // 作業時間(長い順)
    WORKING_TIME_DESC("jobs.working_time desc"),
    // 納期までの時間(短い順)
    WORKING_FINISH_AT_ASC("jobs.working_finish_at asc"),
    // 納期までの時間(長い順)
    WORKING_FINSIH_AT_DESC("jobs.working_finish_at desc")
}

data class JobQuery(
    var keyword: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var minimumWorkingTime: Int? = null,
    var maximumWorkingTime: Int? = null,
    var minimumReward: Int? = null,
    var maximumReward: Int? = null,
    var jobKind: JobKind? = null,
    var period: PeriodType = PeriodType.ALL,
    var sortBy: SortType = SortType.DISTANCE_ASC
)