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


class StorageAccessConfirmDialogFragment : DialogFragment(), StorageAccessConfirmEventHandlers {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogStorageAccessConfirmBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_storage_access_confirm,
            null,
            false
        )
        binding.lifecycleOwner = activity
        binding.handlers = this

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