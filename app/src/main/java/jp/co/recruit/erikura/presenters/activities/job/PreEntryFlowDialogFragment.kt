package jp.co.recruit.erikura.presenters.activities.job

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.DialogPreEntryFlowBinding


class PreEntryFlowDialogFragment: DialogFragment(), PreEntryFlowDialogFragmentEventHandlers {
    companion object {
        fun newInstance(): PreEntryFlowDialogFragment {
            return PreEntryFlowDialogFragment().also {
                it.arguments = Bundle().also { args ->
                    // 引数があれば args に設定する
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogPreEntryFlowBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_pre_entry_flow,
            null,
            false
        )
        binding.lifecycleOwner = activity
        binding.handlers = this

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
