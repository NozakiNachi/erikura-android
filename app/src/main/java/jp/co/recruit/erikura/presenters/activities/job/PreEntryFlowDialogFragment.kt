package jp.co.recruit.erikura.presenters.activities.job

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.DialogPreEntryFlowBinding
import java.util.*


class PreEntryFlowDialogFragment: DialogFragment(), PreEntryFlowDialogFragmentEventHandlers {
    companion object {
        const val JOB_ARGUMENT = "job"
        fun newInstance(job: Job?): PreEntryFlowDialogFragment {
            return PreEntryFlowDialogFragment().also {
                it.arguments = Bundle().also { args ->
                    args.putParcelable(JOB_ARGUMENT, job)
                }
            }
        }
    }

    private var job: Job? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.let { args ->
            job = args.getParcelable(JOB_ARGUMENT)
        }
        val workingDay = ErikuraApplication.instance.getWorkingDay(job?.workingStartAt?: Date())
        val binding = DataBindingUtil.inflate<DialogPreEntryFlowBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_pre_entry_flow,
            null,
            false
        )
        binding.lifecycleOwner = activity
        binding.handlers = this
        binding.root.findViewById<TextView>(R.id.preEntryFlowDialog_step3Explain1).setText(
            (workingDay + ErikuraApplication.instance.getString(R.string.preEntryFlowDialog_step3Explain1))
        )
        binding.root.findViewById<TextView>(R.id.preEntryFlowDialog_step3Explain2).setText(
            String.format(ErikuraApplication.instance.getString(R.string.preEntryFlowDialog_step3Explain2), workingDay)
        )
        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)
        return builder.create()
    }

    override fun onClickClose(view: View) {
        dialog?.dismiss()
    }
}

interface PreEntryFlowDialogFragmentEventHandlers {
    fun onClickClose(view: View)
}
