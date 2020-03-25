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
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.models.UserSession


class NormalJobDetailsFragment(private val activity: AppCompatActivity, val job: Job?, val user: User) : Fragment() {
    private val viewModel: NormalJobDetailsFragmentViewModel by lazy {
        ViewModelProvider(this).get(NormalJobDetailsFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding = FragmentNormalJobDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        viewModel.setup(activity, job, user)
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val transaction = childFragmentManager.beginTransaction()
        val timeLabel = TimeLabelFragment(job, user)
        val jobInfoView = JobInfoViewFragment(job)
        val thumbnailImage = ThumbnailImageFragment(job)
        val manualButton = ManualButtonFragment(job)
        val applyFlowLink = ApplyFlowLinkFragment()
        val jobDetailsView = JobDetailsViewFragment(job)
        val mapView = MapViewFragment(activity, job)
        val applyFlowView = ApplyFlowViewFragment()
        val applyButton = ApplyButtonFragment(job, user)
        transaction.add(R.id.jobDetails_timeLabelFragment, timeLabel, "timeLabel")
        transaction.add(R.id.jobDetails_jobInfoViewFragment, jobInfoView, "jobInfoView")
        transaction.add(R.id.jobDetails_thumbnailImageFragment, thumbnailImage, "thumbnailImage")
        transaction.add(R.id.jobDetails_manualButtonFragment, manualButton, "manualButton")
        transaction.add(R.id.jobDetails_applyFlowLinkFragment, applyFlowLink, "applyFlowLink")
        transaction.add(R.id.jobDetails_jobDetailsViewFragment, jobDetailsView, "jobDetailsView")
        transaction.add(R.id.jobDetails_mapViewFragment, mapView, "mapView")
        transaction.add(R.id.jobDetails_applyFlowViewFragment, applyFlowView, "applyFlowView")
        transaction.add(R.id.jobDetails_applyButtonFragment, applyButton, "applyButton")
        transaction.commit()
    }

}

class NormalJobDetailsFragmentViewModel: ViewModel() {
    val bitmapDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()
    val warningCaption: MutableLiveData<String> = MutableLiveData()
    val warningCaptionVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    fun setup(activity: Activity, job: Job?, user: User) {
        if (job != null){
            // ダウンロード
            val thumbnailUrl = if (!job.thumbnailUrl.isNullOrBlank()) {job.thumbnailUrl}else {job.jobKind?.noImageIconUrl.toString()}
            thumbnailUrl?.let {
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
