package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.EvaluateType
import jp.co.recruit.erikura.business.models.OutputSummary
import jp.co.recruit.erikura.business.models.OutputSummaryExamplesAttributes
import jp.co.recruit.erikura.business.models.ReportExample
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentReportExamplesBinding
import jp.co.recruit.erikura.databinding.FragmentReportExamplesSummaryItemBinding
import jp.co.recruit.erikura.presenters.activities.report.ReportSummaryItemViewModel
import jp.co.recruit.erikura.presenters.util.setOnSafeClickListener
import jp.co.recruit.erikura.presenters.view_models.BaseJobDetailViewModel

class ReportExamplesFragment(reportExample: ReportExample) : Fragment(), ReportExamplesFragmentEventHandlers {

    private lateinit var reportSummaryAdapter: ReportExampleSummaryAdapter
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportExampleFragmentViewModel::class.java)
    }
    private val reportExample: ReportExample = reportExample

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding: FragmentReportExamplesBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_report_examples, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handlers = this

        //FIXME listOf()にList<OutputSummary>（実施箇所を渡す）
        reportSummaryAdapter = ReportExampleSummaryAdapter(activity!!, reportExample.output_summary_examples_attributes).also {
        val reportSummaryView: RecyclerView = binding.root.findViewById(R.id.report_example_summaries)
        reportSummaryView.setHasFixedSize(true)
        reportSummaryView.adapter = reportSummaryAdapter

        return super.onCreateView(inflater, container, savedInstanceState)
        }
    }
}

class ReportExampleSummaryViewHolder(val binding: FragmentReportExamplesSummaryItemBinding) :
    RecyclerView.ViewHolder(binding.root)

class ReportExampleSummaryAdapter(
    val activity: FragmentActivity,
    var summaries: List<OutputSummaryExamplesAttributes>,
    val jobDetails: Boolean = false
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
            position,
            jobDetails
        )
    }
}

// 実施箇所
class ReportExampleSummaryItemViewModel(
    activity: FragmentActivity,
    view: View,
    val summary: OutputSummaryExamplesAttributes,
    summariesCount: Int,
    position: Int,
    jobDetails: Boolean
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
    // FIXME ボタンのvisibilityを用意　生成時のモデルの数とpositionを元に算出
}

interface ReportExamplesFragmentEventHandlers{
}
