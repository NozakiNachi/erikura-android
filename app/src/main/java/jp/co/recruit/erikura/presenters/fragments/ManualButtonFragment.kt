package jp.co.recruit.erikura.presenters.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.recruit.erikura.business.models.Job
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
            val termsOfServiceURLString = job.manualUrl
            val intent = Intent(activity, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(termsOfServiceURLString)
            }
            startActivity(intent)
        }
    }
}

interface ManualButtonFragmentEventHandlers {
    fun onClickManualButton(view: View)
}

