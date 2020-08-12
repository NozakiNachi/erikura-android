package jp.co.recruit.erikura.presenters.activities.job

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.DialogJobTitleBinding

class JobTitleDialogFragment: DialogFragment() {
    companion object {
        const val TITLE_ARGUMENT = "title"
        fun newInstance(title: String): JobTitleDialogFragment {
            return JobTitleDialogFragment().also {
                it.arguments = Bundle().also { args ->
                    args.putString(TITLE_ARGUMENT, title)
                }
            }
        }
    }

    private var title: String = ""
    private val viewModel: JobTitleDialogFragmentViewModel by lazy {
        ViewModelProvider(this).get(JobTitleDialogFragmentViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            title = args.getString(TITLE_ARGUMENT) ?: ""
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = Dialog(activity!!)
        // タイトル非表示
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        // フルスクリーン
        dialog.window!!.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        )

        val binding = DataBindingUtil.inflate<DialogJobTitleBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_job_title,
            null,
            false
        )
        binding.lifecycleOwner = activity
        viewModel.setup(title)
        binding.viewModel = viewModel

        dialog.setContentView(binding.root)

        return dialog
    }
}

class JobTitleDialogFragmentViewModel: ViewModel() {
    val title: MutableLiveData<String> = MutableLiveData()

    fun setup(text: String) {
        title.value = text
    }
}