package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import android.util.Log
import kotlinx.android.parcel.Parcelize
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.net.MalformedURLException
import java.net.URL

@Parcelize
data class JobKind(
    var id: Int = 0,
    var name: String? = null,
    var iconUrl: String? = null,
    var refine: Boolean = false,
    var summaryTitles: List<String> = listOf()
): Parcelable {
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
