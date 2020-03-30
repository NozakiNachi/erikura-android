package jp.co.recruit.erikura.business.models

data class RequiredClientVersion (
    var current_version: String,
    var minimum_version: String
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
/*
    func clientVersion(completion: @escaping (RequiredClientVersion?) -> Void) {
        let params: Parameters = [:]
        request(action: "/utils/client_version", method: .get, params: params) { json in
            if (json["errors"].count > 0) {
                self.displayErrorAlert(messages: self.getErrors(json: json))
            } else {
                let version = RequiredClientVersion(json: json["body"])
                completion(version)
            }
        }
    }

class RequiredClientVersion {
    var current: String?
    var minimum: String?

    init?(json: JSON) {
        guard !(json.object is NSNull) else { return nil }

        current = json["current_version"].string
        minimum = json["minimum_version"].string
    }

    func isCurrentSatisfied(version: String) -> Bool {
        let pattern = "^\\d+(\\.\\d+)*$"
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return true }
        let matches = regex.matches(in: version, range: NSRange(location: 0, length: version.count))
        if matches.count > 0 {
            return satisfied(version: parse(version), required: parse(current))
        }
        else {
            return true
        }
    }

    func isMinimumSatisfied(version: String) -> Bool {
        let pattern = "^\\d+(\\.\\d+)*$"
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return true }
        let matches = regex.matches(in: version, range: NSRange(location: 0, length: version.count))
        if matches.count > 0 {
            return satisfied(version: parse(version), required: parse(minimum))
        }
        else {
            return true
        }
    }

    // バージョンを数値の配列に変換します
    private func parse(_ version: String?) -> [Int]? {
        guard let version = version else { return nil }
        let components = version.components(separatedBy: ".").map { Int($0) ?? 0 }
        return components
    }

    private func satisfied(version: [Int]?, required: [Int]?) -> Bool {
        guard let required = required else { return true }  // 要求バージョンがないので、常に満たしている想定
        guard let version = version else { return false }   // バイナリのバージョンがないので、常に満たしていないと想定

        for (ver, req) in zip(version, required) {
            if ver > req { return true  }   // 現在バージョンが req よりも大きい場合は、満たしていると考える
            if ver < req { return false }   // 小さい場合は満たしていないと考える
        }
        // 上記チェックを抜けた場合
        if version.count > required.count { return true }   // 現在バージョン側によりマイナーなバージョン情報がついている => 満たしていると判定
        if version.count < required.count { return false }  // 要求バージョン側によりマイナーなバージョン情報がついている => 満たしていないと判定
        // 全く同じバージョンなので満たしている
        return true
    }
}

 */