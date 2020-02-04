package jp.co.recruit.erikura.presenters.util

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.DialogMessageAlertBinding

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

        return dialog
    }

    class MessageAlertViewModel : ViewModel() {
        val messages: MutableLiveData<String> = MutableLiveData()
    }
}