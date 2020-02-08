package jp.co.recruit.erikura.presenters.activities.job

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import jp.co.recruit.erikura.R
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.DialogStartBinding


class StartDialogFragment(private val job: Job?) : DialogFragment() {
    private val viewModel: StartDialogFragmentViewModel by lazy {
        ViewModelProvider(this).get(StartDialogFragmentViewModel::class.java)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogStartBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_start,
            null,
            false
        )
        binding.lifecycleOwner = activity
        viewModel.setup(job)
        binding.viewModel = viewModel

        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)
        return builder.create()
    }
}

class StartDialogFragmentViewModel: ViewModel() {
    val caption: MutableLiveData<String> = MutableLiveData()
    val reportPlaces: MutableLiveData<String> = MutableLiveData()
    val reportPlacesVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    fun setup(job: Job?) {
        if (job != null) {
            if(!job.summaryTitles.isNullOrEmpty()) {
                caption.value = ErikuraApplication.instance.getString(R.string.applyDialog_caption2Pattern1)
                var summaryTitleStr = ""
                job.summaryTitles.forEachIndexed { index, s ->
                    summaryTitleStr += "(${index+1}) ${s}　"
                }
                reportPlaces.value = summaryTitleStr
                reportPlacesVisibility.value = View.VISIBLE
            }else {
                caption.value = ErikuraApplication.instance.getString(R.string.applyDialog_caption2Pattern2)
                reportPlacesVisibility.value = View.GONE
            }
        }
    }
}