package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentCancelButtonBinding

class CancelButtonFragment(private val job: Job?) : Fragment(), CancelButtonFragmentEventHandler {

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
        // FIXME: キャンセルモーダルの表示
    }

}

interface CancelButtonFragmentEventHandler {
    fun onClickCancel(view: View)
}