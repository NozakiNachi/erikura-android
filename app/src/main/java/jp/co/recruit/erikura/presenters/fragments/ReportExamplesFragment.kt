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
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentReportExamplesBinding
import jp.co.recruit.erikura.databinding.FragmentReportExamplesSummaryItemBinding
import jp.co.recruit.erikura.presenters.activities.report.ReportSummaryItemViewModel
import jp.co.recruit.erikura.presenters.util.setOnSafeClickListener
import jp.co.recruit.erikura.presenters.view_models.BaseJobDetailViewModel
import java.util.*

class ReportExamplesFragment : Fragment, ReportExamplesFragmentEventHandlers {
    companion object {
        const val OUTPUT_SUMMARY_EXAMPLES_ARGUMENT = "report_example_output_summary"
        const val JOB = "job"
        const val CREATED_AT = "created_at"
        const val POSITION = "position"
        const val REPORTEXAMPLECOUNT = "report_example_count"

        fun newInstance(outputSummaryExamplesAttributes: List<OutputSummaryExamplesAttributes>?, job: Job, created_at: String?, position: Int, reportExampleCount: Int): ReportExamplesFragment {
            return ReportExamplesFragment().also {
                it.arguments = Bundle().also { args ->
                    args.putParcelableArrayList(OUTPUT_SUMMARY_EXAMPLES_ARGUMENT,
                        ArrayList(outputSummaryExamplesAttributes ?: listOf())
                    )
                    args.putParcelable(JOB, job)
                    args.putString(CREATED_AT, created_at)
                    args.putInt(POSITION, position)
                    args.putInt(REPORTEXAMPLECOUNT, reportExampleCount)
                }
            }
        }
    }

    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportExampleFragmentViewModel::class.java)
    }
    private lateinit var reportSummaryView: RecyclerView
    private lateinit var reportSummaryAdapter: ReportExampleSummaryAdapter
    private var output_summary_examples_attributes: List<OutputSummaryExamplesAttributes>? = null
    private var job: Job? = null
    private var created_at: String? = null
    private var position: Int? = null
    private var reportExampleCount: Int? = null

    constructor() : super()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.let { args ->
            output_summary_examples_attributes = args.getParcelableArrayList<OutputSummaryExamplesAttributes>(OUTPUT_SUMMARY_EXAMPLES_ARGUMENT)?.toList() ?: listOf()
            job = args.getParcelable(JOB)
            created_at = args.getString(CREATED_AT)
            position = args.getInt(POSITION)
            reportExampleCount = args.getInt(REPORTEXAMPLECOUNT)
        }

        val binding: FragmentReportExamplesBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_report_examples, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handlers = this

        viewModel.jobKindName.value = job?.jobKind?.name
        job?.placeId?.let{place_id ->
            Api(activity!!).place(place_id) { place ->
                if (place.hasEntries || place.workingPlaceShort.isNullOrEmpty()) {
                    // 現ユーザーが応募済の物件の場合　フル住所を表示
                    viewModel.address.value = place.workingPlace + place.workingBuilding
                } else {
                    // 現ユーザーが未応募の物件の場合　短縮住所を表示
                    viewModel.address.value = place.workingPlaceShort
                }
            }
        }
        viewModel.createdAt.value = created_at
        viewModel.btnVisible(position, reportExampleCount)


        //報告箇所の画面生成
        output_summary_examples_attributes?.let { summary ->
            reportSummaryView = binding.root.findViewById(R.id.report_example_summaries)
            reportSummaryView.setHasFixedSize(true)
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
}

class ReportExampleSummaryViewHolder(val binding: FragmentReportExamplesSummaryItemBinding) :
    RecyclerView.ViewHolder(binding.root)

class ReportExampleSummaryAdapter(
    val activity: FragmentActivity,
    var summaries: List<OutputSummaryExamplesAttributes>
) : RecyclerView.Adapter<ReportExampleSummaryViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportExampleSummaryViewHolder {
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
            summaries[position],
            summaries.count(),
            position
        )
    }
}

// 実施箇所
class ReportExampleSummaryItemViewModel(
    activity: FragmentActivity,
    view: View,
    val summary: OutputSummaryExamplesAttributes,
    summariesCount: Int,
    position: Int
) : ViewModel() {
    private val imageView: ImageView = view.findViewById(R.id.report_example_summary_item_image)
    private val textView: TextView = view.findViewById(R.id.summary_status)
    val summaryName: MutableLiveData<String> = MutableLiveData()
    val summaryStatus: MutableLiveData<String> = MutableLiveData()
    val summaryComment: MutableLiveData<String> = MutableLiveData()
    init {
        if (summary.beforeCleaningPhotoUrl.isNullOrBlank()) {
            imageView.setImageDrawable(ErikuraApplication.instance.applicationContext.resources.getDrawable(R.drawable.ic_noimage, null))
        }
        else {
            val assetsManager = ErikuraApplication.assetsManager
            assetsManager.fetchImage(activity, summary.beforeCleaningPhotoUrl!!, imageView)
        }
        summaryName.value = summary.place
        val evaluateType = EvaluateType.valueOf(summary.evaluation?.toUpperCase()?: "UNSELECTED")
        when(evaluateType) {
            EvaluateType.UNSELECTED -> {
                summaryStatus.value = ""
            }
            EvaluateType.UNANSWERED -> {
                summaryStatus.value = ""
            }
            EvaluateType.GOOD -> {
                summaryStatus.value = ErikuraApplication.instance.getString(evaluateType.resourceId)
                // 黒
                textView.setTextColor(activity.resources.getColor(R.color.black))
            }

            else -> {
                summaryStatus.value = ErikuraApplication.instance.getString(evaluateType.resourceId)
                // 異常ありは赤
                textView.setTextColor(activity.resources.getColor(R.color.coral))
            }
        }
        summaryComment.value = summary.comment
    }
}


class ReportExampleFragmentViewModel : ViewModel() {
    val address: MutableLiveData<String> = MutableLiveData()
    val jobKindName: MutableLiveData<String> = MutableLiveData()
    val createdAt: MutableLiveData<String> = MutableLiveData()
    val position = MutableLiveData<Int>()
    val count = MutableLiveData<Int>()
    var prevBtnVisibility: MediatorLiveData<Int> = MediatorLiveData()
    var nextBtnVisibility: MediatorLiveData<Int> = MediatorLiveData()

    fun btnVisible(position: Int?, count: Int?) {
        if (position != 0) {
            prevBtnVisibility.value = View.VISIBLE
        }
        else {
            prevBtnVisibility.value = View.GONE
        }
        if (position != (count?.minus(1))) {
            nextBtnVisibility.value = View.VISIBLE
        }
        else {
            nextBtnVisibility.value = View.GONE
        }
    }
}

interface ReportExamplesFragmentEventHandlers{
//    fun onClickNext(view: View)
//    fun onClickPrev(view: View)
}
