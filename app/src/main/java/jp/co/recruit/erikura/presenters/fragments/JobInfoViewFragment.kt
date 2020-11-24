package jp.co.recruit.erikura.presenters.fragments

import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.FragmentJobInfoViewBinding
import jp.co.recruit.erikura.presenters.activities.job.JobTitleDialogFragment

class JobInfoViewFragment : BaseJobDetailFragment, JobInfoViewFragmentEventHandlers {
    companion object {
        fun newInstance(user: User?): JobInfoViewFragment {
            return JobInfoViewFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, user)
                }
            }
        }
    }
    constructor(): super()

    private val viewModel: JobInfoViewFragmentViewModel by lazy {
        ViewModelProvider(this).get(JobInfoViewFragmentViewModel::class.java)
    }

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
        viewModel.setup(job)
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
        val dialog = JobTitleDialogFragment.newInstance(job?.title ?: "")
        dialog.show(childFragmentManager, "JobTitle")
    }
}

class JobInfoViewFragmentViewModel: ViewModel() {
    val job = MutableLiveData<Job>()
    val title: MutableLiveData<String> = MutableLiveData()
    val workingTime: MutableLiveData<String> = MutableLiveData()
    val fee: MutableLiveData<String> = MutableLiveData()

    val boostVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(job) { job ->
            result.value = if (job?.boost ?: false) { View.VISIBLE } else { View.GONE }
        }
    }
    val wantedVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(job) { job ->
            result.value = if (job?.wanted ?: false) { View.VISIBLE } else { View.GONE }
        }
    }
    val feeLabel = MediatorLiveData<CharSequence>().also { result ->
        result.addSource(job) { job: Job? ->
            val taxLabel = "（税込）"
            val sb = SpannableStringBuilder()
            sb.append(ErikuraApplication.instance.resources.getString(R.string.jobDetails_fee))
            if (job?.boost ?: false) {
                sb.setSpan(
                    AbsoluteSizeSpan(12, true),
                    sb.indexOf(taxLabel), sb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            result.value = sb
        }
    }

    fun setup(job: Job?) {
        this.job.value = job
        job?.let { job ->
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
