package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
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

        fun newInstance(outputSummaryExamplesAttributes: List<OutputSummaryExamplesAttributes>?): ReportExamplesFragment {
            return ReportExamplesFragment().also {
                it.arguments = Bundle().also { args ->
                    args.putParcelableArrayList(OUTPUT_SUMMARY_EXAMPLES_ARGUMENT,
                        ArrayList(outputSummaryExamplesAttributes ?: listOf())
                    )
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

    constructor() : super()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.let { args ->
            output_summary_examples_attributes = args.getParcelableArrayList<OutputSummaryExamplesAttributes>(OUTPUT_SUMMARY_EXAMPLES_ARGUMENT)?.toList() ?: listOf()
        }

        container?.removeAllViews()
        val binding: FragmentReportExamplesBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_report_examples, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handlers = this

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
            else -> {
                summaryStatus.value = ErikuraApplication.instance.getString(evaluateType.resourceId)
            }
        }
        summaryComment.value = summary.comment
    }
}


class ReportExampleFragmentViewModel : ViewModel() {
}

interface ReportExamplesFragmentEventHandlers{
}
