package jp.co.recruit.erikura.presenters.util

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.databinding.DialogLocationAlertBinding
import jp.co.recruit.erikura.databinding.DialogMessageAlertBinding
import jp.co.recruit.erikura.databinding.DialogWriteStorageAccessConfirmBinding

// 画面へのメッセージ表示を行うためのユーティリティ
object MessageUtils {
    fun displayAlert(context: FragmentActivity, messages: Collection<String>, caption: String? = null, onCloseListener: (() -> Unit)? = null): AlertDialog {
        val dialog = AlertDialog.Builder(context).apply {
            caption?.let { setTitle(caption) }

            val viewModel = MessageAlertViewModel()
            viewModel.messages.value = messages.joinToString("\n")

            val binding: DialogMessageAlertBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_message_alert,
                null, false
            )
            binding.viewModel = viewModel
            binding.lifecycleOwner = context

            setView(binding.root)

            setOnDismissListener {
                onCloseListener?.invoke()
            }
        }.create()

        dialog.show()

        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "error_modal", params= bundleOf())
        Tracking.track(name= "error_modal")

        return dialog
    }

    fun displayLocationAlert(context: FragmentActivity, onCloseListener: ((openSettings: Boolean) -> Unit)? = null): AlertDialog {
        var openSettings = false
        val dialog = AlertDialog.Builder(context).apply {
            val binding: DialogLocationAlertBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_location_alert,
                null, false
            )
            binding.lifecycleOwner = context

            setView(binding.root)
            setOnDismissListener { onCloseListener?.invoke(openSettings) }
        }.create()

        dialog.show()

        val button: Button = dialog.findViewById(R.id.update_button)
        button.setOnSafeClickListener {
            openSettings = true
            val uriString = "package:" + ErikuraApplication.instance.packageName
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(uriString))
            context.startActivity(intent)
        }

        return dialog
    }

    fun displayPedometerAlert(context: FragmentActivity, onCloseListener: ((openSettings: Boolean) -> Unit)? = null): AlertDialog {
        var openSettings = false
        val dialog = AlertDialog.Builder(context)
            .setView(R.layout.dialog_allow_activity_recognition)
            .create()
        dialog.show()

        val label1: TextView = dialog.findViewById(R.id.allow_activity_label1)
        val sb = SpannableStringBuilder("・設定画面から「権限」＞「身体活動」をタップし「許可」を選択してください。")
        val becomeBold: (String) -> Unit = { keyword ->
            val start = sb.toString().indexOf(keyword)
            sb.setSpan(StyleSpan(R.style.label_w6), start, start + keyword.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        becomeBold("「権限」")
        becomeBold("「身体活動」")
        becomeBold("「許可」")

        label1.text = sb

        val button: Button = dialog.findViewById(R.id.update_button)
        button.setOnSafeClickListener {
            openSettings = true
            val uriString = "package:" + ErikuraApplication.instance.packageName
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(uriString))
            context.startActivity(intent)
        }

        // ダイアログが消えた場合の対応
        dialog.setOnDismissListener {
            onCloseListener?.invoke(openSettings)
        }

        return dialog
    }

    fun displayWriteExternalStorageAlert(context: FragmentActivity, onCloseListener: ((openSettings: Boolean) -> Unit)? = null): AlertDialog {
        var openSettings = false
        val dialog = AlertDialog.Builder(context).apply {
            val binding: DialogWriteStorageAccessConfirmBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_write_storage_access_confirm,
                null, false
            )
            binding.lifecycleOwner = context

            setView(binding.root)
            setOnDismissListener { onCloseListener?.invoke(openSettings) }
        }.create()

        dialog.show()

        val button: Button = dialog.findViewById(R.id.update_button)
        button.setOnSafeClickListener {
            openSettings = true
            val uriString = "package:" + ErikuraApplication.instance.packageName
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(uriString))
            context.startActivity(intent)
        }

        return dialog
    }

    class MessageAlertViewModel : ViewModel() {
        val messages: MutableLiveData<String> = MutableLiveData()
    }
}