package jp.co.recruit.erikura.presenters.fragments


import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.*
import jp.co.recruit.erikura.databinding.FragmentReportedJobDetailsBinding
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentOperatorCommentItemBinding
import jp.co.recruit.erikura.presenters.activities.report.ReportSummaryAdapter
import java.text.SimpleDateFormat
import java.util.*


class ReportedJobDetailsFragment(
    private val activity: AppCompatActivity,
    val job: Job?,
    val user: User
) : Fragment(), ReportedJobDetailsFragmentEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportedJobDetailsFragmentViewModel::class.java)
    }

    private lateinit var reportSummaryAdapter: ReportSummaryAdapter
    private lateinit var additionalOperatorCommentsAdapter: OperatorCommentAdapter
    var fromConfirm = false
    var pictureIndex = 0
    var outputSummaryList: MutableList<OutputSummary> = mutableListOf()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()

        val binding = FragmentReportedJobDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handlers = this

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        reportSummaryAdapter = ReportSummaryAdapter(activity, listOf(), true)
        val reportSummaryView: RecyclerView = activity.findViewById(R.id.reportedJobDetails_reportSummaries)
        reportSummaryView.setHasFixedSize(true)
        reportSummaryView.adapter = reportSummaryAdapter

        additionalOperatorCommentsAdapter = OperatorCommentAdapter(activity, listOf())
        val additionalCommentView: RecyclerView = activity.findViewById(R.id.reportedJobDetails_additionalOperatorComments)
        additionalCommentView.setHasFixedSize(true)
        additionalCommentView.adapter = additionalOperatorCommentsAdapter

        val transaction = childFragmentManager.beginTransaction()
        val timeLabel = TimeLabelFragment(job, user)
        val jobInfoView = JobInfoViewFragment(job)
        val thumbnailImage = ThumbnailImageFragment(job)
//        val reportedJobStatus = ReportedJobStatusFragment(activity, job?.report)
        val reportedJobEditButton = ReportedJobEditButtonFragment(job)
        val reportedJobRemoveButton = ReportedJobRemoveButtonFragment(job)
        val jobDetailsView = JobDetailsViewFragment(job)

        transaction.add(R.id.reportedJobDetails_timeLabelFragment, timeLabel, "timeLabel")
        transaction.add(R.id.reportedJobDetails_jobInfoViewFragment, jobInfoView, "jobInfoView")
        transaction.add(
            R.id.reportedJobDetails_thumbnailImageFragment,
            thumbnailImage,
            "thumbnailImage"
        )
