package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.OutputSummary
import jp.co.recruit.erikura.databinding.FragmentReportedJobDetailsBinding
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.databinding.FragmentImplementationLocationListBinding
import jp.co.recruit.erikura.databinding.FragmentReportSummaryItemBinding
import jp.co.recruit.erikura.presenters.activities.report.ReportSummaryAdapter
import jp.co.recruit.erikura.presenters.activities.report.ReportSummaryItemViewModel
import jp.co.recruit.erikura.presenters.activities.report.ReportSummaryViewHolder


class ReportedJobDetailsFragment(private val activity: AppCompatActivity, val job: Job?, val user: User) : Fragment() {
    private val viewModel: ReportedJobDetailsFragmentViewModel by lazy {
        ViewModelProvider(this).get(ReportedJobDetailsFragmentViewModel::class.java)
    }

    private lateinit var reportSummaryAdapter: ReportSummaryAdapter
    var fromConfirm = false
    var pictureIndex = 0
    var outputSummaryList: MutableList<OutputSummary> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reportSummaryAdapter = ReportSummaryAdapter(activity!!, listOf(), true)
        //リサイクラービューをセット
//        val reportSummaryView: RecyclerView = findViewById(R.id.report_confirm_report_summaries)
//        reportSummaryView.setHasFixedSize(true)
//        reportSummaryView.adapter = reportSummaryAdapter
    }

    private fun setup() {
        val max = (job?.report?.outputSummaries?.lastIndex?: 0) + 1
        viewModel.summaryTitle.value = ErikuraApplication.instance.getString(R.string.report_form_caption, pictureIndex+1, max)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding = FragmentReportedJobDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        viewModel.setup(activity, job, user)
        job?.report?.let {
            val minute = it.workingMinute ?: 0
            viewModel.workingTime.value = if (minute == 0) {
                ""
            } else {
                "${minute}分"
            }
        }
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val transaction = childFragmentManager.beginTransaction()
        //×ボタン
        val timeLabel = TimeLabelFragment(job, user)
        val jobInfoView = JobInfoViewFragment(job)
        val thumbnailImage = ThumbnailImageFragment(job)
        val reportedJobStatus = ReportedJobStatusFragment(activity, job?.report)
        val reportedJobEditButton = ReportedJobEditButtonFragment(job)
        val reportedJobRemoveButton = ReportedJobRemoveButtonFragment(job)
        val jobDetailsView = JobDetailsViewFragment(job)
        //実施箇所表示
        //作業時間
        //マニュアル外の報告
        //マニュアル外の報告画像
        //コメント
        //運営からの評価
        //案件の評価

        //FIXME:　×ボタン
        transaction.add(R.id.jobDetails_timeLabelFragment, timeLabel, "timeLabel")
        transaction.add(R.id.jobDetails_jobInfoViewFragment, jobInfoView, "jobInfoView")
        transaction.add(R.id.jobDetails_thumbnailImageFragment, thumbnailImage, "thumbnailImage")
        transaction.add(R.id.jobDetails_reportedJobStatus, reportedJobStatus, "reportedJobStatus")
        transaction.add(R.id.reportedJobEditButton, reportedJobEditButton, "reportedJobEditButton")
        transaction.add(R.id.reportedJobRemoveButton, reportedJobRemoveButton, "reportedJobRemoveButton")
        transaction.add(R.id.jobDetails_jobDetailsViewFragment, jobDetailsView, "jobDetailsView")
        //FIXME: 実施箇所表示

//        val implementedLocationList: RecyclerView = activity!!.findViewById(R.id.)
//        implementedLocationList.setHasFixedSize(true)
        // Holder(getするやつ)とアダプターを用意する
//        implementedLocationList.adapter = implementLocationAdapter
//        implementedLocationList.addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL))
//        implementedLocationList.addItemDecoration(JobListItemDecorator())
        //FIXME:　作業時間
        //transaction.add(R.id.jobDetails_jobDetailsViewFragment, jobDetailsView, "jobDetailsView")
        //FIXME: マニュアル外の報告
        //transaction.add(R.id.jobDetails_jobDetailsViewFragment, jobDetailsView, "jobDetailsView")
        //FIXME: マニュアル外の報告画像
        //transaction.add(R.id.jobDetails_thumbnailImageFragment, thumbnailImage, "thumbnailImage")
        //FIXME: コメント
        //transaction.add(R.id.jobDetails_thumbnailImageFragment, thumbnailImage, "thumbnailImage")
        //FIXME: 運営からの評価
        //transaction.add(R.id.jobDetails_thumbnailImageFragment, thumbnailImage, "thumbnailImage")
        //FIXME:　案件の評価
        //transaction.add(R.id.jobDetails_thumbnailImageFragment, thumbnailImage, "thumbnailImage")
        transaction.commit()
    }
}

