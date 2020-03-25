package jp.co.recruit.erikura.presenters.fragments

import android.app.ActivityOptions
import android.content.Intent
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
import jp.co.recruit.erikura.databinding.FragmentReportedJobEditButtonBinding
import jp.co.recruit.erikura.presenters.activities.report.ReportConfirmActivity
import java.util.*


class ReportedJobEditButtonFragment(private val job: Job?) : Fragment(), ReportedJobEditButtonFragmentEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportedJobEditButtonViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentReportedJobEditButtonBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handler = this

        job?.let { job ->
            if (job.entry!!.limitAt!! <= Date() && job.report?.isRejected?: false) {
                viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.edit_rejected_report)
            }else {
                viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.edit_report)
            }
        }

        return binding.root
    }

    override fun onClickReportedJobEditButton(view: View) {
        if(job?.isReportEditable?: false) {
            val intent = Intent(activity,ReportConfirmActivity::class.java)
            intent.putExtra("job",job)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
         }
    }
}

class ReportedJobEditButtonViewModel: ViewModel() {
    val buttonText: MutableLiveData<String> = MutableLiveData()
}

interface ReportedJobEditButtonFragmentEventHandlers {
    fun onClickReportedJobEditButton(view: View)
}
