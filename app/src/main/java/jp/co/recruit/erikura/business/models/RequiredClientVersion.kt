package jp.co.recruit.erikura.business.models

data class RequiredClientVersion (
    var current_version: String,
    var minimum_version: String
) {

    fun isCurrentSatisfied(version: String): Boolean {
        val versionPattern = Regex(pattern = "^\\d+(\\.\\d+)*$")
        return if (versionPattern.matches(version)) {
            satisfied(parse(version), parse(current_version))
        }
        else {
            // パースに失敗する場合は、満たしたと判断する
            true
        }
    }

    fun isMinimumSatisfied(version: String): Boolean {
        val versionPattern = Regex(pattern = "^\\d+(\\.\\d+)*$")
        return if (versionPattern.matches(version)) {
            satisfied(parse(version), parse(minimum_version))
        }
        else {
            // パースに失敗する場合は、満たしたと判断する
            true
        }
    }

    private fun parse(version: String): IntArray {
        return version.split(".").map { it.toInt() }.toIntArray()
    }

    private fun satisfied(version: IntArray, required: IntArray): Boolean {
        var result = true
        run {
            version.zip(required).forEach { pair ->
                val ver = pair.first
                val req = pair.second
                if (ver > req) {
                    result = true
                    return@run
                }
                if (ver < req) {
                    result = false
                    return@run
                }
            }
        }
        return result
    }
}
