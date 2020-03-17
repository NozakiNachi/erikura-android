package jp.co.recruit.erikura.presenters.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentReportedJobRemoveButtonBinding
import jp.co.recruit.erikura.presenters.activities.job.CancelDialogFragment
import java.util.*


class ReportedJobRemoveButtonFragment(private val job: Job?) : Fragment(), ReportedJobRemoveButtonFragmentEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportedJobRemoveButtonViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentReportedJobRemoveButtonBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handler = this

        job?.let { job ->
            if (job.entry!!.limitAt!! <= Date() && job.report?.isRejected?: false) {
                viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.cancel_report_entry)
            }else {
                viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.remove_report)
            }
        }

        return binding.root
    }

    override fun onClickReportedJobRemoveButton(view: View) {
        job?.let { job ->
            if (job.entry!!.limitAt!! <= Date() && job.report?.isRejected?: false) {
                val dialog = CancelDialogFragment(job)
                dialog.show(childFragmentManager, "Cancel")
            }else {
                val dialog = ReportedJobRemoveDialogFragment(job)
                dialog.show(childFragmentManager, "Remove")
            }
        }
    }
}

class ReportedJobRemoveButtonViewModel: ViewModel() {
    val buttonText: MutableLiveData<String> = MutableLiveData()
}

interface ReportedJobRemoveButtonFragmentEventHandlers {
    fun onClickReportedJobRemoveButton(view: View)
}
