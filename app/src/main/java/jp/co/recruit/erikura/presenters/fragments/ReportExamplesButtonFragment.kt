package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.FragmentReportExamplesButtonBinding

class ReportExamplesButtonFragment : BaseJobDetailFragment, ReportExamplesButtonFragmentEventHandlers {
    companion object {
        fun newInstance(job: Job?, user: User?): ReportExamplesButtonFragment {
            return ReportExamplesButtonFragment().also {
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
        val binding = FragmentReportExamplesButtonBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.handler = this
        return binding.root
    }

    override fun onClickReportExamples(view: View) {
        job?.let { job ->
            JobUtil.openReportExample(activity!!, job)
        }
    }
}

interface ReportExamplesButtonFragmentEventHandlers {
    fun onClickReportExamples(view: View)
}