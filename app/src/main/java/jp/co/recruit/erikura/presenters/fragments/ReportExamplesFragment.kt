package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.*
import jp.co.recruit.erikura.databinding.FragmentReportExamplesBinding
import jp.co.recruit.erikura.databinding.FragmentReportExamplesSummaryItemBinding
import jp.co.recruit.erikura.presenters.activities.report.ReportExamplesActivity
import java.text.SimpleDateFormat
import java.util.*

class ReportExamplesFragment : Fragment, ReportExamplesFragmentEventHandlers {
    companion object {
        const val REPORT_EXAMPLES_ARGUMENT = "report_example"
        const val JOB = "job"
        const val POSITION = "position"
        const val REPORT_EXAMPLE_COUNT = "report_example_count"

        fun newInstance(
            reportExample: ReportExample?,
            job: Job,
            position: Int,
            reportExampleCount: Int
        ): ReportExamplesFragment {
            return ReportExamplesFragment().also {
                it.arguments = Bundle().also { args ->
                    args.putParcelable(REPORT_EXAMPLES_ARGUMENT, reportExample)
                    args.putParcelable(JOB, job)
                    args.putInt(POSITION, position)
                    args.putInt(REPORT_EXAMPLE_COUNT, reportExampleCount)
                }
            }
        }
    }

    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportExampleFragmentViewModel::class.java)
    }
    private lateinit var reportSummaryView: RecyclerView
    private lateinit var reportSummaryAdapter: ReportExampleSummaryAdapter
    private var reportExamples: ReportExample? = null
    private var outputSummaryExamples: List<OutputSummaryExample>? = null
    private var job: Job? = null
    private var createdAt: String? = null
    private var position: Int? = null
    private var reportExampleCount: Int? = null

    constructor() : super()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.let { args ->
            reportExamples = args.getParcelable(REPORT_EXAMPLES_ARGUMENT)
            outputSummaryExamples = reportExamples?.output_summary_examples_attributes
            job = args.getParcelable(JOB)
            createdAt = makeSentenceCreatedAt(reportExamples?.created_at)
            position = args.getInt(POSITION)
            reportExampleCount = args.getInt(REPORT_EXAMPLE_COUNT)
        }

        val binding: FragmentReportExamplesBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_report_examples, container, false
        )
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handlers = this

        job?.place?.let { place ->
            if (place.hasEntries || place.workingPlaceShort.isNullOrEmpty()) {
                // 現ユーザーが応募済の物件の場合　フル住所を表示
                viewModel.address.value = place.workingPlace?:"" + place.workingBuilding
            } else {
                // 現ユーザーが未応募の物件の場合　短縮住所を表示
                viewModel.address.value = place.workingPlaceShort
            }
        }
        viewModel.createdAt.value = createdAt
        viewModel.position.value = position
        viewModel.count.value = reportExampleCount
        viewModel.makeSentenceJobKindName(position, reportExampleCount, job?.jobKind?.name)


        //報告箇所の画面生成
        outputSummaryExamples?.let { summary ->
            reportSummaryView = binding.root.findViewById(R.id.report_example_summaries)
            //　画像やコメントの長さによる
            reportSummaryView.setHasFixedSize(false)
            //レイアウトマネージャの設定
            val manager = LinearLayoutManager(activity)
            // 縦スクロールのリスト
            manager.orientation = RecyclerView.VERTICAL
            reportSummaryView.layoutManager = manager
            reportSummaryAdapter = ReportExampleSummaryAdapter(activity!!, summary)
            reportSummaryView.adapter = reportSummaryAdapter
        }
        return binding.root
    }

    override fun onClickPrev(view: View) {
        val activity: ReportExamplesActivity = activity as ReportExamplesActivity
        activity.onClickPrev()
    }

    override fun onClickNext(view: View) {
        val activity: ReportExamplesActivity = activity as ReportExamplesActivity
        activity.onClickNext()
    }

    private fun makeSentenceCreatedAt(created_at: Date?): String {
        val df = SimpleDateFormat("yyyy/MM/dd")
        return ("このお手本の作業報告日：".plus(df.format(created_at)))
    }
}

class ReportExampleSummaryViewHolder(val binding: FragmentReportExamplesSummaryItemBinding) :
    RecyclerView.ViewHolder(binding.root)

