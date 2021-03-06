package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.FragmentCancelButtonBinding
import jp.co.recruit.erikura.presenters.activities.job.CancelDialogFragment

class CancelButtonFragment : BaseJobDetailFragment, CancelButtonFragmentEventHandler {
    companion object {
        fun newInstance(user: User?): CancelButtonFragment {
            val args = Bundle()
            fillArguments(args, user)

            return CancelButtonFragment().also {
                it.arguments = args
            }
        }
    }

    constructor(): super()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentCancelButtonBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.handlers = this
        return binding.root
    }

    override fun onClickCancel(view: View) {
        val dialog = CancelDialogFragment.newInstance(job)
        dialog.show(childFragmentManager, "Cancel")
    }
}

interface CancelButtonFragmentEventHandler {
    fun onClickCancel(view: View)
}