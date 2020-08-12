package jp.co.recruit.erikura.presenters.activities.job

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import jp.co.recruit.erikura.R
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import jp.co.recruit.erikura.databinding.DialogApplyFlowBinding


class ApplyFlowDialogFragment: DialogFragment(), ApplyFlowDialogFragmentEventHandlers {
    companion object {
        fun newInstance(): ApplyFlowDialogFragment {
            return ApplyFlowDialogFragment().also {
                it.arguments = Bundle().also { args ->
                    // 引数があれば args に設定する
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogApplyFlowBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_apply_flow,
            null,
            false
        )
        binding.lifecycleOwner = activity
        binding.handlers = this

        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)
        return builder.create()
    }

    override fun onTouch(view: View) {
        dismiss()
    }
}

interface ApplyFlowDialogFragmentEventHandlers {
    fun onTouch(view: View)
}
