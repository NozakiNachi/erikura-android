package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.FragmentApplyFlowLinkBinding
import jp.co.recruit.erikura.presenters.activities.job.ApplyFlowDialogFragment
import jp.co.recruit.erikura.presenters.activities.job.PreEntryFlowDialogFragment
import java.util.*

class ApplyFlowLinkFragment : BaseJobDetailFragment, ApplyFlowLinkFragmentEventHandlers {
    companion object {
        fun newInstance(user: User?): ApplyFlowLinkFragment {
            return ApplyFlowLinkFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, user)
                }
            }
        }
    }

    constructor(): super()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentApplyFlowLinkBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.handler = this
        return binding.root
    }

    override fun onClickApplyFlowLink(view: View) {
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_job_guideline", params= bundleOf())
        Tracking.viewJobDetails(name= "/jobs/guideline", title= "応募後の流れを確認画面", jobId= job?.id ?: 0)
        val now = Date()
        if (job?.isPreEntry == true) {
            val dialog = PreEntryFlowDialogFragment()
            dialog.show(childFragmentManager, "PreEntryFlow")
        } else {
            val dialog = ApplyFlowDialogFragment()
            dialog.show(childFragmentManager, "ApplyFlow")
        }
    }
}

interface ApplyFlowLinkFragmentEventHandlers {
    fun onClickApplyFlowLink(view: View)
}
