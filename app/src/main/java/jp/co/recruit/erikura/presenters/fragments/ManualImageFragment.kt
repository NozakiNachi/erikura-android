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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.storage.Asset
import jp.co.recruit.erikura.databinding.FragmentManualImageBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity


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
        viewModel.setup(activity!!)
        binding.viewModel = viewModel
        binding.handler = this
        return binding.root
    }

    override fun onClickManualImage(view: View) {
        if(job?.manualUrl != null){
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_job_manual", params= bundleOf())
            Tracking.viewJobDetails(name= "/jobs/manual", title= "マニュアル表示", jobId= job?.id ?: 0)

            val manualUrl = job.manualUrl
            val assetsManager = ErikuraApplication.assetsManager
            Api(activity!!).showProgressAlert()
            assetsManager.fetchAsset(activity!!, manualUrl!!, Asset.AssetType.Pdf) { asset ->
                val intent = Intent(activity, WebViewActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse(asset.url)
                }
                Api(activity!!).hideProgressAlert()
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
            }
        }
    }

}

class ManualImageFragmentViewModel: ViewModel() {
    val bitmap: MutableLiveData<Bitmap> = MutableLiveData()

    fun setup(activity: Activity) {
        val assetsManager = ErikuraApplication.assetsManager
        val url = ErikuraApplication.instance.getString(R.string.jobDetails_manualImageURL)

        assetsManager.fetchImage(activity, url) { result ->
            activity.runOnUiThread {
                bitmap.value = result
            }
        }
    }
}

interface ManualImageFragmentEventHandlers {
    fun onClickManualImage(view: View)
}