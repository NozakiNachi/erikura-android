package jp.co.recruit.erikura.presenters.fragments

import JobUtil
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.FragmentManualButtonBinding

class ManualButtonFragment : BaseJobDetailFragment, ManualButtonFragmentEventHandlers {
    companion object {
        fun newInstance(job: Job?, user: User?): ManualButtonFragment {
            return ManualButtonFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, job, user)
                }
            }
        }
    }

    constructor(): super()

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

