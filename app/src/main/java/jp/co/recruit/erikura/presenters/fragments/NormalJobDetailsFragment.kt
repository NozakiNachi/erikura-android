package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentNormalJobDetailsBinding
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.lifecycle.MediatorLiveData
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.models.UserSession

class NormalJobDetailsFragment(private val activity: AppCompatActivity, job: Job?, user: User?) : BaseJobDetailFragment(job, user) {
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

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
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
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding = FragmentNormalJobDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel

        viewModel.setup(activity, job, user)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val transaction = childFragmentManager.beginTransaction()
        timeLabel = TimeLabelFragment(job, user)
        jobInfoView = JobInfoViewFragment(job, user)
        thumbnailImage = ThumbnailImageFragment(job, user)
        manualButton = ManualButtonFragment(job, user)
        applyFlowLink = ApplyFlowLinkFragment(job, user)
        jobDetailsView = JobDetailsViewFragment(job, user)
        mapView = MapViewFragment(activity, job, user)
        applyFlowView = ApplyFlowViewFragment(job, user)
        applyButton = ApplyButtonFragment(job, user)
        transaction.add(R.id.jobDetails_timeLabelFragment, timeLabel!!, "timeLabel")
        transaction.add(R.id.jobDetails_jobInfoViewFragment, jobInfoView!!, "jobInfoView")
        transaction.add(R.id.jobDetails_thumbnailImageFragment, thumbnailImage!!, "thumbnailImage")
        transaction.add(R.id.jobDetails_manualButtonFragment, manualButton!!, "manualButton")
        transaction.add(R.id.jobDetails_applyFlowLinkFragment, applyFlowLink!!, "applyFlowLink")
        transaction.add(R.id.jobDetails_jobDetailsViewFragment, jobDetailsView!!, "jobDetailsView")
        transaction.add(R.id.jobDetails_mapViewFragment, mapView!!, "mapView")
        transaction.add(R.id.jobDetails_applyFlowViewFragment, applyFlowView!!, "applyFlowView")
        transaction.add(R.id.jobDetails_applyButtonFragment, applyButton!!, "applyButton")
        transaction.commitAllowingStateLoss()
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_job_detail", params= bundleOf())
        Tracking.viewJobDetails(name= "/jobs/${job?.id?.toString() ?: ""}", title= "タスク詳細画面", jobId= job?.id ?: 0)
    }
}

class NormalJobDetailsFragmentViewModel: ViewModel() {
    val job = MutableLiveData<Job>()
    val user = MutableLiveData<User>()

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
