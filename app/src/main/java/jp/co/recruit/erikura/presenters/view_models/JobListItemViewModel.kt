package jp.co.recruit.erikura.presenters.view_models

import JobUtil
import android.app.Activity
import android.graphics.Bitmap
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.util.JobUtils

class JobListItemViewModel(activity: Activity, val job: Job, val currentPosition: LatLng? = null, val timeLabelType: JobUtil.TimeLabelType): ViewModel() {
    val assetsManager = ErikuraApplication.assetsManager

    val reward: String get() = String.format("%,d円", job.fee)
    val workingTime: String get() = String.format("%d分", job.workingTime)
    val workingFinishAt: String get() {
        val finishAt = if (timeLabelType == JobUtil.TimeLabelType.OWNED) {
            job?.entry?.limitAt ?: job.workingFinishAt
        }
        else {
            job.workingFinishAt
        }
        return String.format("〜%s", JobUtils.DateFormats.simple.format(finishAt))
    }
    val tools: String get() = String.format("持ち物: %s", job.tools ?: "")

    val image: MutableLiveData<Bitmap> = MutableLiveData()
    val textColor: MutableLiveData<Int> = MutableLiveData()
    val timeLimit: MutableLiveData<SpannableStringBuilder> = MutableLiveData()
    val distance: MutableLiveData<SpannableStringBuilder> = MutableLiveData()

    val goodCount: Int get() = job?.report?.operatorLikesCount ?: 0
    val commentCount: Int get() = job?.report?.operatorCommentsCount ?: 0
    val goodText: String get() = String.format("%,d件", goodCount)
    val commentText: String get() = String.format("%,d件", commentCount)
    val hasGood: Boolean get() = goodCount > 0
    val hasComment: Boolean get() = commentCount > 0
    val goodVisible: Int get() = if (timeLabelType == JobUtil.TimeLabelType.OWNED && hasGood) { View.VISIBLE } else { View.GONE }
    val commentVisible: Int get() = if (timeLabelType == JobUtil.TimeLabelType.OWNED && hasComment) { View.VISIBLE } else { View.GONE }

    val boostVisibility: Int get() { return if (job.boost) { View.VISIBLE } else { View.GONE} }
    val wantedVisibility: Int get() { return if (job.wanted) { View.VISIBLE } else { View.GONE} }

    init {
        val (timeLimitText, timeLimitColor) = JobUtil.setupTimeLabel(ErikuraApplication.instance.applicationContext, job, timeLabelType)
        textColor.value = timeLimitColor
        timeLimit.value = timeLimitText

        currentPosition?.let {
            val dist = SphericalUtil.computeDistanceBetween(job.latLng, it)
            val sb = SpannableStringBuilder()
            sb.append("検索地点より")

            val start = sb.length
            if (dist < 1000.0) {
                sb.append(String.format("%,dm", dist.toInt()))
            }else {
                var distDouble: Double = dist / 1000
                sb.append(String.format("%.1fkm", distDouble))
            }
            sb.setSpan(
                RelativeSizeSpan(16.0f / 12.0f),
                start,
                sb.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            distance.value = sb
        }
    }
}
