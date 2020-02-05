package jp.co.recruit.erikura.presenters.activities.job

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import androidx.fragment.app.DialogFragment
import jp.co.recruit.erikura.R
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.DialogApplyBinding
import jp.co.recruit.erikura.databinding.DialogCancelBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity

class CancelDialogFragment(private val job: Job?): DialogFragment(), CancelDialogFragmentEventHandlers {
    private val viewModel: CancelDialogFragmentViewModel by lazy {
        ViewModelProvider(this).get(CancelDialogFragmentViewModel::class.java)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogCancelBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_cancel,
            null,
            false
        )
        binding.lifecycleOwner = activity
        binding.viewModel = viewModel
        binding.handlers = this


        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)
        return builder.create()
    }


    override fun onClickCancel(view: View) {

    }
}

class CancelDialogFragmentViewModel: ViewModel() {
    val reason: MutableLiveData<String> = MutableLiveData()
    val reasonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
}

interface CancelDialogFragmentEventHandlers {
    fun onClickCancel(view: View)
}