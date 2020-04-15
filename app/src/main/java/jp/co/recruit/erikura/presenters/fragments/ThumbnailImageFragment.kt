package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentThumbnailImageBinding


class ThumbnailImageFragment(val job: Job?) : Fragment() {
    private val viewModel: ThumbnailImageFragmentViewModel by lazy {
        ViewModelProvider(this).get(ThumbnailImageFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentThumbnailImageBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel

        viewModel.setup(activity!!, binding.root, job)
        return binding.root
    }

}

class ThumbnailImageFragmentViewModel: ViewModel() {
    val bitmap: MutableLiveData<Bitmap> = MutableLiveData()

    fun setup(activity: Activity, root: View, job: Job?) {
        if (job != null){
            // ダウンロード
            val imageView: ImageView = root.findViewById(R.id.thumbnailImage_image)
            val thumbnailUrl = if (!job.thumbnailUrl.isNullOrBlank()) {job.thumbnailUrl}else {job.jobKind?.noImageIconUrl?.toString()}
            if (thumbnailUrl.isNullOrBlank()) {
                imageView.setImageDrawable(ErikuraApplication.instance.applicationContext.resources.getDrawable(R.drawable.ic_noimage, null))
            }else {
                val assetsManager = ErikuraApplication.assetsManager
                assetsManager.fetchImage(activity, thumbnailUrl, imageView)
            }
        }
    }
}



