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
import jp.co.recruit.erikura.business.models.ReportExample
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentReportExamplesBinding
import jp.co.recruit.erikura.databinding.FragmentReportSummaryItemBinding
import jp.co.recruit.erikura.presenters.activities.report.ReportSummaryItemViewModel
import jp.co.recruit.erikura.presenters.util.setOnSafeClickListener
import jp.co.recruit.erikura.presenters.view_models.BaseJobDetailViewModel

class ReportExamplesFragment : Fragment(), ReportExamplesFragmentEventHandlers {

    private lateinit var reportSummaryAdapter: ReportSummaryAdapter
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportExampleFragmentViewModel::class.java)
    }

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
        reportSummaryAdapter = ReportSummaryAdapter(activity!!, listOf()).also {
        val reportSummaryView: RecyclerView = binding.root.findViewById(R.id.report_confirm_report_summaries)
        reportSummaryView.setHasFixedSize(true)
        reportSummaryView.adapter = reportSummaryAdapter

        return super.onCreateView(inflater, container, savedInstanceState)
        }
    }
}

class ReportSummaryViewHolder(val binding: FragmentReportSummaryItemBinding) :
    RecyclerView.ViewHolder(binding.root)

class ReportSummaryAdapter(
    val activity: FragmentActivity,
    var summaries: List<OutputSummary>,
    val jobDetails: Boolean = false
) : RecyclerView.Adapter<ReportSummaryViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportSummaryViewHolder {
        val binding = DataBindingUtil.inflate<FragmentReportSummaryItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.fragment_report_summary_item,
            parent,
            false
        )

        return ReportSummaryViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return summaries.count()
    }

    override fun onBindViewHolder(holder: ReportSummaryViewHolder, position: Int) {
        val view = holder.binding.root
        holder.binding.lifecycleOwner = activity
        holder.binding.viewModel = ReportSummaryItemViewModel(
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
class ReportSummaryItemViewModel(
    activity: FragmentActivity,
    view: View,
    val summary: OutputSummary,
    summariesCount: Int,
    position: Int,
    jobDetails: Boolean
) : ViewModel() {
    private val imageView: ImageView = view.findViewById(R.id.report_summary_item_image)
    val summaryName: MutableLiveData<String> = MutableLiveData()
    val summaryStatus: MutableLiveData<String> = MutableLiveData()
    val summaryComment: MutableLiveData<String> = MutableLiveData()
    init {
        summary.photoAsset?.let {
            if (summary.beforeCleaningPhotoUrl != null){
                it.loadImageFromString(activity, imageView)
            }else {
                it.loadImage(activity, imageView)
            }
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
