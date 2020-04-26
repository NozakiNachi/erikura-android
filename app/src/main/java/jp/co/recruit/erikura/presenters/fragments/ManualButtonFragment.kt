package jp.co.recruit.erikura.presenters.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.storage.Asset
import jp.co.recruit.erikura.databinding.FragmentManualButtonBinding
import okhttp3.internal.closeQuietly
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


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
            activity?.let { activity ->
                JobUtil.openManual(activity, job!!)
            }
        }
    }
}

interface ManualButtonFragmentEventHandlers {
    fun onClickManualButton(view: View)
}

