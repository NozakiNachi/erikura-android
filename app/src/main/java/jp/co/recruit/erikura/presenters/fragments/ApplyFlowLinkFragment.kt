package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.recruit.erikura.databinding.FragmentApplyFlowLinkBinding
import jp.co.recruit.erikura.presenters.activities.job.ApplyFlowDialogFragment

class ApplyFlowLinkFragment : Fragment(), ApplyFlowLinkFragmentEventHandlers {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentApplyFlowLinkBinding.inflate(inflater, container, false)
        binding.handler = this
        return binding.root
    }

    override fun onClickApplyFlowLink(view: View) {
        val dialog = ApplyFlowDialogFragment()
        dialog.show(childFragmentManager, "ApplyFlow")
    }

}

interface ApplyFlowLinkFragmentEventHandlers {
    fun onClickApplyFlowLink(view: View)
}
