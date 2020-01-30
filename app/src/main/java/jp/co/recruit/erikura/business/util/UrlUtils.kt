package jp.co.recruit.erikura.business.util

import android.util.Log
import jp.co.recruit.erikura.BuildConfig
import java.net.MalformedURLException
import java.net.URL

object UrlUtils {
    fun parse(urlString: String): URL? {
        try {
            return URL(urlString)
        }
        catch (e: MalformedURLException) {
            return if (urlString.startsWith("/")) {
                val newUrlString = BuildConfig.SERVER_BASE_URL + urlString
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