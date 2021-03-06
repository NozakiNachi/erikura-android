package jp.co.recruit.erikura.presenters.fragments

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ToggleButton
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.*
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentOperatorCommentItemBinding
import jp.co.recruit.erikura.databinding.FragmentReportedJobDetailsBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.report.ReportSummaryAdapter
import jp.co.recruit.erikura.presenters.view_models.BaseJobDetailViewModel

class ReportedJobDetailsFragment : BaseJobDetailFragment, ReportedJobDetailsFragmentEventHandlers {
    companion object {
        fun newInstance(user: User?): ReportedJobDetailsFragment {
            val args = Bundle()
            fillArguments(args, user)

            return ReportedJobDetailsFragment().also {
                it.arguments = args
            }
        }
    }

    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportedJobDetailsFragmentViewModel::class.java)
    }

    private lateinit var reportSummaryAdapter: ReportSummaryAdapter
    private lateinit var additionalOperatorCommentsAdapter: OperatorCommentAdapter
    private var timeLabel: TimeLabelFragment? = null
    private var jobInfoView: JobInfoViewFragment? = null
    private var thumbnailImage: ThumbnailImageFragment? = null
    private var reportedJobEditButton: ReportedJobEditButtonFragment? = null
    private var reportedJobRemoveButton: ReportedJobRemoveButtonFragment? = null
    private var jobDetailsView: JobDetailsViewFragment? = null
    private var propertyNotesButton: PropertyNotesButtonFragment? = null
    private var reportExamplesButton: ReportExamplesButtonFragment? = null

    constructor(): super()

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
        viewModel.job.value = job
        viewModel.user.value = user

        if (isAdded) {
            timeLabel?.refresh(job, user)
            jobInfoView?.refresh(job, user)
            thumbnailImage?.refresh(job, user)
            reportedJobEditButton?.refresh(job, user)
            reportedJobRemoveButton?.refresh(job, user)
            jobDetailsView?.refresh(job, user)
            propertyNotesButton?.refresh(job, user)
            reportExamplesButton?.refresh(job, user)

            activity?.let { activity ->
                fetchReport()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()

        val binding = FragmentReportedJobDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handlers = this

        viewModel.job.value = this.job
        viewModel.user.value = this.user

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        reportSummaryAdapter = ReportSummaryAdapter(activity!!, listOf(), true)
        val reportSummaryView: RecyclerView = activity!!.findViewById(R.id.reportedJobDetails_reportSummaries)
        reportSummaryView.setHasFixedSize(true)
        reportSummaryView.adapter = reportSummaryAdapter

        additionalOperatorCommentsAdapter = OperatorCommentAdapter(activity!!, listOf())
        val additionalCommentView: RecyclerView = activity!!.findViewById(R.id.reportedJobDetails_additionalOperatorComments)
        additionalCommentView.setHasFixedSize(true)
        additionalCommentView.adapter = additionalOperatorCommentsAdapter

        val transaction = childFragmentManager.beginTransaction()
        timeLabel = TimeLabelFragment.newInstance(user)
        jobInfoView = JobInfoViewFragment.newInstance(user)
        thumbnailImage = ThumbnailImageFragment.newInstance(user)
        reportedJobEditButton = ReportedJobEditButtonFragment.newInstance(user)
        reportedJobRemoveButton = ReportedJobRemoveButtonFragment.newInstance(user)
        jobDetailsView = JobDetailsViewFragment.newInstance(user)
        propertyNotesButton = PropertyNotesButtonFragment.newInstance(user)
        reportExamplesButton = ReportExamplesButtonFragment.newInstance(user)
        transaction.add(R.id.reportedJobDetails_timeLabelFragment, timeLabel!!, "timeLabel")
        transaction.add(R.id.reportedJobDetails_jobInfoViewFragment, jobInfoView!!, "jobInfoView")
        transaction.add(R.id.reportedJobDetails_thumbnailImageFragment, thumbnailImage!!, "thumbnailImage")
        transaction.add(R.id.reportedJobEditButton, reportedJobEditButton!!, "reportedJobEditButton")
        transaction.add(R.id.reportedJobRemoveButton, reportedJobRemoveButton!!, "reportedJobRemoveButton")
        transaction.add(R.id.reportedJobDetails_jobDetailsViewFragment, jobDetailsView!!, "jobDetailsView")
        transaction.add(R.id.jobDetails_propertyNotesButtonFragment, propertyNotesButton!!, "propertyNotesButton")
        transaction.add(R.id.jobDetails_reportExamplesButtonFragment, reportExamplesButton!!, "reportExamplesButton")
        transaction.commitAllowingStateLoss()

        fetchReport()
    }

    override fun onStart() {
        super.onStart()
        // ?????????????????????????????????????????????
        Tracking.logEvent(event= "view_job_detail", params= bundleOf())
        Tracking.viewJobDetails(name= "/jobs/${job?.id?.toString() ?: ""}", title= "?????????????????????", jobId= job?.id ?: 0)
    }

    override fun onClickFavorite(view: View) {
        job?.place?.id?.let { placeId ->
            // ??????????????????????????????????????????
            val favorited = viewModel.favorited.value ?: false

            val favoriteButton: ToggleButton = this.view?.findViewById(R.id.favorite_button)!!
            // ?????????????????????????????????????????????????????????
            favoriteButton.isEnabled = false
            val api = Api(activity!!)
            val errorHandler: (List<String>?) -> Unit = { messages ->
                api.displayErrorAlert(messages)
                favoriteButton.isEnabled = true
            }
            if (favorited) {
                // ??????????????????????????????????????????????????????
                api.placeFavorite(placeId, onError = errorHandler) {
                    viewModel.favorited.value = true
                    favoriteButton.isEnabled = true
                }
            }
            else {
                // ???????????????????????????
                api.placeFavoriteDelete(placeId, onError = errorHandler) {
                    viewModel.favorited.value = false
                    favoriteButton.isEnabled = true
                }
            }
        }
    }

    override fun onClickTransitionWebModal(view: View) {
        // WEB???????????????????????????????????????
        BaseActivity.currentActivity?.let { activity ->
            TransitionWebModal.transitionWebModal(view, activity, job, user)
        }
    }

    private fun fetchReport() {
        // report????????????
        job?.let { job ->
            // ????????????????????????????????????????????????????????????????????????
            if (job?.report?.deleted ?: true) {
                setup()
            }
            else {
                activity?.let {
                    Api(it).reloadReport(job) { r ->
                        var report = r
                        report.additionalPhotoAsset = if (report.additionalReportPhotoUrl.isNullOrEmpty()){
                            null
                        }
                        else{
                            createAssets(report.additionalReportPhotoUrl?.toUri()?: Uri.EMPTY)
                        }
                        report.outputSummaries.forEach { summary ->
                            summary.photoAsset = createAssets(summary.beforeCleaningPhotoUrl?.toUri()?: Uri.EMPTY)
                        }
                        job.report = report
                        setup()
                    }
                }
            }
        }
    }

    private fun setup() {
        if (job != null) {
            // ??????????????????
            val thumbnailUrl = if (!job?.thumbnailUrl.isNullOrBlank()) {job?.thumbnailUrl} else {job?.jobKind?.noImageIconUrl?.toString()}
            if (thumbnailUrl.isNullOrBlank()) {
                val drawable = ErikuraApplication.instance.applicationContext.resources.getDrawable(R.drawable.ic_noimage, null)
                val bitmapReduced = Bitmap.createScaledBitmap( drawable.toBitmap(), 15, 15, true)
                val bitmapDraw = BitmapDrawable(bitmapReduced)
                bitmapDraw.alpha = 150
                viewModel.bitmapDrawable.value = bitmapDraw
            }else {
                val assetsManager = ErikuraApplication.assetsManager
                activity?.let { activity ->
                    assetsManager.fetchImage(activity, thumbnailUrl) { result ->
                        activity.runOnUiThread {
                            val bitmapReduced = Bitmap.createScaledBitmap(result, 15, 15, true)
                            val bitmapDraw = BitmapDrawable(bitmapReduced)
                            bitmapDraw.alpha = 150
                            viewModel.bitmapDrawable.value = bitmapDraw
                        }
                    }
                }
            }

            // ??????????????????????????????
            if (Api.isLogin) {
                activity?.let { activity ->
                    Api(activity).placeFavoriteShow(job?.place?.id ?: 0) {
                        viewModel.favorited.value = it
                    }
                }
            }
            // ???????????????????????????????????????
            job?.let { job ->
                if ((job.goodExamplesCount ?: 0) > 0) {
                    viewModel.reportExamplesButtonVisibility.value = View.VISIBLE
                } else {
                    viewModel.reportExamplesButtonVisibility.value = View.GONE
                }
            }

            job?.report?.let {
                // ????????????????????????????????????
                var str = SpannableStringBuilder()
                when (it.status) {
                    ReportStatus.Accepted -> {
                        str.append(ErikuraApplication.instance.getString(R.string.report_status_confirmed))
                        str.setSpan(
                            ForegroundColorSpan(Color.rgb(25, 197, 183)),
                            0,
                            str.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        if (it.acceptComment == null) {
                            viewModel.acceptedCommentVisibility.value = View.GONE
                            viewModel.rejectedCommentVisibility.value = View.GONE
                        } else {
                            viewModel.acceptedComment.value = it.acceptComment ?: ""
                            viewModel.acceptedCommentVisibility.value = View.VISIBLE
                            viewModel.rejectedCommentVisibility.value = View.GONE
                        }
                        viewModel.buttonVisibility.value = View.GONE
                    }
                    ReportStatus.Rejected -> {
                        str.append(ErikuraApplication.instance.getString(R.string.report_status_reject))
                        str.setSpan(
                            ForegroundColorSpan(Color.RED),
                            0,
                            str.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        if (it.rejectComment.isNullOrBlank()) {
                            viewModel.acceptedCommentVisibility.value = View.GONE
                            viewModel.rejectedCommentVisibility.value = View.GONE
                        } else {
                            viewModel.acceptedCommentVisibility.value = View.GONE
                            viewModel.rejectedComment.value = it.rejectComment ?: ""
                            viewModel.rejectedCommentVisibility.value = View.VISIBLE
                        }
                        viewModel.buttonVisibility.value = View.VISIBLE
                    }
                    ReportStatus.Unconfirmed -> {
                        str.append(ErikuraApplication.instance.getString(R.string.report_status_unconfirmed))
                        str.setSpan(
                            ForegroundColorSpan(Color.rgb(137, 133, 129)),
                            0,
                            str.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        viewModel.acceptedCommentVisibility.value = View.GONE
                        viewModel.rejectedCommentVisibility.value = View.GONE
                        viewModel.buttonVisibility.value = View.VISIBLE
                    }
                }
                viewModel.status.value = str

                // ?????????????????????
                var summaries = mutableListOf<OutputSummary>()
                it.outputSummaries.forEach {summary ->
                    if (!summary.willDelete) {
                        summaries.add(summary)
                    }
                }
                reportSummaryAdapter.summaries = summaries
                reportSummaryAdapter.notifyDataSetChanged()

                // ?????????????????????
                val minute = it.workingMinute ?: 0
                viewModel.workingTime.value = if (minute == 0) {
                    ""
                } else {
                    "${minute}???"
                }

                // ?????????????????????????????????
                val item = it.additionalPhotoAsset ?: MediaItem()
                if (item.contentUri != null) {
                    activity?.let { activity ->
                        val imageView: ImageView = activity.findViewById(R.id.reported_job_details_other_image)
                        item.loadImageFromString(activity, imageView)
                    }
                    viewModel.otherFormImageVisibility.value = View.VISIBLE
                } else {
                    viewModel.otherFormImageVisibility.value = View.GONE
                }
                val additionalComment = it.additionalComment ?: ""
                viewModel.otherFormComment.value = additionalComment
                // ??????????????????????????????????????????????????????
                if (it.additionalOperatorLikes) {
                    viewModel.otherFormGoodCount.value = "1???"
                    viewModel.otherFormGoodCountVisibility.value = View.VISIBLE
                }else {
                    viewModel.otherFormGoodCountVisibility.value = View.GONE
                }
                if (it.additionalOperatorComments.isNotEmpty()) {
                    additionalOperatorCommentsAdapter.operatorComments = it.additionalOperatorComments
                    additionalOperatorCommentsAdapter.notifyDataSetChanged()
                    viewModel.otherFormEvaluationVisibility.value = View.VISIBLE
                    viewModel.otherFormCommentCount.value = "${it.additionalOperatorComments.count()}???"
                    viewModel.otherFormCommentCountVisibility.value = View.VISIBLE
                }else {
                    viewModel.otherFormEvaluationVisibility.value = View.GONE
                    viewModel.otherFormCommentCountVisibility.value = View.GONE
                }

                // ?????????????????????
                val evaluation = it.evaluation ?: ""
                when (evaluation) {
                    "good" ->
                        viewModel.evaluate.value = true
                    "bad" ->
                        viewModel.evaluate.value = false
                }
                viewModel.evaluateButtonVisibility.value = if (evaluation == "unanswered") {
                    View.GONE
                } else {
                    View.VISIBLE
                }
                val comment = it.comment ?: ""
                viewModel.evaluationComment.value = comment
            }
        }
    }

    private fun createAssets(uri: Uri): MediaItem {
        val item = MediaItem(id = 0, mimeType = "", size = 0, contentUri = uri)
        return item
    }
}

class ReportedJobDetailsFragmentViewModel : BaseJobDetailViewModel() {
    val bitmapDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()
    val favorited: MutableLiveData<Boolean> = MutableLiveData()

    // ???????????????????????????
    val status: MutableLiveData<SpannableStringBuilder> = MutableLiveData()
    val rejectedComment: MutableLiveData<String> = MutableLiveData()
    val rejectedCommentVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val acceptedComment: MutableLiveData<String> = MutableLiveData()
    val acceptedCommentVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    // ??????????????????????????????????????????????????????
    val buttonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    // ??????????????????
    val workingTime: MutableLiveData<String> = MutableLiveData()

    // ????????????????????????
    val otherFormImageVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val otherFormComment: MutableLiveData<String> = MutableLiveData()
    val otherFormEvaluationVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val otherFormGoodCount: MutableLiveData<String> = MutableLiveData()
    val otherFormGoodCountVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val otherFormCommentCount: MutableLiveData<String> = MutableLiveData()
    val otherFormCommentCountVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    // ???????????????
    val evaluate: MutableLiveData<Boolean> = MutableLiveData()
    val evaluateButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val evaluationComment: MutableLiveData<String> = MutableLiveData()

    // ???????????????
    val reportExamplesButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)


//    private val imageView: ImageView = view.findViewById(R.id.report_summary_item_image)
//
//    val goodCount: Int get() = job?.report?.operatorLikeCount ?: 0
//    val commentCount: Int get() = job?.report?.operatorCommentsCount ?: 0
//    val goodText: String get() = String.format("%,d???", goodCount)
//    val commentText: String get() = String.format("%,d???", commentCount)
//    val hasGood: Boolean get() = goodCount > 0
//    val hasComment: Boolean get() = commentCount > 0
//
//    val summaryTitle: MutableLiveData<String> = MutableLiveData()
//    val summaryName: MutableLiveData<String> = MutableLiveData()
//    val summaryStatus: MutableLiveData<String> = MutableLiveData()
//    val summaryComment: MutableLiveData<String> = MutableLiveData()
//
//    val goodVisible: Int get() = if (timeLabelType == JobUtil.TimeLabelType.OWNED && hasGood) { View.VISIBLE } else { View.GONE }
//    val commentVisible: Int get() = if (timeLabelType == JobUtil.TimeLabelType.OWNED && hasComment) { View.VISIBLE } else { View.GONE }
//    init {
//        summary.photoAsset?.let {
//            it.loadImage(activity, imageView)
//        }
//
//        summaryTitle.value = ErikuraApplication.instance.getString(R.string.report_form_caption, position+1, summariesCount)
//        summaryName.value = summary.workingPlace
//        summaryStatus.value = summary.evaluation
//        summaryComment.value = summary.comment
//    }

}

interface ReportedJobDetailsFragmentEventHandlers {
    fun onClickFavorite(view: View)
    fun onClickTransitionWebModal(view: View)
}


// ?????????????????????????????????
class OperatorCommentItemViewModel(val operatorComment: OperatorComment): ViewModel() {
    val comment: MutableLiveData<String> = MutableLiveData()
    val commentCreatedAt: MutableLiveData<String> = MutableLiveData()

    init {
        comment.value = operatorComment.body
        commentCreatedAt.value = JobUtils.DateFormats.simple.format(operatorComment.createdAt)
    }
}

class OperatorCommentViewHolder(val binding: FragmentOperatorCommentItemBinding) : RecyclerView.ViewHolder(binding.root)

class OperatorCommentAdapter(
    val activity: FragmentActivity,
    var operatorComments: List<OperatorComment>
    ): RecyclerView.Adapter<OperatorCommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OperatorCommentViewHolder {
        val binding = DataBindingUtil.inflate<FragmentOperatorCommentItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.fragment_operator_comment_item,
            parent,
            false
        )
        return OperatorCommentViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return operatorComments.count()
    }

    override fun onBindViewHolder(holder: OperatorCommentViewHolder, position: Int) {
        holder.binding.lifecycleOwner = activity
        holder.binding.viewModel = OperatorCommentItemViewModel(operatorComments[position])
    }
}



