package jp.co.recruit.erikura.presenters.fragments

import android.content.Intent
import android.graphics.Typeface
import android.graphics.Typeface.BOLD
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
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
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.util.Log
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.bold
import jp.co.recruit.erikura.presenters.activities.job.PlaceDetailActivity


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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        var tv = activity!!.findViewById<TextView>(R.id.jobDetailsView_placeLink)

        var place = SpannableStringBuilder()
        if(job != null) {
            if ( (job.place?.hasEntries?: false) || (job.place?.workingPlaceShort.isNullOrBlank()) ) {
                place.append(job.place?.workingPlace)
                if(!(job.place?.workingBuilding.isNullOrBlank())) {
                    place.append("\n${job.place?.workingBuilding}")
                }
            }else {
                place.append(job.place?.workingPlaceShort)
            }
            place.append("　")
            var start = place.length
            place.bold { append(ErikuraApplication.instance.getString(R.string.jobDetails_workingPlaceLink)) }
            var end = place.length
            place.setSpan(object : ClickableSpan() {
                override fun onClick(view: View) {
                    // 場所詳細画面へ遷移
                    val intent= Intent(activity, PlaceDetailActivity::class.java)
                    intent.putExtra("place", job.place)
                    intent.flags = Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
                    startActivity(intent)
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            val tf = Typeface.create(ResourcesCompat.getFont(ErikuraApplication.instance.applicationContext, R.font.hirakakupron_w6_alphanbum_01), BOLD)
            place.setSpan(tf, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        tv.text = place
        tv.movementMethod = LinkMovementMethod.getInstance()
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
    val workingPlace: MutableLiveData<SpannableStringBuilder> = MutableLiveData()

    val openMapButtonText: MutableLiveData<SpannableString> = MutableLiveData()

    fun setup(job: Job?){
        if (job != null) {
            // 納期
            limit.value = "${dateToString(job.workingStartAt?: Date(), "yyyy/MM/dd HH:mm")} ～ ${dateToString(job.workingFinishAt?: Date(), "yyyy/MM/dd HH:mm")}"
            // 持ち物
            tool.value = job.tools
            // 仕事概要
            summary.value = job.summary
            // 報告箇所
            var summaryTitleStr = ""
            job.summaryTitles.forEachIndexed { index, s ->
                if (index == job.summaryTitles.lastIndex) {
                    summaryTitleStr += "${s}"
                }else {
                    summaryTitleStr += "${s}、"
                }
            }
            summaryTitles.value = summaryTitleStr
            // 場所
/*            var place = SpannableStringBuilder()

            if ( (job.place?.hasEntries?: false) || (job.place?.workingPlaceShort.isNullOrBlank()) ) {
                place.append(job.place?.workingPlace)
                if(!(job.place?.workingBuilding.isNullOrBlank())) {
                    place.append("\n${job.place?.workingBuilding}")
                }
            }else {
                place.append(job.place?.workingPlaceShort)
            }
            place.append("　")
            var start = place.length
            place.bold { append(ErikuraApplication.instance.getString(R.string.jobDetails_workingPlaceLink)) }
            var end = place.length
            place.setSpan(object : ClickableSpan() {
                override fun onClick(view: View) {
                    // 場所詳細画面へ遷移
                    Log.v("debug", "linkが押下された")
                    val intent= Intent(get, PlaceDetailActivity::class.java)
                    intent.putExtra("place", place)
                    startActivity(intent)
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            val tf = Typeface.create(ResourcesCompat.getFont(ErikuraApplication.instance.applicationContext, R.font.hirakakupron_w6_alphanbum_01), BOLD)
            place.setSpan(tf, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            workingPlace.value = place*/
        }

        val str = SpannableString(ErikuraApplication.instance.getString(R.string.openMap))
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