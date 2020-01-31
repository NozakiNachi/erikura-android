package jp.co.recruit.erikura.presenters.fragments

import JobUtil
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication

import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentTimeLabelBinding

class TimeLabelFragment(val job: Job?) : Fragment() {
    private val viewModel: TimeLabelFragmentViewModel by lazy {
        ViewModelProvider(this).get(TimeLabelFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = FragmentTimeLabelBinding.inflate(inflater, container, false)
        viewModel.setup(job)
        binding.viewModel = viewModel
        return binding.root
    }
}

class TimeLabelFragmentViewModel: ViewModel() {
    val text: MutableLiveData<CharSequence> = MutableLiveData()
    val color: MutableLiveData<Int> = MutableLiveData()

    fun setup(job: Job?) {
        val (timeLimitText, timeLimitColor) = JobUtil.setupTimeLabel(ErikuraApplication.instance.applicationContext, job)
        text.value = timeLimitText
        color.value = timeLimitColor
    }
}