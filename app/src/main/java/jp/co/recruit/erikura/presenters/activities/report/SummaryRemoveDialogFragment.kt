package jp.co.recruit.erikura.presenters.activities.report

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.OutputSummary
import jp.co.recruit.erikura.databinding.DialogSummaryRemoveBinding

class SummaryRemoveDialogFragment(private val job: Job, private val index: Int): DialogFragment(), SummaryRemoveEventHandlers {
    private val viewModel: SummaryRemoveViewModel by lazy {
        ViewModelProvider(this).get(SummaryRemoveViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogSummaryRemoveBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_summary_remove,
            null,
            false
        )
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handlers = this

        viewModel.msg.value =  ErikuraApplication.instance.getString(R.string.report_confirm_remove_summary, index+1)

        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)
        return builder.create()
    }
    override fun onClickRemoveButton(view: View) {
        job.report?.let {
            var outputSummaryList: MutableList<OutputSummary> = mutableListOf()
            outputSummaryList = it.outputSummaries.toMutableList()
            outputSummaryList.removeAt(index)
            it.outputSummaries = outputSummaryList
        }
    }
}

class SummaryRemoveViewModel: ViewModel() {
    val msg: MutableLiveData<String> = MutableLiveData()
}

interface SummaryRemoveEventHandlers {
    fun onClickRemoveButton(view: View)
}