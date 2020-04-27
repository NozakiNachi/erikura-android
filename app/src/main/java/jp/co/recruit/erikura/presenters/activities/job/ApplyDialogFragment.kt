package jp.co.recruit.erikura.presenters.activities.job

import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.TextAppearanceSpan
import android.util.Log
import androidx.fragment.app.DialogFragment
import jp.co.recruit.erikura.R
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.DialogApplyBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
import jp.co.recruit.erikura.presenters.activities.errors.LoginRequiredActivity

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

        binding.root.setOnTouchListener { view, event ->
            if (view != null) {
                val imm: InputMethodManager = activity!!.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
            return@setOnTouchListener false
        }

        val builder = AlertDialog.Builder(activity)
        builder.setView(binding.root)
        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        var tv = dialog!!.findViewById<TextView>(R.id.apply_agreementLink)
        tv.text = makeLink()
        tv.movementMethod = LinkMovementMethod.getInstance()
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
        if (UserSession.retrieve() != null) {
            if (job != null) {
                Api(activity!!).entry(job, viewModel.entryQuestionAnswer.value?: "", onError = {
                    Log.v("DEBUG", "応募失敗")
                    // 詳細画面のリロード
                    val intent= Intent(activity, JobDetailsActivity::class.java)
                    if(it != null) {
                        val array = it.toTypedArray()
                        intent.putExtra("errorMessages", array)
                    }
                    intent.putExtra("job", job)
                    startActivity(intent)
                }) {
                    // 応募のトラッキングの送出
                    Tracking.logEvent(event= "job_entry", params= bundleOf())
                    Tracking.jobEntry(name= "job_entry", title= "", job= job)
                    Tracking.logEventFB(event= "EntryJob")

                    val intent= Intent(activity, ApplyCompletedActivity::class.java)
                    intent.putExtra("job", job)
                    startActivity(intent)
                }
            }
        }else {
            val intent= Intent(activity, LoginRequiredActivity::class.java)
            startActivity(intent)
        }
    }

    private fun makeLink(): SpannableStringBuilder {
        var str = SpannableStringBuilder()

        var start = 0
        str.append(ErikuraApplication.instance.getString(R.string.registerEmail_terms_of_service))
        var end = str.length
        val linkTextAppearanceSpan = TextAppearanceSpan(ErikuraApplication.instance.applicationContext, R.style.linkText)
        str.setSpan(linkTextAppearanceSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        str.setSpan(object : ClickableSpan() {
            override fun onClick(view: View) {
                onClickTermsOfService(view)
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        str.append(ErikuraApplication.instance.getString(R.string.registerEmail_comma))
        start = str.length
        str.append(ErikuraApplication.instance.getString(R.string.registerEmail_privacy_policy))
        end = str.length
        val linkTextAppearanceSpan2 = TextAppearanceSpan(ErikuraApplication.instance.applicationContext, R.style.linkText)
        str.setSpan(linkTextAppearanceSpan2, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        str.setSpan(object : ClickableSpan() {
            override fun onClick(view: View) {
                onClickPrivacyPolicy(view)
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        str.append(ErikuraApplication.instance.getString(R.string.registerEmail_agree))
        return str
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
        result.addSource(entryQuestionVisibility) { result.value = isValid() }
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

    private fun isValid(): Boolean {
        var valid = true

        // 応募時の質問がある場合のみバリデーションを行います
        if (entryQuestionVisibility.value == View.VISIBLE) {
            // 必須チェック
            if (valid && entryQuestionAnswer.value.isNullOrBlank()) {
                valid = false
            }
            // 長さチェック
            if (valid && (entryQuestionAnswer.value?.length ?: 0) > 2000) {
                valid = false
            }
        }

        return valid
    }
}

interface ApplyDialogFragmentEventHandlers {
    fun onClickTermsOfService(view: View)
    fun onClickPrivacyPolicy(view: View)
    fun onClickEntryButton(view: View)
}