package jp.co.recruit.erikura.presenters.view_models

import android.app.Activity
import android.graphics.Bitmap
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.business.models.Job
import java.text.SimpleDateFormat

class JobListItemViewModel(activity: Activity, val job: Job, val currentPosition: LatLng?): ViewModel() {
    val assetsManager = ErikuraApplication.assetsManager
    val resources = activity.resources
    val dateFormat = SimpleDateFormat("YYYY/MM/dd HH:mm")

    val reward: String get() = String.format("%,d円", job.fee)
    val workingTime: String get() = String.format("%d分", job.workingTime)
    val workingFinishAt: String get() = String.format("〜%s", dateFormat.format(job.workingFinishAt))
    val tools: String get() = String.format("持ち物: %s", job.tools ?: "")

    val image: MutableLiveData<Bitmap> = MutableLiveData()
    val textColor: MutableLiveData<Int> = MutableLiveData()
    val timeLimit: MutableLiveData<SpannableStringBuilder> = MutableLiveData()
    val distance: MutableLiveData<SpannableStringBuilder> = MutableLiveData()

    init {
        val (timeLimitText, timeLimitColor) = JobUtil.setupTimeLabel(ErikuraApplication.instance.applicationContext, job)
        textColor.value = timeLimitColor
        timeLimit.value = timeLimitText

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
