package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job

class ApplyFlowViewFragment(val job: Job?) : Fragment() {

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
        val applyFlowLink = ApplyFlowLinkFragment(job)
        transaction.add(R.id.applyFlow_applyFlowLinkFragment, applyFlowLink, "applyFlowLink")
        transaction.commitAllowingStateLoss()
    }
}
