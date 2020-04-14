package jp.co.recruit.erikura.presenters.fragments

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.storage.Asset
import jp.co.recruit.erikura.databinding.FragmentManualButtonBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity


class ManualButtonFragment(val job: Job?) : Fragment(), ManualButtonFragmentEventHandlers {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentManualButtonBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.handler = this
        return binding.root
    }

    override fun onClickManualButton(view: View) {
        if(job?.manualUrl != null){
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_job_manual", params= bundleOf())
            Tracking.viewJobDetails(name= "/jobs/manual", title= "マニュアル表示", jobId= job?.id ?: 0)

            val manualUrl = job.manualUrl
            val assetsManager = ErikuraApplication.assetsManager
            assetsManager.fetchAsset(activity!!, manualUrl!!, Asset.AssetType.Pdf) { asset ->
                val intent = Intent(activity, WebViewActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse(asset.url)
                }
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
            }
        }
    }
}

interface ManualButtonFragmentEventHandlers {
    fun onClickManualButton(view: View)
}

