package jp.co.recruit.erikura.presenters.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.FragmentPreEntryFlowViewBinding
import java.text.SimpleDateFormat


class PreEntryFlowViewFragment() : BaseJobDetailFragment() {
    companion object {
        fun newInstance(user: User?): PreEntryFlowViewFragment {
            return PreEntryFlowViewFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, user)
                }
            }
        }
    }

    private var applyFlowLink: ApplyFlowLinkFragment? = null
    private val viewModel: PreEntryFlowViewFragmentViewModel by lazy {
        ViewModelProvider(this).get(PreEntryFlowViewFragmentViewModel::class.java)
    }

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
        applyFlowLink?.refresh(job, user)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentPreEntryFlowViewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        job?.workingStartAt?.let { startAt ->
            viewModel.workingDay.value = (JobUtil.getWorkingDay(startAt) + ErikuraApplication.instance.getString(R.string.jobDetails_preEntryFlowCaption1))
        }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val transaction = childFragmentManager.beginTransaction()
        applyFlowLink = ApplyFlowLinkFragment.newInstance(user)
        transaction.add(R.id.applyFlow_applyFlowLinkFragment, applyFlowLink!!, "applyFlowLink")
        transaction.commitAllowingStateLoss()
    }
}

class PreEntryFlowViewFragmentViewModel: ViewModel() {
    val workingDay: MutableLiveData<String> = MutableLiveData()
}