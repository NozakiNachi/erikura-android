package jp.co.recruit.erikura.presenters.activities.job

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import jp.co.recruit.erikura.R
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.databinding.DialogApplyBinding

class ApplyDialogFragment: DialogFragment(), ApplyDialogFragmentEventHandlers {
    private val viewModel: ApplyDialogFragmentViewModel by lazy {
        ViewModelProvider(this).get(ApplyDialogFragmentViewModel::class.java)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogApplyBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_apply,
            null,
            false
        )
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handlers = this

        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)
        return builder.create()
    }
}

class ApplyDialogFragmentViewModel: ViewModel() {

}

interface ApplyDialogFragmentEventHandlers {

}