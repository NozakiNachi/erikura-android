package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.drawToBitmap
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.storage.Asset
import jp.co.recruit.erikura.data.storage.AssetsManager
import jp.co.recruit.erikura.databinding.FragmentMarkerBinding
import jp.co.recruit.erikura.presenters.view_models.JobDetailMarkerView
import jp.co.recruit.erikura.presenters.view_models.MarkerViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

// マーカーの作成が終わった場合に呼び出されるコールバック
typealias MarkerSetupCallback = (Marker) -> Unit

class ErikuraMarkerView(private val activity: AppCompatActivity, private val map: GoogleMap, private val job: Job, private val optionsRequired: Boolean) {
    companion object {
        const val BASE_ZINDEX: Float            = 5000f
        const val ACTIVE_ZINDEX_OFFSET: Float   = 10000f
        const val BOOST_ZINDEX_OFFSET: Float    = 1000f
        const val WANTED_ZINDEX_OFFSET: Float   = 2000f
        const val SOON_ZINDEX_OFFSET: Float     = -1000f
        const val FUTURE_ZINDEX_OFFSET: Float   = -2000f
        const val ENTRIED_ZINDEX_OFFSET: Float  = -6000f

        val assetsManager: AssetsManager get() = ErikuraApplication.assetsManager

        fun build(activity: AppCompatActivity, map: GoogleMap, job: Job, optionsRequired: Boolean = true, callback: MarkerSetupCallback?): ErikuraMarkerView {
            val markerView = ErikuraMarkerView(activity, map, job, optionsRequired)
            callback?.invoke(markerView.marker)
            return markerView
        }
    }

    private val markerViewModel: MarkerViewModel = makeMarkerViewModel(job, optionsRequired)
    lateinit var marker: Marker

    var active: Boolean
        get() = markerViewModel.active.value ?: false
        set(value) {
            markerViewModel.active.value = value
            updateMarkerIcon()
        }

    init {
        buildMarker()
    }

    fun makeMarkerViewModel(job: Job, optionsRequired: Boolean = true): MarkerViewModel {
        return if (optionsRequired) {MarkerViewModel(job)} else {JobDetailMarkerView(job)}
    }

    private fun buildMarker(): Marker {
        val markerUrl = markerViewModel.markerUrl

        return assetsManager.lookupAsset(markerUrl)?.let {
            marker = map.addMarker(
                MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromPath(it.path))
                    .position(job.latLng)
            )
            marker
        } ?: run {
            marker = map.addMarker(
                MarkerOptions()
                    .position(job.latLng)
            ).also {
                marker = it
                updateMarkerIcon()
            }
            marker
        }
    }

    private fun updateMarkerIcon() {
        val markerUrl = markerViewModel.markerUrl

        return assetsManager.lookupAsset(markerUrl)?.let {
            activity.run {
                marker.setIcon(BitmapDescriptorFactory.fromPath(it.path))
            }
        } ?: run {
            buildMarkerImage() {
                activity.run {
                    marker.setIcon(BitmapDescriptorFactory.fromPath(it.path))
                }
            }
        }
    }

    private fun buildMarkerImage(callback: (Asset) -> Unit) {
        val markerUrl: String = markerViewModel.markerUrl
        val viewModel = makeMarkerViewModel(markerViewModel.job, optionsRequired)
        viewModel.icon.value = markerViewModel.icon.value
        viewModel.active.value = markerViewModel.active.value

        markerViewModel.iconUrl?.also { url ->
            assetsManager.fetchImage(activity, url.toString(), Asset.AssetType.Marker) { icon ->
                markerViewModel.icon.value = icon
                viewModel.icon.value = icon
                buildMarkerImageImpl(callback, markerUrl, viewModel, saveCache = true)
            }
        } ?: run{
            buildMarkerImageImpl(callback, markerUrl, viewModel, saveCache = false)
        }
    }

    private fun buildMarkerImageImpl(callback: (Asset) -> Unit, markerUrl: String, viewModel: MarkerViewModel, saveCache: Boolean) {
        val lifecycleOwner = activity

        val downloadHandler: (Activity, String, Asset.AssetType, (Asset) -> Unit) -> Unit = { _, _, type, onComplete ->
            val dest = generateMarkerImage(viewModel)
            try {
                if (saveCache) {
                    assetsManager.removeExpiredCache(type)
                    lateinit var asset: Asset
                    assetsManager.realm.executeTransaction { realm ->
                        asset = realm.where(Asset::class.java).equalTo("url", markerUrl).findFirst() ?: realm.createObject(Asset::class.java, markerUrl)
                        asset.path = dest.path
                        asset.lastAccessedAt = Date()
                        asset.type = type
                    }
                    onComplete(asset)
                }
                else {
                    val asset = Asset()
                    asset.url = markerUrl
                    asset.path = dest.path
                    asset.lastAccessedAt = Date()
                    asset.type = type

                    onComplete(asset)

                    dest.delete()
                }
            } catch (e: IOException) {
                Log.e("ERIKURA", "Marker Image Creation Failed: ${e.message}", e)
            }
        }
        assetsManager.downloadAsset(activity, markerUrl, Asset.AssetType.Marker, downloadHandler) { asset ->
            callback(asset)
        }
    }

    private fun generateMarkerImage(viewModel: MarkerViewModel): File {
        val binding = FragmentMarkerBinding.inflate(activity.layoutInflater, null, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel

        binding.executePendingBindings()

        val markerView = binding.root
        markerView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)

        val markerImage = markerView.drawToBitmap(Bitmap.Config.ARGB_8888)
        val dest = assetsManager.generateDownloadFile()
        FileOutputStream(dest).use { out ->
            markerImage.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }

        return dest
    }
}
