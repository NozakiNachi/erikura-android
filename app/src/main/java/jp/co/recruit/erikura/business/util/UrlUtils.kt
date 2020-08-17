package jp.co.recruit.erikura.business.util

import android.net.Uri
import android.util.Log
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import java.net.MalformedURLException
import java.net.URL

object UrlUtils {
    fun parse(urlString: String): URL? {
        try {
            return URL(urlString)
        }
        catch (e: MalformedURLException) {
            return if (urlString.startsWith("/")) {
                var pathUri = Uri.parse(urlString)
                var baseUri = Uri.parse(BuildConfig.SERVER_BASE_URL)
                val builder = Uri.Builder()
                    .scheme(baseUri.scheme)
                    .encodedAuthority(baseUri.authority)
                    .appendEncodedPath(pathUri.path)
                Log.d(ErikuraApplication.LOG_TAG, "Adjusted URL: ${builder.build().toString()}")
                val newUrlString = builder.build().toString()
                try {
                    URL(newUrlString)
                }
                catch (e: MalformedURLException) {
                    Log.e("URL Parse Error", e.message, e)
                    null
                }
            }
            else {
                Log.e("URL Parse Error", e.message, e)
                null
            }
        }
    }
}