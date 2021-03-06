package jp.co.recruit.erikura.presenters.view_models

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

open class MarkerViewModel(val job: Job): ViewModel() {
    val active: MutableLiveData<Boolean> = MutableLiveData(false)
    val icon: MutableLiveData<Bitmap> = MutableLiveData()

    val fee: String get() = String.format("%,d円", job.fee)
    open val isDisabled: Boolean get() = (job.isEntried || job.isFuture) && !isOwnerMarker
    val iconUrl: URL? get () {
        if (job.isPreEntry) {
            return job.jobKind?.activeIconUrl
        }
        else if (isDisabled) {
            return job.jobKind?.inactiveIconUrl
        }
        else {
            return job.jobKind?.activeIconUrl
        }
    }

    open val isOwnerMarker: Boolean get() {
        // 自身が応募した案件、かつ作業報告前の場合のみ「自身が応募」の表示を行います
        return job.isOwner && !job.isReported
    }

    open val boostVisibility: Int get() {
        return when {
            isOwnerMarker -> View.GONE
            job.boost -> View.VISIBLE
            else -> View.GONE
        }
    }
    open val wantedVisibility: Int get() {
        return when {
            isOwnerMarker -> View.GONE
            job.wanted -> View.VISIBLE
            else -> View.GONE
        }
    }

    open val soonVisibility: Int get() {
        return when {
            job.isEntried -> View.GONE
            job.isStartSoon && !job.isPreEntry -> View.VISIBLE
            else -> View.GONE
        }
    }

    open val futureVisibility: Int get() {
        return when {
            job.isEntried -> View.GONE
            job.isFuture && !job.isStartSoon && !job.isPreEntry -> View.VISIBLE
            else -> View.GONE
        }
    }

    open val futureText: String get() {
        if (job?.preEntryStartAt != null) {
            //　先行応募の場合
            val sdf = SimpleDateFormat("MM/dd")
            return String.format("%s開始", sdf.format(job?.preEntryStartAt))
        } else {
            return job.workingStartAt?.let {
                val sdf = SimpleDateFormat("MM/dd")
                return String.format("%s開始", sdf.format(it))
            } ?: ""
        }
    }

    open val preEntryVisibility: Int get() {
        return when {
            job.isPreEntry -> View.VISIBLE
            else -> View.GONE
        }
    }

    open val preEntryText: CharSequence get() {
        return SpannableStringBuilder().apply {
            ResourcesCompat.getFont(ErikuraApplication.applicationContext, R.font.fa_solid_900)?.let { fasFont ->
                JobUtil.appendStringWithFont(this, "\uf058 ", "fas", fasFont)
            }
            append("先行応募可")
        }
    }

    open val ownerVisibility: Int get() {
        return when {
            isOwnerMarker -> View.VISIBLE
            else -> View.GONE
        }
    }

    val resources: Resources get() = ErikuraApplication.instance.applicationContext.resources

    val bodyBackground: Drawable
        get() {
            val drawableId = if (job.isPreEntry) {
                R.drawable.background_pre_entry_marker
            }
            else if (isDisabled) {
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
            val drawableId = if (job.isPreEntry) {
                R.drawable.background_marker_triangle
            }
            else if (isDisabled) {
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
        } else if(job.isPreEntry) {
            R.color.warmGrey
        } else if (isDisabled) {
            R.color.pinkishGrey
        } else {
            R.color.black
        }
        return ErikuraApplication.instance.applicationContext.resources.getColor(colorId, null)
    }

    open val markerUrl: String get() {
        val iconPath = job.jobKind?.activeIconUrl?.path ?: "EMPTY_PATH"
        val entry = when {
            isOwnerMarker   -> "owner"
            job.isEntried   -> "true"
            else            -> "false"
        }
        if (job.isPreEntry) {
            return "eriukra-marker://v2/${job.fee}/$entry/${job.wanted}/${job.boost}/${active.value}/${job.isFuture}/preEntry/${iconPath}/"
        }
        if (job.isStartSoon) {
            return "eriukra-marker://v2/${job.fee}/$entry/${job.wanted}/${job.boost}/${active.value}/${job.isFuture}/comingSoon/${iconPath}/"
        }
        else if (job.isFuture) {
            val df = SimpleDateFormat("YYYYMMdd")
            val time = if (job.preEntryStartAt != null) {
                df.format(job.preEntryStartAt ?: Date())
            } else {
                df.format(job.workingStartAt ?: Date())
            }
            return "eriukra-marker://v2/${job.fee}/$entry/${job.wanted}/${job.boost}/${active.value}/${job.isFuture}/${time}/${iconPath}/"
        }
        else {
            return "eriukra-marker://v2/${job.fee}/$entry/${job.wanted}/${job.boost}/${active.value}/${job.isFuture}/current/${iconPath}/"
        }
    }
}
