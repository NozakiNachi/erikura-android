package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import jp.co.recruit.erikura.business.util.UrlUtils
import kotlinx.android.parcel.Parcelize
import org.apache.commons.io.FilenameUtils
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
        return iconUrl?.let { UrlUtils.parse(it) }
    }
    val inactiveIconUrl: URL? get() {
        return activeIconUrl?.let { url ->
            val extension = FilenameUtils.getExtension(url.path)
            val path = arrayOf(FilenameUtils.getBaseName(url.path) + "_inactive", extension).joinToString(".")
            URL(activeIconUrl, arrayOf(path, url.query).filterNotNull().joinToString("?"))
        }
    }

    override fun toString(): String {
        return name ?: super.toString()
    }
}
