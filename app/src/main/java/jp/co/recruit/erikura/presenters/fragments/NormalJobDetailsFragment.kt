package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.databinding.FragmentNormalJobDetailsBinding
import jp.co.recruit.erikura.presenters.view_models.BaseJobDetailViewModel

class NormalJobDetailsFragment : BaseJobDetailFragment {
    companion object {
        fun newInstance(job: Job?, user: User?): NormalJobDetailsFragment {
            val args = Bundle()
            fillArguments(args, job, user)

            return NormalJobDetailsFragment().also {
                it.arguments = args
            }
        }
    }

    private val viewModel: NormalJobDetailsFragmentViewModel by lazy {
        ViewModelProvider(this).get(NormalJobDetailsFragmentViewModel::class.java)
    }
    private var timeLabel: TimeLabelFragment? = null
    private var jobInfoView: JobInfoViewFragment? = null
    private var thumbnailImage: ThumbnailImageFragment? = null
    private var manualButton: ManualButtonFragment? = null
    private var applyFlowLink: ApplyFlowLinkFragment? = null
    private var jobDetailsView: JobDetailsViewFragment? = null
    private var mapView: MapViewFragment? = null
    private var applyFlowView: ApplyFlowViewFragment? = null
    private var applyButton: ApplyButtonFragment? = null
    private var propertyNotesButton: PropertyNotesButtonFragment? = null
    private var reportExamplesButton: ReportExamplesButtonFragment? = null

    constructor() : super()

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
        if (isAdded) {
            activity?.let { activity ->
                viewModel.setup(activity, job, user)
                timeLabel?.refresh(job, user)
                jobInfoView?.refresh(job, user)
                thumbnailImage?.refresh(job, user)
                manualButton?.refresh(job, user)
                applyFlowLink?.refresh(job, user)
                jobDetailsView?.refresh(job, user)
                mapView?.refresh(job, user)
                applyFlowView?.refresh(job, user)
                applyButton?.refresh(job, user)
                propertyNotesButton?.refresh(job, user)
                reportExamplesButton?.refresh(job, user)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        container?.removeAllViews()
        val binding = FragmentNormalJobDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel

        viewModel.setup(activity!!, job, user)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val transaction = childFragmentManager.beginTransaction()
        timeLabel = TimeLabelFragment.newInstance(job, user)
        jobInfoView = JobInfoViewFragment.newInstance(job, user)
        thumbnailImage = ThumbnailImageFragment.newInstance(job, user)
        manualButton = ManualButtonFragment.newInstance(job, user)
        applyFlowLink = ApplyFlowLinkFragment.newInstance(job, user)
        jobDetailsView = JobDetailsViewFragment.newInstance(job, user)
        mapView = MapViewFragment.newInstance(job, user)
        applyFlowView = ApplyFlowViewFragment.newInstance(job, user)
        applyButton = ApplyButtonFragment.newInstance(job, user)
        propertyNotesButton = PropertyNotesButtonFragment.newInstance(job, user)
        reportExamplesButton = ReportExamplesButtonFragment.newInstance(job, user)
        transaction.add(R.id.jobDetails_timeLabelFragment, timeLabel!!, "timeLabel")
        transaction.add(R.id.jobDetails_jobInfoViewFragment, jobInfoView!!, "jobInfoView")
        transaction.add(R.id.jobDetails_thumbnailImageFragment, thumbnailImage!!, "thumbnailImage")
        transaction.add(R.id.jobDetails_manualButtonFragment, manualButton!!, "manualButton")
        transaction.add(R.id.jobDetails_applyFlowLinkFragment, applyFlowLink!!, "applyFlowLink")
        transaction.add(R.id.jobDetails_jobDetailsViewFragment, jobDetailsView!!, "jobDetailsView")
        transaction.add(R.id.jobDetails_mapViewFragment, mapView!!, "mapView")
        transaction.add(R.id.jobDetails_applyFlowViewFragment, applyFlowView!!, "applyFlowView")
        transaction.add(R.id.jobDetails_applyButtonFragment, applyButton!!, "applyButton")
        transaction.add(R.id.jobDetails_propertyNotesButtonFragment, propertyNotesButton!!, "propertyNotesButton")
        transaction.add(R.id.jobDetails_reportExamplesButtonFragment, reportExamplesButton!!, "reportExamplesButton")
        transaction.commitAllowingStateLoss()
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_job_detail", params= bundleOf())
        Tracking.viewJobDetails(name= "/jobs/${job?.id?.toString() ?: ""}", title= "タスク詳細画面", jobId= job?.id ?: 0)
    }
}

class NormalJobDetailsFragmentViewModel: BaseJobDetailViewModel() {
    val nextUpdateSchedule = MediatorLiveData<String>()?.also { result ->
        result.addSource(job) { job ->
            result.value = job.nextUpdateScheduledAt?.let { JobUtils.DateFormats.simple.format(it) }
        }
    }
    val nextUpdateScheduleVisible = MediatorLiveData<Int>()?.also { result ->
        result.addSource(job) { job ->
            // 表示条件は下記の通り
            //   - 他人が応募済みの案件の場合は表示 : inactive 状態
            //   - 応募者なしで終了した案件の場合は表示 : past 状態
            //   - 募集中の案件の場合は非表示 : active 状態
            //   - 自分が応募済みの案件の場合は非表示 : past状態だが、NormalJobDetails は使われないので対象外
            result.value = if (job.isPastOrInactive && job.nextUpdateScheduledAt != null) {
                View.VISIBLE
            }
            else {
                View.GONE
            }
        }
    }

    val applicable = MediatorLiveData<Boolean>()?.also { result ->
        result.addSource(job) { result.value = it.isApplicable(user.value) }
        result.addSource(user) { result.value = job.value?.isApplicable(it) ?: false}
    }

    val bitmapDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()
    val warningCaption = MediatorLiveData<String>().also { result ->
        result.addSource(job) { result.value = decideWarningCaption() }
        result.addSource(user) { result.value = decideWarningCaption() }
    }
    val warningCaptionVisibility = MediatorLiveData<Int>().also { result ->
        result.value = View.GONE
        result.addSource(warningCaption) {
            result.value = if (it.isNullOrBlank()) { View.GONE } else { View.VISIBLE }
        }
    }

    fun setup(activity: Activity, job: Job?, user: User?) {
        this.job.value = job
        this.user.value = user

        if (job != null){
            // ダウンロード
            val thumbnailUrl = if (!job.thumbnailUrl.isNullOrBlank()) {job.thumbnailUrl}else {job.jobKind?.noImageIconUrl?.toString()}
            if (thumbnailUrl.isNullOrBlank()) {
                val drawable = ErikuraApplication.instance.applicationContext.resources.getDrawable(R.drawable.ic_noimage, null)
                val bitmapReduced = Bitmap.createScaledBitmap( drawable.toBitmap(), 15, 15, true)
                val bitmapDraw = BitmapDrawable(bitmapReduced)
                bitmapDraw.alpha = 150
                bitmapDrawable.value = bitmapDraw
            }else {
                val assetsManager = ErikuraApplication.assetsManager
                assetsManager.fetchImage(activity, thumbnailUrl) { result ->
                    activity.runOnUiThread {
                        val bitmapReduced = Bitmap.createScaledBitmap(result, 15, 15, true)
                        val bitmapDraw = BitmapDrawable(bitmapReduced)
                        bitmapDraw.alpha = 150
                        bitmapDrawable.value = bitmapDraw
                    }
                }
            }
        }
    }

    private fun decideWarningCaption(): String? {
        return job.value?.let { job ->
            job.notApplicableReason(user.value)
        }
    }
}
