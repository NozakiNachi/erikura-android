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
import jp.co.recruit.erikura.databinding.FragmentJobDetailsViewBinding
import java.text.SimpleDateFormat
import java.util.*


class JobDetailsViewFragment(val job: Job?) : Fragment() {
    private val viewModel: JobDetailsViewFragmentViewModel by lazy {
        ViewModelProvider(this).get(JobDetailsViewFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentJobDetailsViewBinding.inflate(inflater, container, false)
        viewModel.setup(job)
        binding.viewModel = viewModel
        return binding.root
    }

}

class JobDetailsViewFragmentViewModel: ViewModel() {
    val limit: MutableLiveData<String> = MutableLiveData()
    val tool: MutableLiveData<String> = MutableLiveData()
    val summary: MutableLiveData<String> = MutableLiveData()
    val summaryTitles: MutableLiveData<String> = MutableLiveData()
    val workingPlace: MutableLiveData<String> = MutableLiveData()

    fun setup(job: Job?){
        if (job != null) {
            limit.value = "${dateToString(job.workingStartAt?: Date(), "yyyy/MM/dd HH:mm")} ～ ${dateToString(job.workingFinishAt?: Date(), "yyyy/MM/dd HH:mm")}"
            tool.value = job.tools
            summary.value = job.summary

            var summaryTitleStr = ""
            job.summaryTitles.forEachIndexed { index, s ->
                if (index == job.summaryTitles.lastIndex) {
                    summaryTitleStr += "${s}"
                }else {
                    summaryTitleStr += "${s}、"
                }
            }

            summaryTitles.value = summaryTitleStr
            workingPlace.value = "${job.place?.workingPlace}\n${job.place?.workingBuilding}}"
        }
    }

    private fun dateToString(date: Date, format: String): String {
        val sdf = SimpleDateFormat(format, Locale.JAPAN)
        return sdf.format(date)
    }
}