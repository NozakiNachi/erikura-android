package jp.co.recruit.erikura.presenters.activities.job

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import jp.co.recruit.erikura.R
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.business.models.CancelReason
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.DialogCancelBinding

class CancelDialogFragment(private val job: Job?): DialogFragment(), CancelDialogFragmentEventHandlers {
    private val viewModel: CancelDialogFragmentViewModel by lazy {
        ViewModelProvider(this).get(CancelDialogFragmentViewModel::class.java)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogCancelBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_cancel,
            null,
            false
        )
        binding.lifecycleOwner = activity
        viewModel.setup(activity!!)
        binding.viewModel = viewModel
        binding.handlers = this


        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)
        return builder.create()
    }

    override fun onReasonSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        viewModel.reasonsItems.value?.let {
            viewModel.reasonSelected = position
            if (position == viewModel.reasonsItems.value?.lastIndex) {
                viewModel.reasonVisibility.value = View.VISIBLE
            }else {
                viewModel.reasonVisibility.value = View.GONE
            }
        }
    }

    override fun onClickCancel(view: View) {
        val reasonCode: Int = if (viewModel.reasonSelected == viewModel.reasonsItems.value?.lastIndex) {0}else {viewModel.reasonSelected}
        val comment: String = if(reasonCode == 0) {viewModel.reasonText.value?:""} else {""}
        if (job != null) {
            Api(activity!!).cancel(job, reasonCode, comment) {
                val intent= Intent(activity, JobDetailsActivity::class.java)
                intent.putExtra("job", job)
                startActivity(intent)
                activity!!.finish()

            }
        }
    }
}

class CancelDialogFragmentViewModel: ViewModel() {
    val reasonText: MutableLiveData<String> = MutableLiveData()
    val reasonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val reasons: MutableLiveData<List<CancelReason>> = MutableLiveData()
    var reasonSelected: Int = 0

    val reasonsItems = MediatorLiveData<List<CancelReasonItem>>().also { result ->
        result.addSource(reasons) {
            val items = mutableListOf<CancelReasonItem>()
            it.forEach { reason -> items.add(CancelReasonItem.Item(reason)) }
            items.add(CancelReasonItem.Other)
            result.value = items
        }
    }

    val isEnabledCancelButton = MediatorLiveData<Boolean>().also { result ->
        result.addSource(reasonVisibility) { result.value = isValid() }
        result.addSource(reasonText) { result.value = isValid() }
    }

    fun setup(activity: Activity) {
        Api(activity).cancelReasons {
            reasons.value = it
        }
    }

    private fun isValid(): Boolean {
        return !(reasonVisibility.value == View.VISIBLE && reasonText.value.isNullOrBlank())
    }
}

interface CancelDialogFragmentEventHandlers {
    fun onReasonSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
    fun onClickCancel(view: View)
}

sealed class CancelReasonItem(val label: String, val value: CancelReason?) {
    object Other: CancelReasonItem("その他", null)
    class Item(cancelReason: CancelReason) : CancelReasonItem(cancelReason.content, cancelReason)

    override fun toString(): String {
        return label
    }
}