package jp.co.recruit.erikura.presenters.view_models

import android.app.Activity
import android.graphics.Bitmap
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobStatus
import java.text.SimpleDateFormat
import java.util.*

class JobListItemViewModel(activity: Activity, val job: Job, val currentPosition: LatLng?): ViewModel() {
    val assetsManager = ErikuraApplication.assetsManager
    val resources = activity.resources
    val dateFormat = SimpleDateFormat("YYYY/MM/dd HH:mm")

    val reward: String get() = String.format("%,d円", job.fee)
    val workingTime: String get() = String.format("%d分", job.workingTime)
    val workingFinishAt: String get() = String.format("〜%s", dateFormat.format(job.workingFinishAt))

    val image: MutableLiveData<Bitmap> = MutableLiveData()
    val textColor: MutableLiveData<Int> = MutableLiveData()
    val timeLimit: MutableLiveData<SpannableStringBuilder> = MutableLiveData()
    val distance: MutableLiveData<SpannableStringBuilder> = MutableLiveData()

    init {
        if (job.isPastOrInactive) {
            textColor.value = ContextCompat.getColor(activity, R.color.warmGrey)
            timeLimit.value = SpannableStringBuilder().apply {
                append("受付終了")
            }
        }
        else if (job.isFuture) {
            textColor.value = ContextCompat.getColor(activity, R.color.waterBlue)
            val now = Date()
            val diff = job.workingStartAt.time - now.time
            val diffHours = diff / (60 * 60 * 1000)
            val diffDays = diffHours / 24
            val diffRestHours = diffHours % 24

            val sb = SpannableStringBuilder()
            sb.append("募集開始まで")
            if (diffDays > 0) {
                val start = sb.length
                sb.append(diffDays.toString())
                sb.setSpan(RelativeSizeSpan(16.0f / 12.0f), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.append("日")
            }
            if (diffDays > 0 && diffRestHours > 0) {
                sb.append("と")
            }
            if (diffRestHours > 0) {
                val start = sb.length
                sb.append(diffRestHours.toString())
                sb.setSpan(RelativeSizeSpan(16.0f / 12.0f), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.append("時間")
            }
            timeLimit.value = sb
        }
        else {
            when(job.status) {
                JobStatus.Working -> {
                    textColor.value = ContextCompat.getColor(activity, R.color.vibrantGreen)
                    timeLimit.value = SpannableStringBuilder().apply {
                        append("作業実施中")
                    }
                }
                JobStatus.Finished -> {
                    textColor.value = ContextCompat.getColor(activity, R.color.vibrantGreen)
                    timeLimit.value = SpannableStringBuilder().apply {
                        append("実施済み(未報告)")
                    }
                }
                JobStatus.Reported -> {
                    textColor.value = ContextCompat.getColor(activity, R.color.warmGrey)
                    timeLimit.value = SpannableStringBuilder().apply {
                        append("作業報告済み")
                    }
                }
                else -> {
                    textColor.value = ContextCompat.getColor(activity, R.color.coral)
                    val now = Date()
                    val diff = job.workingFinishAt.time - now.time
                    val diffHours = diff / (60 * 60 * 1000)
                    val diffDays = diffHours / 24
                    val diffRestHours = diffHours % 24

                    val sb = SpannableStringBuilder()
                    sb.append("作業終了まで")
                    if (diffDays > 0) {
                        val start = sb.length
                        sb.append(diffDays.toString())
                        sb.setSpan(
                            RelativeSizeSpan(16.0f / 12.0f),
                            start,
                            sb.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        sb.append("日")
                    }
                    if (diffDays > 0 && diffRestHours > 0) {
                        sb.append("と")
                    }
                    if (diffRestHours > 0) {
                        val start = sb.length
                        sb.append(diffRestHours.toString())
                        sb.setSpan(
                            RelativeSizeSpan(16.0f / 12.0f),
                            start,
                            sb.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        sb.append("時間")
                    }
                    timeLimit.value = sb
                }
            }
        }

        currentPosition?.let {
            val dist = SphericalUtil.computeDistanceBetween(job.latLng, it)
            val sb = SpannableStringBuilder()
            sb.append("検索地点より")

            val start = sb.length
            sb.append(String.format("%,dm", dist.toInt()))
            sb.setSpan(
                RelativeSizeSpan(16.0f / 12.0f),
                start,
                sb.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            distance.value = sb
        }

        job.thumbnailUrl?.let { url ->
            assetsManager.fetchImage(activity, url) { bitmap ->
                image.value = bitmap
            }
        }
    }
}
