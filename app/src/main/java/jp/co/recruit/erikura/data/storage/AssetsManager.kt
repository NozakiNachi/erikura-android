package jp.co.recruit.erikura.data.storage

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import io.realm.Realm
import io.realm.Sort
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.data.network.Api
import java.io.File
import java.net.URL
import java.util.*
import javax.inject.Singleton
import kotlin.collections.HashMap

typealias CompletionCallback = (Asset) -> Unit

@Singleton
class AssetsManager {
    companion object {
        val maxAssets: Map<Asset.AssetType, Int> =
            mapOf(
                Pair(Asset.AssetType.Marker, 50000),
                Pair(Asset.AssetType.Pdf, 10),
                Pair(Asset.AssetType.Other, 200)
            )

        fun create(): AssetsManager {
            return AssetsManager()
        }
    }

    private val lock = java.lang.Object()
    private val completionCallbackMap: MutableMap<String, MutableList<CompletionCallback>> = HashMap()
    val realm: Realm = ErikuraApplication.realm

    fun fetchAsset(activity: Activity, url: String, type: Asset.AssetType = Asset.AssetType.Other, onComplete: (asset: Asset) -> Unit) {
        lookupAsset(url)?.also { asset ->
            val file = File(asset.path)
            if (file.exists()) {
                onComplete(asset)
            }
            else {
                downloadAsset(activity, url, type) { asset ->
                    onComplete(asset)
                }
            }
        } ?: run {
            downloadAsset(activity, url, type) { asset ->
                onComplete(asset)
            }
        }
    }

    fun fetchImage(activity: Activity, url: String, type: Asset.AssetType = Asset.AssetType.Other, onComplete: (image: Bitmap) -> Unit) {
        fetchAsset(activity, url, type) { asset ->
            try {
                BitmapFactory.decodeFile(asset.path)?.let {
                    onComplete(it)
                }
            }
            catch (e: Exception) {
                Log.e("Bitmap decode error", e.message, e)
            }
        }
    }

    fun fetchImage(activity: Activity, url: String, imageView: ImageView, type: Asset.AssetType = Asset.AssetType.Other) {
        fetchAsset(activity, url, type) { asset ->
            Glide.with(activity).load(File(asset.path)).into(imageView)
        }
    }


    fun lookupAsset(url: String): Asset? {
        var asset: Asset? = null
        realm.executeTransaction {
            asset = realm.where(Asset::class.java).equalTo("url", url).findFirst()
            asset?.let {
                it.lastAccessedAt = Date()
            }
        }
        return asset
    }

    fun downloadAsset(
        activity: Activity,
        urlString: String,
        type: Asset.AssetType = Asset.AssetType.Other,
        downloadHandler: ((activity: Activity, urlString: String, type: Asset.AssetType, onComplete: (asset: Asset) -> Unit) -> Unit)? = null,
        onComplete: CompletionCallback
    ){
        synchronized(lock) {
            if (completionCallbackMap.containsKey(urlString)) {
                val callbacks: MutableList<CompletionCallback> = (completionCallbackMap[urlString])!!
                callbacks.add(onComplete)
            }
            else {
                val callbacks = mutableListOf(onComplete)
                completionCallbackMap.put(urlString, callbacks)
                val completeHandler: CompletionCallback = { asset ->
                    val callbacks = completionCallbackMap.remove(urlString)
                    callbacks?.forEach {
                        it(asset)
                    }
                }

                if (downloadHandler != null) {
                    downloadHandler(activity, urlString, type, completeHandler)
                }
                else {
                    downloadAssetImpl(activity, urlString, type, completeHandler)
                }
            }
        }
    }

    fun downloadAssetImpl(
        activity: Activity,
        urlString: String,
        type: Asset.AssetType = Asset.AssetType.Other,
        onComplete: CompletionCallback
    ) {
        synchronized(lock) {
            val url = adjustURL(URL(urlString))
            val dest: File = generateDownloadFile()
            Api(activity).downloadResource(url, dest) { file ->
                registerAsset(url.toString(), type, file)?.let { asset ->
                    onComplete(asset)
                }
            }
        }
    }

    open fun registerAsset(url: String, type: Asset.AssetType, file: File): Asset? {
        var asset: Asset? = null

        if (file.exists()) {
            removeExpiredCache(type)
            realm.executeTransaction {
                asset = realm.where(Asset::class.java).equalTo("url", url).findFirst()?: realm.createObject(Asset::class.java, url)
                asset?.path = file.path
                asset?.lastAccessedAt = Date()
                asset?.type = type
            }
        }

        return asset
    }

    fun removeExpiredCache(type: Asset.AssetType) {
        realm.executeTransaction {
            val count = realm.where(Asset::class.java).equalTo("assetType", type.name).count().toInt()
            val max = maxAssets[type] ?: 0
            if (count > max) {
                val assets = realm.where(Asset::class.java).equalTo("assetType", type.name).sort("lastAccessedAt", Sort.ASCENDING).findAll()
                val exceeded = count - max
                for (i in 1 .. Math.min(exceeded, assets.count())) {
                    assets.get(i - 1)?.let {
                        // ファイルの実体を削除します
                        File(it.path).delete()
                        // DBより削除します
                        it.deleteFromRealm()
                    }
                }
            }
        }
    }

    fun adjustURL(url: URL): URL {
        if (url.protocol.isBlank() || url.host.isBlank()) {
            val pathAndQuery = arrayOf(url.path, url.query).filterNotNull().joinToString("?")
            val pathAndQueryAndFragment = arrayOf(pathAndQuery, url.ref).filterNotNull().joinToString("#")
            return URL(URL(BuildConfig.SERVER_BASE_URL), pathAndQueryAndFragment)
        }
        return url
    }

    fun generateDownloadFile(): File {
        // FIXME: 一時ディレクトリを作成すること
        return createTempFile()
    }
}