package jp.co.recruit.erikura.presenters.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentReportedJobRemoveButtonBinding


class ReportedJobRemoveButtonFragment(val job: Job?) : Fragment(), ReportedJobRemoveButtonFragmentEventHandlers {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentReportedJobRemoveButtonBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.handler = this
        return binding.root
    }

    override fun onClickReportedJobRemoveButton(view: View) {

    }
}

interface ReportedJobRemoveButtonFragmentEventHandlers {
    fun onClickReportedJobRemoveButton(view: View)
}
