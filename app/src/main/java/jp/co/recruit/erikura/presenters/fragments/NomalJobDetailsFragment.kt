package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentNomalJobDetailsBinding


class NomalJobDetailsFragment(private val activity: Activity, val job: Job?) : Fragment() {

    lateinit var bitmapImage: Bitmap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = FragmentNomalJobDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val transaction = childFragmentManager.beginTransaction()
        val timeLabel = TimeLabelFragment(job)
        val jobInfoView = JobInfoViewFragment(job)
        //val thumbnailImage = ThumbnailImageFragment(bitmapImage)
        transaction.add(R.id.jobDetails_timeLabelFragment, timeLabel, "timeLabel")
        transaction.add(R.id.jobDetails_jobInfoViewFragment, jobInfoView, "jobInfoView")
        //transaction.add(R.id.jobDetails_thumbnailImageFragment, thumbnailImage, "thumbnailImage")
        transaction.commit()
    }

}
