package jp.co.recruit.erikura.presenters.fragments

import android.app.ActivityOptions
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
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.bold
import jp.co.recruit.erikura.business.models.JobStatus
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
        binding.lifecycleOwner = activity
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
            val linkTextAppearanceSpan = TextAppearanceSpan(ErikuraApplication.instance.applicationContext, R.style.linkText)
            place.setSpan(linkTextAppearanceSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            place.setSpan(object : ClickableSpan() {
                override fun onClick(view: View) {
                    // 場所詳細画面へ遷移
                    val intent= Intent(activity, PlaceDetailActivity::class.java)
                    intent.putExtra("workingPlace", job.place)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        tv.text = place
        tv.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onClickOpenMap(view: View) {
        val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${job?.latitude?:0},${job?.longitude?:0}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
    }

}

class JobDetailsViewFragmentViewModel: ViewModel() {
    val limit: MutableLiveData<String> = MutableLiveData()
    val msgVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val tool: MutableLiveData<String> = MutableLiveData()
    val summary: MutableLiveData<String> = MutableLiveData()
    val summaryTitles: MutableLiveData<String> = MutableLiveData()
    val summaryTitlesVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val openMapButtonText: MutableLiveData<SpannableString> = MutableLiveData()

    fun setup(job: Job?){
        job?.let { job ->
            // 納期
            setupLimit(job)
            // 持ち物
            setupTools(job)
            // 仕事概要
            setupSummary(job)
            // 報告箇所
            setupSummaryTitles(job)
        }

        val str = SpannableString(ErikuraApplication.instance.getString(R.string.openMap))
        val drawable = ContextCompat.getDrawable(ErikuraApplication.instance.applicationContext, R.drawable.link)
        drawable!!.setBounds(0, 0, 40, 40)
        val span = ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BASELINE)
        str.setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        openMapButtonText.value = str
    }

    /**
     * 納期表示を設定します
     */
    private fun setupLimit(job: Job) {
        val format = "yyyy/MM/dd HH:mm"
        if (job.entry != null && (job.entry?.owner ?: false)) {
            // 自身が応募したタスクの場合は、エントリ時刻と、24時間のリミット時刻を表示します
            val startAt = dateToString(job.entry?.createdAt ?: Date(), format)
            val finishAt = dateToString(job.entry?.limitAt ?: Date(), format)
            limit.value = "${startAt} 〜 ${finishAt}"
            job.entry?.limitAt?.also { limitAt ->
                if (!job.isReported && limitAt < Date()) {
                    msgVisibility.value = View.VISIBLE
                }
            }
        }
        else {
            val startAt = dateToString(job.workingStartAt ?: Date(), format)
            val finishAt = dateToString(job.workingFinishAt ?: Date(), format)
            // 自分が応募していないタスクについて募集期間を表示します
            limit.value = "${startAt} 〜 ${finishAt}"
        }
    }

    /**
     * 持ち物を設定します
     */
    private fun setupTools(job: Job) {
        tool.value = job.tools
    }

    /**
     * 仕事概要を設定します
     */
    private fun setupSummary(job: Job) {
        summary.value = job.summary
    }

    private fun setupSummaryTitles(job: Job) {
        // 報告箇所がからの場合は非表示とする
        if (job.summaryTitles.isEmpty()) {
            summaryTitlesVisibility.value = View.GONE
            summaryTitles.value = ""
        }
        else {
            summaryTitlesVisibility.value = View.VISIBLE
            summaryTitles.value = job.summaryTitles.joinToString("、")
        }
    }

    private fun dateToString(date: Date, format: String): String {
        val sdf = SimpleDateFormat(format, Locale.JAPAN)
        return sdf.format(date)
    }
}

interface JobDetailsViewFragmentEventHandlers {
    fun onClickOpenMap(view: View)
}