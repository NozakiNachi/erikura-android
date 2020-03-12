package jp.co.recruit.erikura.presenters.activities.report

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.DialogUploadFailedBinding

class UploadFailedDialogFragment: DialogFragment() {
    var onClickListener: OnClickListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogUploadFailedBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_upload_failed,
            null,
            false
        )
        binding.lifecycleOwner = activity

        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)

        binding.root.findViewById<Button>(R.id.upload_failed_retry_button).setOnClickListener {
            onClickListener?.apply {
                onClickRetryButton()
            }
        }

        binding.root.findViewById<Button>(R.id.upload_failed_remove_button).setOnClickListener {
            onClickListener?.apply {
                onClickRemoveButton()
            }
        }

        return builder.create()
    }

    interface OnClickListener {
        fun onClickRetryButton()
        fun onClickRemoveButton()
    }

}