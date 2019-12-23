package jp.co.recruit.erikura.business.models

data class RequiredClientVersion (
    var current: String,
    var minimum: String
) {

    fun isCurrentSatisfied(version: String): Boolean {
        // FIXME: 実装
        return true
    }

    fun isMinimumSatisfied(version: String): Boolean {
        // FIXME: 実装
        return true
    }

    private fun parse(version: String): Array<Int> {
        // FIXME: 実装
        return arrayOf(0, 0, 0, 0)
    }

    private fun satisfied(version: Array<Int>, required: Array<Int>): Boolean {
        // FIXME: 実装
        return true
    }
}