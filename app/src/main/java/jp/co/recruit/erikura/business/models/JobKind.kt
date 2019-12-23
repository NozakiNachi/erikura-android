package jp.co.recruit.erikura.business.models

data class JobKind(
    var id: Int,
    var name: String,
    var iconUrl: String?,
    var refine: Boolean = false,
    var summaryTitles: List<String> = listOf()
)