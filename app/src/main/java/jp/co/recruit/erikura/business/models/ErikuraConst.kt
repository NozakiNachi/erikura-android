package jp.co.recruit.erikura.business.models

object ErikuraConst {
    const val maxCommentLength = 2000
    const val maxOutputSummaryCommentLength = 256
    const val maxOutputSummaries = 100
    val emailPattern = """\A[\w._%+-|]+@[\w0-9.-]+\.[A-Za-z]{2,}\z""".toRegex()
}