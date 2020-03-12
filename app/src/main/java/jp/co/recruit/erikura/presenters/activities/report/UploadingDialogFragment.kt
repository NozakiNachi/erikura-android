package jp.co.recruit.erikura.presenters.activities.report

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.DialogUploadingBinding

class UploadingDialogFragment: DialogFragment() {
    private val viewModel: UploadingViewModel by lazy {
        ViewModelProvider(this).get(UploadingViewModel::class.java)
    }
    var numPhotos = 0
    var numUploadedPhotos = 0
        set(value) {
            field = value
            viewModel.progress.value = ErikuraApplication.instance.getString(R.string.uploading_progress, field, numPhotos)
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogUploadingBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_uploading,
            null,
            false
        )
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel

        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)

        viewModel.progress.value = ErikuraApplication.instance.getString(R.string.uploading_progress, numUploadedPhotos, numPhotos)

        return builder.create()
    }
}

class UploadingViewModel: ViewModel() {
    val progress: MutableLiveData<String> = MutableLiveData()
}