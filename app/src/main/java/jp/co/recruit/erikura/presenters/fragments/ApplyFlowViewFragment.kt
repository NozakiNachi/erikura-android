package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User

class ApplyFlowViewFragment : BaseJobDetailFragment {
    companion object {
        fun newInstance(user: User?): ApplyFlowViewFragment {
            return ApplyFlowViewFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, user)
                }
            }
        }
    }

    constructor(): super()

    private var applyFlowLink: ApplyFlowLinkFragment? = null

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
        applyFlowLink?.refresh(job, user)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_apply_flow_view, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val transaction = childFragmentManager.beginTransaction()
        applyFlowLink = ApplyFlowLinkFragment.newInstance(user)
        transaction.add(R.id.applyFlow_applyFlowLinkFragment, applyFlowLink!!, "applyFlowLink")
        transaction.commitAllowingStateLoss()
    }
}
