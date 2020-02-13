package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

// マーカーの作成が終わった場合に呼び出されるコールバック
typealias MarkerSetupCallback = (Marker) -> Unit

class ErikuraMarkerView(private val activity: AppCompatActivity, private val map: GoogleMap, private val job: Job) {
    companion object {
        const val BASE_ZINDEX: Float            = 5000f
        const val ACTIVE_ZINDEX_OFFSET: Float   = 10000f
        const val BOOST_ZINDEX_OFFSET: Float    = 1000f
        const val WANTED_ZINDEX_OFFSET: Float   = 2000f
        const val SOON_ZINDEX_OFFSET: Float     = -1000f
        const val FUTURE_ZINDEX_OFFSET: Float   = -2000f
        const val ENTRIED_ZINDEX_OFFSET: Float  = -6000f

        val assetsManager: AssetsManager get() = ErikuraApplication.assetsManager

        fun build(activity: AppCompatActivity, map: GoogleMap, job: Job, callback: MarkerSetupCallback?): ErikuraMarkerView {
            val markerView = ErikuraMarkerView(activity, map, job)
            callback?.invoke(markerView.marker)
            return markerView
        }
    }

    private val markerViewModel: MarkerViewModel = if (activity.localClassName == "presenters.activities.job.JobDetailsActivity") JobDetailMarkerView(job) else MarkerViewModel(job)
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

    private fun buildMarker(): Marker {
        val markerUrl = markerViewModel.markerUrl

        return assetsManager.lookupAsset(markerUrl)?.let {
            val bitmap = BitmapFactory.decodeFile(it.path)
            marker = map.addMarker(
                MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
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
                val bitmap = BitmapFactory.decodeFile(it.path)
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap))
            }
        } ?: run {
            buildMarkerImage() {
                activity.run {
                    val bitmap = BitmapFactory.decodeFile(it.path)
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap))
                }
            }
        }
    }

    private fun buildMarkerImage(callback: (Asset) -> Unit) {
        val markerUrl: String = markerViewModel.markerUrl
        val viewModel = MarkerViewModel(markerViewModel.job)
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

        val downloadHandler: (Activity, String, Asset.AssetType, (Asset) -> Unit) -> Unit = { activity, _urlString, type, onComplete ->
            val binding = FragmentMarkerBinding.inflate(activity.layoutInflater, null, false)
            binding.lifecycleOwner = lifecycleOwner
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
            try {
                FileOutputStream(dest).use { out ->
                    markerImage.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }

                if (saveCache) {
                    assetsManager.removeExpiredCache(type)
                    assetsManager.realm.executeTransaction { realm ->
                        val asset = realm.createObject(Asset::class.java, markerUrl)
                        asset.path = dest.path
                        asset.lastAccessedAt = Date()
                        asset.type = type
                        onComplete(asset)
                    }
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
                Log.e("Error", e.message, e)
                // FIXME: エラー処理として何をするべきか?
            }
        }
        assetsManager.downloadAsset(activity, markerUrl, Asset.AssetType.Marker, downloadHandler) { asset ->
            callback(asset)
        }
    }
}
