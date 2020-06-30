package jp.co.recruit.erikura.business.models

import java.util.*

data class Caution(
    var question: String,
    var answer: String,
    var files: List<String> = listOf()
) {
}