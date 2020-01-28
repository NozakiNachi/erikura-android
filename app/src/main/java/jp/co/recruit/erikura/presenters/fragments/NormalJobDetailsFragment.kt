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


class NormalJobDetailsFragment(private val activity: AppCompatActivity, val job: Job?) : Fragment() {
    private val viewModel: NormalJobDetailsFragmentViewModel by lazy {
        ViewModelProvider(this).get(NormalJobDetailsFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding = FragmentNormalJobDetailsBinding.inflate(inflater, container, false)
        viewModel.setup(activity, job)
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val transaction = childFragmentManager.beginTransaction()
        val timeLabel = TimeLabelFragment(job)
        val jobInfoView = JobInfoViewFragment(job)
        val thumbnailImage = ThumbnailImageFragment(activity, job)
        val manualButton = ManualButtonFragment(job)
        val applyFlowLink = ApplyFlowLinkFragment()
        val jobDetailsView = JobDetailsViewFragment(job)
        val mapView = MapViewFragment(activity, job)
        val applyFlowView = ApplyFlowViewFragment()
        transaction.add(R.id.jobDetails_timeLabelFragment, timeLabel, "timeLabel")
        transaction.add(R.id.jobDetails_jobInfoViewFragment, jobInfoView, "jobInfoView")
        transaction.add(R.id.jobDetails_thumbnailImageFragment, thumbnailImage, "thumbnailImage")
        transaction.add(R.id.jobDetails_manualButtonFragment, manualButton, "manualButton")
        transaction.add(R.id.jobDetails_applyFlowLinkFragment, applyFlowLink, "applyFlowLink")
        transaction.add(R.id.jobDetails_jobDetailsViewFragment, jobDetailsView, "jobDetailsView")
        transaction.add(R.id.jobDetails_mapViewFragment, mapView, "mapView")
        transaction.add(R.id.jobDetails_applyFlowViewFragment, applyFlowView, "applyFlowView")
        transaction.commit()
    }

}

class NormalJobDetailsFragmentViewModel: ViewModel() {
    val bitmapDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()

    fun setup(activity: Activity, job: Job?) {
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

        }
    }
}
