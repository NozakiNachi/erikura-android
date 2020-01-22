package jp.co.recruit.erikura.presenters.activities.job

import jp.co.recruit.erikura.R
import android.app.Dialog
import androidx.fragment.app.DialogFragment
import android.view.WindowManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.databinding.DialogJobTitleBinding


class JobTitleDialogFragment(val title: String): DialogFragment() {
    private val viewModel: JobTitleDialogFragmentViewModel by lazy {
        ViewModelProvider(this).get(JobTitleDialogFragmentViewModel::class.java)
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