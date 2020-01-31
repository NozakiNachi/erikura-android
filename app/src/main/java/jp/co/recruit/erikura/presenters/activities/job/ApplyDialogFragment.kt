package jp.co.recruit.erikura.presenters.activities.job

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import jp.co.recruit.erikura.R
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.DialogApplyBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity

class ApplyDialogFragment(private val job: Job?): DialogFragment(), ApplyDialogFragmentEventHandlers {
    private val viewModel: ApplyDialogFragmentViewModel by lazy {
        ViewModelProvider(this).get(ApplyDialogFragmentViewModel::class.java)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogApplyBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_apply,
            null,
            false
        )
        binding.lifecycleOwner = activity
        viewModel.setup(job)
        binding.viewModel = viewModel
        binding.handlers = this

        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)
        return builder.create()
    }

    override fun onClickTermsOfService(view: View) {
        val termsOfServiceURLString = BuildConfig.SERVER_BASE_URL + "/pdf/terms_of_service.pdf"
        val intent = Intent(activity, WebViewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(termsOfServiceURLString)
        }
        startActivity(intent)
    }

    override fun onClickPrivacyPolicy(view: View) {
        val privacyPolicyURLString = BuildConfig.SERVER_BASE_URL + "/pdf/privacy_policy.pdf"
        val intent = Intent(activity, WebViewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(privacyPolicyURLString)
        }
        startActivity(intent)
    }

    override fun onClickEntryButton(view: View) {
        // FIXME: 応募処理の実装
    }
}

class ApplyDialogFragmentViewModel: ViewModel() {
    val entryQuestion: MutableLiveData<String> = MutableLiveData()
    val entryQuestionVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val entryQuestionAnswer: MutableLiveData<String> = MutableLiveData()
    val caption: MutableLiveData<String> = MutableLiveData()
    val reportPlaces: MutableLiveData<String> = MutableLiveData()
    val reportPlacesVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    val isEntryButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(entryQuestionAnswer) { result.value = isValid() }
    }

    fun setup(job: Job?) {
        if (job != null) {
            if(!job.entryQuestion.isNullOrBlank()) {
                entryQuestion.value = job.entryQuestion
                entryQuestionVisibility.value = View.VISIBLE
            }else {
                entryQuestionVisibility.value = View.GONE
            }
            
            if(!job.summaryTitles.isNullOrEmpty()) {
                caption.value = ErikuraApplication.instance.getString(R.string.applyDialog_caption2Pattern1)
                var summaryTitleStr = ""
                job.summaryTitles.forEachIndexed { index, s ->
                    summaryTitleStr += "(${index+1}) ${s}　"
                }
                reportPlaces.value = summaryTitleStr
                reportPlacesVisibility.value = View.VISIBLE
            }else {
                caption.value = ErikuraApplication.instance.getString(R.string.applyDialog_caption2Pattern2)
                reportPlacesVisibility.value = View.GONE
            }
        }
    }

    fun isValid(): Boolean {
        if (entryQuestionVisibility.value == View.VISIBLE && entryQuestionAnswer.value.isNullOrBlank()) {
            return false
        }else {
            return true
        }
    }
}

interface ApplyDialogFragmentEventHandlers {
    fun onClickTermsOfService(view: View)
    fun onClickPrivacyPolicy(view: View)
    fun onClickEntryButton(view: View)
}