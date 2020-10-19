package jp.co.recruit.erikura.presenters.activities.report

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.DialogStorageAccessConfirmBinding
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.TextView


class StorageAccessConfirmDialogFragment() : DialogFragment(), StorageAccessConfirmEventHandlers {
    companion object {
        const val FROM_ID_VERIFY_ARGUMENT = "fromIdVerify"
        fun newInstance(fromIdVerify: Boolean?): StorageAccessConfirmDialogFragment {
            return StorageAccessConfirmDialogFragment().also {
                it.arguments = Bundle().also { args ->
                    if (fromIdVerify == true) {
                        args.putBoolean(FROM_ID_VERIFY_ARGUMENT, fromIdVerify)
                    }
                }
            }
        }
    }

    private var fromIdVerify: Boolean? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogStorageAccessConfirmBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_storage_access_confirm,
            null,
            false
        )
        binding.lifecycleOwner = activity
        binding.handlers = this

        arguments?.let { args ->
            fromIdVerify = args.getBoolean(FROM_ID_VERIFY_ARGUMENT)

        }
        if (fromIdVerify == true) {
            val textView: TextView = binding.root.findViewById(R.id.caption_for_accept)
            textView.setText("身分証確認のため、写真へのアクセスを許可してください。")
        }
        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)
        return builder.create()
    }

    override fun onClickOpenSettings(view: View) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:jp.co.recruit.erikura"))
        startActivity(intent)
    }

}

interface StorageAccessConfirmEventHandlers {
    fun onClickOpenSettings(view: View)
}