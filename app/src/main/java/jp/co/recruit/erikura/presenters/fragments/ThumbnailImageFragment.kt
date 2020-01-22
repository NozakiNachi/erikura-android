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
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentThumbnailImageBinding

class ThumbnailImageFragment(private val activity: Activity, val job: Job?) : Fragment() {
    private val viewModel: ThumbnailImageFragmentViewModel by lazy {
        ViewModelProvider(this).get(ThumbnailImageFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentThumbnailImageBinding.inflate(inflater, container, false)
        viewModel.setup(activity, job)
        binding.viewModel = viewModel
        return binding.root
    }

}

class ThumbnailImageFragmentViewModel: ViewModel() {
    val bitmap: MutableLiveData<Bitmap> = MutableLiveData()

    fun setup(activity: Activity, job: Job?) {
        if (job != null){
            // ダウンロード
            job.thumbnailUrl?.let { url ->
                val assetsManager = ErikuraApplication.assetsManager

                assetsManager.fetchImage(activity, url) { result ->
                    activity.runOnUiThread {
                        bitmap.value = result
                    }
                }
            }
        }
    }
}



