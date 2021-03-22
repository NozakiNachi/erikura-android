package jp.co.recruit.erikura.presenters.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentReportedJobEditButtonBinding
import jp.co.recruit.erikura.presenters.activities.report.ReportConfirmActivity
import org.apache.commons.lang.builder.ToStringBuilder
import java.lang.RuntimeException
import java.util.*

class ReportedJobEditButtonFragment : BaseJobDetailFragment, ReportedJobEditButtonFragmentEventHandlers {
    companion object {
        fun newInstance(user: User?): ReportedJobEditButtonFragment {
            return ReportedJobEditButtonFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, user)
                }
            }
        }
    }

    constructor(): super()

    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportedJobEditButtonViewModel::class.java)
    }

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)

        job?.let { job ->
            job.entry?.let { entry ->
                entry.limitAt?.let { limitAt ->
                    viewModel.enabled.value = true
                    if (limitAt <= Date() && job.report?.isRejected?: false) {
                        viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.edit_rejected_report)
                    }else {
                        viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.edit_report)
                    }
                } ?: run {
                    viewModel.enabled.value = false
                    viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.edit_report)
                    FirebaseCrashlytics.getInstance().setCustomKey("job", ToStringBuilder.reflectionToString(job))
                    FirebaseCrashlytics.getInstance().setCustomKey("entry", ToStringBuilder.reflectionToString(entry))
                    FirebaseCrashlytics.getInstance().recordException(RuntimeException("limitAt is null: job = ${ToStringBuilder.reflectionToString(job)}, entry=${ToStringBuilder.reflectionToString(entry)}"))
                }
            } ?: run {
                viewModel.enabled.value = false
                viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.edit_report)
                FirebaseCrashlytics.getInstance().setCustomKey("job", ToStringBuilder.reflectionToString(job))
                FirebaseCrashlytics.getInstance().recordException(RuntimeException("job entry is null: ${ToStringBuilder.reflectionToString(job)}"))
            }
        }
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
            job.entry?.let { entry ->
                entry.limitAt?.let { limitAt ->
                    viewModel.enabled.value = true
                    if (job.entry!!.limitAt!! <= Date() && job.report?.isRejected?: false) {
                        viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.edit_rejected_report)
                    }else {
                        viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.edit_report)
                    }
                } ?: run {
                    viewModel.enabled.value = false
                    viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.edit_report)
                    FirebaseCrashlytics.getInstance().setCustomKey("job", ToStringBuilder.reflectionToString(job))
                    FirebaseCrashlytics.getInstance().setCustomKey("entry", ToStringBuilder.reflectionToString(entry))
                    FirebaseCrashlytics.getInstance().recordException(RuntimeException("limitAt is null: job = ${ToStringBuilder.reflectionToString(job)}, entry=${ToStringBuilder.reflectionToString(entry)}"))
                }
            } ?: run {
                viewModel.enabled.value = false
                viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.edit_report)
                FirebaseCrashlytics.getInstance().setCustomKey("job", ToStringBuilder.reflectionToString(job))
                FirebaseCrashlytics.getInstance().recordException(RuntimeException("job entry is null: ${ToStringBuilder.reflectionToString(job)}"))
            }
        }

        return binding.root
    }

    override fun onClickReportedJobEditButton(view: View) {
        if(job?.isReportEditable?: false) {
            val intent = Intent(activity,ReportConfirmActivity::class.java)
            ErikuraApplication.instance.currentJob = job
            startActivity(intent)
        }
        else {
            Api(activity!!).displayErrorAlert(listOf(getString(R.string.jobDetails_overLimit)))
        }
    }
}

class ReportedJobEditButtonViewModel: ViewModel() {
    val buttonText: MutableLiveData<String> = MutableLiveData()
    val enabled: MutableLiveData<Boolean> = MutableLiveData(true)
}

interface ReportedJobEditButtonFragmentEventHandlers {
    fun onClickReportedJobEditButton(view: View)
}
