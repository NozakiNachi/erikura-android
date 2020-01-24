package jp.co.recruit.erikura.presenters.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentJobDetailsViewBinding
import java.text.SimpleDateFormat
import java.util.*


class JobDetailsViewFragment(val job: Job?) : Fragment(), JobDetailsViewFragmentEventHandlers {
    private val viewModel: JobDetailsViewFragmentViewModel by lazy {
        ViewModelProvider(this).get(JobDetailsViewFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentJobDetailsViewBinding.inflate(inflater, container, false)
        viewModel.setup(job)
        binding.viewModel = viewModel
        binding.handlers = this
        return binding.root
    }

    override fun onClickOpenMap(view: View) {
        val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${job?.latitude?:0},${job?.longitude?:0}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

}

class JobDetailsViewFragmentViewModel: ViewModel() {
    val limit: MutableLiveData<String> = MutableLiveData()
    val tool: MutableLiveData<String> = MutableLiveData()
    val summary: MutableLiveData<String> = MutableLiveData()
    val summaryTitles: MutableLiveData<String> = MutableLiveData()
    val workingPlace: MutableLiveData<String> = MutableLiveData()
    val workingBuilding: MutableLiveData<String> = MutableLiveData()

    val openMapButtonText: MutableLiveData<SpannableString> = MutableLiveData()

    fun setup(job: Job?){
        if (job != null) {
            limit.value = "${dateToString(job.workingStartAt?: Date(), "yyyy/MM/dd HH:mm")} ～ ${dateToString(job.workingFinishAt?: Date(), "yyyy/MM/dd HH:mm")}"
            tool.value = job.tools
            summary.value = job.summary

            var summaryTitleStr = ""
            job.summaryTitles.forEachIndexed { index, s ->
                if (index == job.summaryTitles.lastIndex) {
                    summaryTitleStr += "${s}"
                }else {
                    summaryTitleStr += "${s}、"
                }
            }

            summaryTitles.value = summaryTitleStr
            workingPlace.value = job.place?.workingPlace
            workingBuilding.value = job.place?.workingBuilding
        }

        val str = SpannableString("　地図アプリで開く")
        val drawable = ContextCompat.getDrawable(ErikuraApplication.instance.applicationContext, R.drawable.link)
        drawable!!.setBounds(0, 0, 40, 40)
        val span = ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BASELINE)
        str.setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        openMapButtonText.value = str
    }

    private fun dateToString(date: Date, format: String): String {
        val sdf = SimpleDateFormat(format, Locale.JAPAN)
        return sdf.format(date)
    }
}

interface JobDetailsViewFragmentEventHandlers {
    fun onClickOpenMap(view: View)
}