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
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.CancelReason
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.DialogCancelBinding
import java.util.*

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

        binding.root.setOnTouchListener { view, event ->
            if (view != null) {
                val imm: InputMethodManager = activity!!.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
            return@setOnTouchListener false
        }

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
        job?.let {
            if (job.entry?.limitAt?: Date() > Date()) {
                Api(activity!!).cancel(job, reasonCode, comment) {
                    // ページ参照のトラッキングの送出
                    Tracking.logEvent(event= "view_job_cacel_finish", params= bundleOf())
                    Tracking.viewJobDetails(name= "/entries/cancelled/${job?.id ?:0}", title= "キャンセル完了画面", jobId= job?.id ?: 0)

                    val intent= Intent(activity, JobDetailsActivity::class.java)
                    intent.putExtra("job", job)
                    startActivity(intent)
                }
            }else {
                val errorMessages = mutableListOf(ErikuraApplication.instance.getString(R.string.jobDetails_overLimit))
                Api(activity!!).displayErrorAlert(errorMessages)
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