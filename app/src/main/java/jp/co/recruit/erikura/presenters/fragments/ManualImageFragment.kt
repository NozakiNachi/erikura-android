package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.storage.Asset
import jp.co.recruit.erikura.databinding.FragmentManualImageBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
import okhttp3.internal.closeQuietly
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class ManualImageFragment(private val job: Job?) : Fragment(), ManualImageFragmentEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ManualImageFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentManualImageBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handler = this
        viewModel.setup(activity!!, binding.root)
        return binding.root
    }

    override fun onClickManualImage(view: View) {
        if(job?.manualUrl != null){
            activity?.let { activity ->
                JobUtil.openManual(activity, job!!)
            }
        }
    }

}

class ManualImageFragmentViewModel: ViewModel() {
    val bitmap: MutableLiveData<Bitmap> = MutableLiveData()

    fun setup(activity: Activity, root: View) {
        val imageView: ImageView = root.findViewById(R.id.manual_image)
        val assetsManager = ErikuraApplication.assetsManager
        val url = ErikuraApplication.instance.getString(R.string.jobDetails_manualImageURL)

        assetsManager.fetchImage(activity, url, imageView)
    }
}

interface ManualImageFragmentEventHandlers {
    fun onClickManualImage(view: View)
}