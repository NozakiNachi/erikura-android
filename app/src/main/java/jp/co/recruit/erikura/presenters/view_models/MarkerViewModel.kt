package jp.co.recruit.erikura.presenters.view_models

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MarkerViewModel(private val job: Job): ViewModel() {
    val active: MutableLiveData<Boolean> = MutableLiveData()
    val icon: MutableLiveData<Bitmap> = MutableLiveData()

    val fee: String get() = String.format("%,d円", job.fee)
    val isDisabled: Boolean get() = job.isEntried || job.isFuture
    val iconUrl: URL? get () {
        if (isDisabled) {
            return job.jobKind.inactiveIconUrl
        }
        else {
            return job.jobKind.activeIconUrl
        }
    }

    val boostVisibility: Int get() {
        if (job.boost) {
            return View.VISIBLE
        }
        else {
            return View.GONE
        }
    }
    val wantedVisibility: Int get() {
        if (job.wanted) {
            return View.VISIBLE
        }
        else {
            return View.GONE
        }
    }
    val soonVisibility: Int get() {
        val now = Date()
        if (job.workingStartAt > now && (job.workingStartAt.time - now.time) < (24 * 60 * 60 * 1000)) {
            return View.VISIBLE
        }
        else {
            return View.GONE
        }
    }

    val resources: Resources get() = ErikuraApplication.instance.applicationContext.resources

    val bodyBackground: Drawable
        get() {
            val drawableId = if (isDisabled) {
                if (active.value ?: false) {
                    R.drawable.background_disabled_marker_active
                }
                else {
                    R.drawable.background_disabled_marker
                }
            }
            else {
                if (active.value ?: false) {
                    R.drawable.background_marker_active
                }
                else {
                    R.drawable.background_marker
                }
            }
            return resources.getDrawable(drawableId, null)
        }

    val triangleBackground: Drawable
        get() {
            val drawableId = if (isDisabled) {
                if (active.value ?: false) {
                    R.drawable.background_marker_disabled_triangle_active
                }
                else {
                    R.drawable.background_marker_disabled_triangle
                }
            }
            else {
                if (active.value ?: false) {
                    R.drawable.background_marker_triangle_active
                }
                else {
                    R.drawable.background_marker_triangle
                }
            }
            return resources.getDrawable(drawableId, null)
        }

    val color: Int get() {
        val colorId = if (active.value ?: false) {
            R.color.pumpkinOrange
        } else if (isDisabled) {
            R.color.pinkishGrey
        } else {
            R.color.black
        }
        return ErikuraApplication.instance.applicationContext.resources.getColor(colorId, null)
    }

    val markerUrl: String get() {
        val iconPath = job.jobKind.activeIconUrl?.path ?: "EMPTY_PATH"
        if (job.isStartSoon) {
            return "eriukra-marker://v2/${job.fee}/${job.isEntried}/${job.wanted}/${job.boost}/${active}/${job.isFuture}/comingSoon/${iconPath}/"
        } else if (job.isFuture) {
            val df = SimpleDateFormat("YYYYMMdd")
            val time = df.format(job.workingStartAt)
            return "eriukra-marker://v2/${job.fee}/${job.isEntried}/${job.wanted}/${job.boost}/${active}/${job.isFuture}/${time}/${iconPath}/"
        } else {
            return "eriukra-marker://v2/${job.fee}/${job.isEntried}/${job.wanted}/${job.boost}/${active}/${job.isFuture}/current/${iconPath}/"
        }
    }
}