class ReportExampleSummaryAdapter(
    val activity: FragmentActivity,
    var summaries: List<OutputSummaryExample>
) : RecyclerView.Adapter<ReportExampleSummaryViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReportExampleSummaryViewHolder {
        val binding = DataBindingUtil.inflate<FragmentReportExamplesSummaryItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.fragment_report_examples_summary_item,
            parent,
            false
        )
        return ReportExampleSummaryViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return summaries.count()
    }

    override fun onBindViewHolder(holder: ReportExampleSummaryViewHolder, position: Int) {
        val view = holder.binding.root
        holder.binding.lifecycleOwner = activity
        holder.binding.viewModel = ReportExampleSummaryItemViewModel(
            activity,
            view,
            summaries[position]
        )
    }
}

class ReportExampleSummaryItemViewModel(
    activity: FragmentActivity,
    view: View,
    val summary: OutputSummaryExample
) : ViewModel() {
    private val imageView: ImageView = view.findViewById(R.id.report_example_summary_item_image)
    private val textView: TextView = view.findViewById(R.id.summary_status)
    val summaryName: MutableLiveData<String> = MutableLiveData()
    val summaryStatus: MutableLiveData<String> = MutableLiveData()
    val summaryComment: MutableLiveData<String> = MutableLiveData()
    val clientCommentVisible = MutableLiveData<Int>(View.GONE)
    val clientComment = MutableLiveData<String>()

    init {
        if (summary.beforeCleaningPhotoUrl.isNullOrBlank()) {
            imageView.setImageDrawable(
                ErikuraApplication.instance.applicationContext.resources.getDrawable(
                    R.drawable.ic_noimage,
                    null
                )
            )
        } else {
            val assetsManager = ErikuraApplication.assetsManager
            assetsManager.fetchImage(activity, summary.beforeCleaningPhotoUrl!!, imageView)
        }
        summaryName.value = summary.place
        val evaluateType = EvaluateType.valueOf(summary.evaluation?.toUpperCase() ?: "UNSELECTED")
        when (evaluateType) {
            EvaluateType.UNSELECTED -> {
                summaryStatus.value = ""
            }
            EvaluateType.GOOD -> {
                summaryStatus.value = ErikuraApplication.instance.getString(evaluateType.resourceId)
                textView.setTextColor(activity.resources.getColor(R.color.black))
            }

            else -> {
                summaryStatus.value = ErikuraApplication.instance.getString(evaluateType.resourceId)
                // 異常ありは文字色を赤
                textView.setTextColor(activity.resources.getColor(R.color.coral))
            }
        }
        summaryComment.value = summary.comment
        clientCommentVisible.value = if (summary.clientComment.isNullOrBlank()) { View.GONE } else { View.VISIBLE }
        clientComment.value = summary.clientComment
    }
}


class ReportExampleFragmentViewModel : ViewModel() {
    val address: MutableLiveData<String> = MutableLiveData()
    val jobKindName: MutableLiveData<String> = MutableLiveData()
    val createdAt: MutableLiveData<String> = MutableLiveData()
    val position = MutableLiveData<Int>()
    val count = MutableLiveData<Int>()
    var prevBtnVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(position) { result.value = btnVisible() }
        result.addSource(count) { result.value = btnVisible() }
    }
    var nextBtnVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(position) { result.value = btnVisible() }
        result.addSource(count) { result.value = btnVisible() }
    }
    var isPrevBtnEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(position) { result.value = prevBtnIsEnable() }
        result.addSource(count) { result.value = prevBtnIsEnable() }
    }
    var isNextBtnEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(position) { result.value = nextBtnIsEnable() }
        result.addSource(count) { result.value = nextBtnIsEnable() }
    }

    private fun prevBtnIsEnable(): Boolean {
        var isEnable = true
        if (position.value == 0) {
            isEnable = false
        }
        return isEnable
    }

    private fun nextBtnIsEnable(): Boolean {
        var isEnable = true
        if (position.value == (count.value?.minus(1))) {
            isEnable = false
        }
        return isEnable
    }

    private fun btnVisible(): Int {
        var viewVisible = View.VISIBLE
        if (position.value == 0 && position.value == (count.value?.minus(1))) {
            viewVisible = View.GONE
        }
        return viewVisible
    }


    fun makeSentenceJobKindName(position: Int?, count: Int?, jobKind: String?) {
        position?.let { p ->
            val outputSumPosition: Int = p.plus(1)
            jobKindName.value = jobKind + " (${outputSumPosition}/${count})"
        }
    }
}

interface ReportExamplesFragmentEventHandlers {
    fun onClickNext(view: View)
    fun onClickPrev(view: View)
}
