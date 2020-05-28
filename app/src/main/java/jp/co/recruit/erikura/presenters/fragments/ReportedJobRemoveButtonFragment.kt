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
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentReportedJobRemoveButtonBinding
import jp.co.recruit.erikura.presenters.activities.job.CancelDialogFragment
import java.util.*


class ReportedJobRemoveButtonFragment : BaseJobDetailFragment, ReportedJobRemoveButtonFragmentEventHandlers {
    companion object {
        fun newInstance(job: Job?, user: User?): ReportedJobRemoveButtonFragment {
            return ReportedJobRemoveButtonFragment().also {
                it.arguments = Bundle().also { args ->
                    fillArguments(args, job, user)
                }
            }
        }
    }

    constructor(): super()

    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportedJobRemoveButtonViewModel::class.java)
    }

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)

        job?.let { job ->
            if (isExpiredAndRejected()) {
                // 納期切れ、かつ差し戻し案件の場合は、「応募と報告をキャンセルする」ボタンを表示します
                viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.cancel_report_entry)
            }else {
                // それ以外は作業報告の削除ボタンを表示します
                viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.remove_report)
            }
        }
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
            if (isExpiredAndRejected()) {
                // 納期切れ、かつ差し戻し案件の場合は、「応募と報告をキャンセルする」ボタンを表示します
                viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.cancel_report_entry)
            }else {
                // それ以外は作業報告の削除ボタンを表示します
                viewModel.buttonText.value = ErikuraApplication.instance.getString(R.string.remove_report)
            }
        }

        return binding.root
    }

    override fun onClickReportedJobRemoveButton(view: View) {
        job?.let { job ->
            if (job.isReportEditable) {
                if (isExpiredAndRejected()) {
                    val dialog = CancelDialogFragment(job)
                    dialog.show(childFragmentManager, "Cancel")
                } else {
                    val dialog = ReportedJobRemoveDialogFragment(job)
                    dialog.show(childFragmentManager, "Remove")
                }
            }
            else {
                Api(activity!!).displayErrorAlert(listOf(getString(R.string.jobDetails_overLimit)))
            }
        }
    }

    private fun isExpiredAndRejected(): Boolean {
        val expired = job?.entry?.isExpired() ?: false
        val rejected = job?.report?.isRejected ?: false
        return expired && rejected
    }

}

class ReportedJobRemoveButtonViewModel: ViewModel() {
    val buttonText: MutableLiveData<String> = MutableLiveData()
}

interface ReportedJobRemoveButtonFragmentEventHandlers {
    fun onClickReportedJobRemoveButton(view: View)
}