//        transaction.add(R.id.reportedJobDetails_reportedJobStatus, reportedJobStatus, "reportedJobStatus")
        transaction.add(R.id.reportedJobEditButton, reportedJobEditButton, "reportedJobEditButton")
        transaction.add(
            R.id.reportedJobRemoveButton,
            reportedJobRemoveButton,
            "reportedJobRemoveButton"
        )
        transaction.add(
            R.id.reportedJobDetails_jobDetailsViewFragment,
            jobDetailsView,
            "jobDetailsView"
        )
        transaction.commit()

        // reportの再取得
        job?.let {
            Api(activity).reloadReport(job) {
                var report = it
                report.additionalPhotoAsset = createAssets(report.additionalReportPhotoUrl?.toUri()?: Uri.EMPTY)
                report.outputSummaries.forEach { summary ->
                    summary.photoAsset = createAssets(summary.beforeCleaningPhotoUrl?.toUri()?: Uri.EMPTY)
                }
                job.report = report
                setup()
            }
        }
    }

    override fun onClickFavorite(view: View) {
        if (viewModel.favorited.value ?: false) {
            // お気に入り登録処理
            Api(activity).placeFavorite(job?.place?.id ?: 0) {
                viewModel.favorited.value = true
            }
        } else {
            // お気に入り削除処理
            Api(activity).placeFavoriteDelete(job?.place?.id ?: 0) {
                viewModel.favorited.value = false
            }
        }
    }

    private fun setup() {
        if (job != null) {
            // ダウンロード
            job.thumbnailUrl?.let { url ->
                val assetsManager = ErikuraApplication.assetsManager

                assetsManager.fetchImage(activity, url) { result ->
                    activity.runOnUiThread {
                        val bitmapReduced = Bitmap.createScaledBitmap(result, 15, 15, true)
                        val bitmapDraw = BitmapDrawable(bitmapReduced)
                        bitmapDraw.alpha = 150
                        viewModel.bitmapDrawable.value = bitmapDraw
                    }
                }
            }

            // お気に入り状態の取得
            UserSession.retrieve()?.let {
                Api(activity).placeFavoriteShow(job.place?.id ?: 0) {
                    viewModel.favorited.value = it
                }
            }

            job.report?.let {
                // 作業報告ステータスの取得
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
                        viewModel.rejectedCommentVisibility.value = View.GONE
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
                            viewModel.rejectedCommentVisibility.value = View.GONE
                        } else {
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
                        viewModel.rejectedCommentVisibility.value = View.GONE
                        viewModel.buttonVisibility.value = View.VISIBLE
                    }
                }
                viewModel.status.value = str

                // 実施箇所の更新
                reportSummaryAdapter.summaries = it.outputSummaries
                reportSummaryAdapter.notifyDataSetChanged()

                // 作業時間の取得
                val minute = it.workingMinute ?: 0
                viewModel.workingTime.value = if (minute == 0) {
                    ""
                } else {
                    "${minute}分"
                }

                // マニュアル外報告の取得
                val item = it.additionalPhotoAsset ?: MediaItem()
                if (item.contentUri != null) {
                    val imageView: ImageView = activity.findViewById(R.id.reported_job_details_other_image)
                    item.loadImage(activity, imageView)
                    viewModel.otherFormImageVisibility.value = View.VISIBLE
                } else {
                    viewModel.otherFormImageVisibility.value = View.GONE
                }
                val additionalComment = it.additionalComment ?: ""
                viewModel.otherFormComment.value = additionalComment
                if (it.additionalOperatorComments.isNotEmpty()) {
                    additionalOperatorCommentsAdapter.operatorComments = it.additionalOperatorComments
                    additionalOperatorCommentsAdapter.notifyDataSetChanged()
                    viewModel.otherFormEvaluationVisible.value = View.VISIBLE
                }else {
                    viewModel.otherFormEvaluationVisible.value = View.GONE
                }

                // 案件評価の取得
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
        val cursor = activity.contentResolver.query(
            uri,
            arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE
            ),
            MediaStore.MediaColumns.SIZE + ">0",
            arrayOf<String>(),
            "datetaken DESC"
        )
        cursor?.moveToFirst()
        val item = if(cursor != null){MediaItem.from(cursor)}else {MediaItem()}
        return item
    }
}

class ReportedJobDetailsFragmentViewModel : ViewModel() {
    val bitmapDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()
    val favorited: MutableLiveData<Boolean> = MutableLiveData()

    // 作業報告ステータス
    val status: MutableLiveData<SpannableStringBuilder> = MutableLiveData()
    val rejectedComment: MutableLiveData<String> = MutableLiveData()
    val rejectedCommentVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    // 作業報告編集削除ボタンの表示・非表示
    val buttonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    // 作業報告時間
    val workingTime: MutableLiveData<String> = MutableLiveData()

    // マニュアル外報告
    val otherFormImageVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val otherFormComment: MutableLiveData<String> = MutableLiveData()
    val otherFormEvaluationVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)

    // 案件の評価
    val evaluate: MutableLiveData<Boolean> = MutableLiveData()
    val evaluateButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val evaluationComment: MutableLiveData<String> = MutableLiveData()


//    private val imageView: ImageView = view.findViewById(R.id.report_summary_item_image)
//
//    val goodCount: Int get() = job?.report?.operatorLikeCount ?: 0
//    val commentCount: Int get() = job?.report?.operatorCommentsCount ?: 0
//    val goodText: String get() = String.format("%,d件", goodCount)
//    val commentText: String get() = String.format("%,d件", commentCount)
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
//        summaryName.value = summary.place
//        summaryStatus.value = summary.evaluation
//        summaryComment.value = summary.comment
//    }

}

interface ReportedJobDetailsFragmentEventHandlers {
    fun onClickFavorite(view: View)
}


// 運営からの評価コメント
class OperatorCommentItemViewModel(val operatorComment: OperatorComment): ViewModel() {
    val comment: MutableLiveData<String> = MutableLiveData()
    val commentCreatedAt: MutableLiveData<String> = MutableLiveData()

    init {
        comment.value = operatorComment.body
        commentCreatedAt.value = dateToString(operatorComment.createdAt, "yyyy/MM/dd HH:mm")
    }

    private fun dateToString(date: Date, format: String): String {
        val sdf = SimpleDateFormat(format, Locale.JAPAN)
        return sdf.format(date)
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



