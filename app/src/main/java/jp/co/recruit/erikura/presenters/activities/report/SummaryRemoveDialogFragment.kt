package jp.co.recruit.erikura.presenters.activities.report

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.DialogSummaryRemoveBinding
import jp.co.recruit.erikura.presenters.util.setOnSafeClickListener

class SummaryRemoveDialogFragment: DialogFragment() {
    companion object {
        const val INDEX_ARGUMENT = "index"
        fun newInstance(index: Int): SummaryRemoveDialogFragment {
            return SummaryRemoveDialogFragment().also {
                it.arguments = Bundle().also { args ->
                    args.putInt(INDEX_ARGUMENT, index)
                }
            }
        }
    }

    private var index: Int = 0
    private val viewModel: SummaryRemoveViewModel by lazy {
        ViewModelProvider(this).get(SummaryRemoveViewModel::class.java)
    }
    var onClickListener: OnClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            index = args.getInt(INDEX_ARGUMENT, 0)
        }
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

        viewModel.msg.value =  ErikuraApplication.instance.getString(R.string.report_confirm_remove_summary, index+1)

        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)

        binding.root.findViewById<Button>(R.id.summary_remove_button).setOnSafeClickListener {
            onClickListener?.apply {
                onClickRemoveButton()
            }
        }

        return builder.create()
    }

    interface OnClickListener {
        fun onClickRemoveButton()
    }
}

class SummaryRemoveViewModel: ViewModel() {
    val msg: MutableLiveData<String> = MutableLiveData()
}