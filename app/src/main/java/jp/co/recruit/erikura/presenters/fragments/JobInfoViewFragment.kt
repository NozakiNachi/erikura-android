package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentJobInfoViewBinding
import jp.co.recruit.erikura.presenters.activities.job.JobTitleDialogFragment


class JobInfoViewFragment(val job: Job?) : Fragment(), JobInfoViewFragmentEventHandlers {
    private val viewModel: JobInfoViewFragmentViewModel by lazy {
        ViewModelProvider(this).get(JobInfoViewFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = FragmentJobInfoViewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        viewModel.setup(job)
        binding.viewModel = viewModel
        binding.handler = this
        return binding.root
    }

    override fun onClickTitle(view: View) {
        val dialog = JobTitleDialogFragment(job?.title ?: "")
        dialog.show(childFragmentManager, "JobTitle")
    }
}

class JobInfoViewFragmentViewModel: ViewModel() {
    val title: MutableLiveData<String> = MutableLiveData()
    val workingTime: MutableLiveData<String> = MutableLiveData()
    val fee: MutableLiveData<String> = MutableLiveData()

    fun setup(job: Job?) {
        if(job != null) {
            title.value = job.title
            workingTime.value = "${job.workingTime}分"
            val feeStr = String.format("%,d", job.fee)
            fee.value = "${feeStr}円"
        }
    }
}

interface JobInfoViewFragmentEventHandlers {
    fun onClickTitle(view: View)
}
