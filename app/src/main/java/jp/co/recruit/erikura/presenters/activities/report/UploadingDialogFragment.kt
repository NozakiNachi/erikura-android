package jp.co.recruit.erikura.presenters.activities.report

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MediatorLiveData
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
    var numPhotos: Int
        get() = viewModel.numOfPhotos.value ?: 0
        set(value) {
            viewModel.numOfPhotos.value = value
        }
    var numUploadedPhotos: Int
        get() = viewModel.numOfUploadedPhotos.value ?: 0
        set(value) {
            viewModel.numOfUploadedPhotos.value = value
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

        return builder.create()
    }
}

class UploadingViewModel: ViewModel() {
    val numOfPhotos = MutableLiveData(0)
    val numOfUploadedPhotos = MutableLiveData(0)

    val progress = MediatorLiveData<String>().also { result ->
        result.addSource(numOfPhotos) { result.value = formatProgress() }
        result.addSource(numOfUploadedPhotos) { result.value = formatProgress() }
    }

    private fun formatProgress(): String {
        return ErikuraApplication.instance.getString(R.string.uploading_progress,
            numOfUploadedPhotos.value ?: 0, numOfPhotos.value ?: 0)
    }
}