class ReportedJobDetailsFragmentViewModel( activity: Activity, view: View, summary: OutputSummary, summariesCount: Int, position: Int,val job: Job, val timeLabelType: JobUtil.TimeLabelType): ViewModel() {
    val workingTime: MutableLiveData<String> = MutableLiveData()
    val bitmapDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()
    val warningCaption: MutableLiveData<String> = MutableLiveData()
    val warningCaptionVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    private val imageView: ImageView = view.findViewById(R.id.report_summary_item_image)

    val goodCount: Int get() = job?.report?.operatorLikeCount ?: 0
    val commentCount: Int get() = job?.report?.operatorCommentsCount ?: 0
    val goodText: String get() = String.format("%,d件", goodCount)
    val commentText: String get() = String.format("%,d件", commentCount)
    val hasGood: Boolean get() = goodCount > 0
    val hasComment: Boolean get() = commentCount > 0

    val summaryTitle: MutableLiveData<String> = MutableLiveData()
    val summaryName: MutableLiveData<String> = MutableLiveData()
    val summaryStatus: MutableLiveData<String> = MutableLiveData()
    val summaryComment: MutableLiveData<String> = MutableLiveData()

    val goodVisible: Int get() = if (timeLabelType == JobUtil.TimeLabelType.OWNED && hasGood) { View.VISIBLE } else { View.GONE }
    val commentVisible: Int get() = if (timeLabelType == JobUtil.TimeLabelType.OWNED && hasComment) { View.VISIBLE } else { View.GONE }
    init {
        summary.photoAsset?.let {
            it.loadImage(activity, imageView)
        }

        summaryTitle.value = ErikuraApplication.instance.getString(R.string.report_form_caption, position+1, summariesCount)
        summaryName.value = summary.place
        summaryStatus.value = summary.evaluation
        summaryComment.value = summary.comment
    }

    fun setup(activity: Activity, job: Job?, user: User) {
        if (job != null){
            // ダウンロード
            job.thumbnailUrl?.let { url ->
                val assetsManager = ErikuraApplication.assetsManager

                assetsManager.fetchImage(activity, url) { result ->
                    activity.runOnUiThread {
                        val bitmapReduced = Bitmap.createScaledBitmap(result, 15, 15, true)
                        val bitmapDraw = BitmapDrawable(bitmapReduced)
                        bitmapDraw.alpha = 150
                        bitmapDrawable.value = bitmapDraw
                    }
                }
            }

            if (job.isFuture || job.isPast) {
                warningCaption.value = ErikuraApplication.instance.getString(R.string.jobDetails_outOfEntryExpire)
                warningCaptionVisibility.value = View.VISIBLE
            }else if (job.isEntried || (UserSession.retrieve() != null && job.targetGender != null && job.targetGender != user.gender) || job.banned) {
                warningCaption.value = ErikuraApplication.instance.getString(R.string.jobDetails_entryFinished)
                warningCaptionVisibility.value = View.VISIBLE
            }else if (!job.reEntryPermitted) {
                warningCaption.value = ErikuraApplication.instance.getString(R.string.jobDetails_cantEntry)
                warningCaptionVisibility.value = View.VISIBLE
            }else if (UserSession.retrieve() != null && user.holdingJobs >= user.maxJobs) {
                warningCaption.value = ErikuraApplication.instance.getString(R.string.jobDetails_maxEntry, user.maxJobs)
                warningCaptionVisibility.value = View.VISIBLE
            }else {
                warningCaption.value = ""
                warningCaptionVisibility.value = View.GONE
            }

        }
    }
}

