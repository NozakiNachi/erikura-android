package jp.co.recruit.erikura.presenters.fragments

import TextViewUtil
import android.content.Context
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication

import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentTimeLabelBinding
import kotlinx.android.synthetic.*

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
    val color: MutableLiveData<ColorStateList> = MutableLiveData()

    fun setup(job: Job?) {
        var tv = TextView(ErikuraApplication.instance.applicationContext)
        TextViewUtil.setupTimeLabel(tv, ErikuraApplication.instance.applicationContext, job)
        text.value = tv.text
        color.value = tv.textColors
    }
}