package jp.co.recruit.erikura.presenters.fragments

import JobUtil
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.FragmentTimeLabelBinding

class TimeLabelFragment(job: Job?, user: User?) : BaseJobDetailFragment(job, user) {
    private val viewModel: TimeLabelFragmentViewModel by lazy {
        ViewModelProvider(this).get(TimeLabelFragmentViewModel::class.java)
    }

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)
        viewModel.setup(job, user)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = FragmentTimeLabelBinding.inflate(inflater, container, false)
        viewModel.setup(job, user)
        binding.viewModel = viewModel
        return binding.root
    }
}

class TimeLabelFragmentViewModel: ViewModel() {
    val text: MutableLiveData<CharSequence> = MutableLiveData()
    val color: MutableLiveData<Int> = MutableLiveData()

    fun setup(job: Job?, user: User?) {
        val timeLabelType: JobUtil.TimeLabelType = if(job!!.isOwner) {JobUtil.TimeLabelType.OWNED} else {JobUtil.TimeLabelType.SEARCH}
        var (timeLimitText, timeLimitColor) = JobUtil.setupTimeLabel(ErikuraApplication.instance.applicationContext, job, timeLabelType)
        if (!job.isGenderMatched(user) || (job?.banned ?: false)) {
            timeLimitColor = ContextCompat.getColor(ErikuraApplication.instance.applicationContext, R.color.warmGrey)
            timeLimitText = SpannableStringBuilder().apply {
                append("受付終了")
            }
        }
        text.value = timeLimitText
        color.value = timeLimitColor
    }
}