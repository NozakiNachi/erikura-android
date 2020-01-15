package jp.co.recruit.erikura.business.models

import android.util.Log
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.net.MalformedURLException
import java.net.URL

data class JobKind(
    var id: Int,
    var name: String,
    var iconUrl: String?,
    var refine: Boolean = false,
    var summaryTitles: List<String> = listOf()
) {
    val activeIconUrl: URL? get() {
        return iconUrl?.let {
            try {
                return URL(it)
            } catch (e: MalformedURLException) {
                Log.e("URL Parse error", e.message, e)
                return null
            }
        }
    }
    val inactiveIconUrl: URL? get() {
        return activeIconUrl?.let { url ->
            val extension = FilenameUtils.getExtension(url.path)
            val path = arrayOf(FilenameUtils.getBaseName(url.path) + "_inactive", extension).joinToString(".")
            return URL(activeIconUrl, arrayOf(path, url.query).filterNotNull().joinToString("?"))
        }
    }
}
