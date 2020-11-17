package jp.co.recruit.erikura.presenters.activities.job

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.DialogApplyBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
import jp.co.recruit.erikura.presenters.activities.errors.LoginRequiredActivity

class ApplyDialogFragment: DialogFragment(), ApplyDialogFragmentEventHandlers {
    companion object {
        const val JOB_ARGUMENT = "job"

        fun newInstance(job: Job?): ApplyDialogFragment {
            return ApplyDialogFragment().also {
                it.arguments = Bundle().also { args ->
                    args.putParcelable(JOB_ARGUMENT, job)
                }
            }
        }
    }

    private var job: Job? = null
    private val viewModel: ApplyDialogFragmentViewModel by lazy {
        ViewModelProvider(this).get(ApplyDialogFragmentViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            job = args.getParcelable(JOB_ARGUMENT)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogApplyBinding>(
            LayoutInflater.from(activity),
            R.layout.dialog_apply,
            null,
            false
        )
        binding.lifecycleOwner = activity
        viewModel.setup(job, this)
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

        dialog?.findViewById<TextView>(R.id.apply_manualLink)?.movementMethod = LinkMovementMethod.getInstance()
        dialog?.findViewById<TextView>(R.id.apply_cautionsLink)?.movementMethod = LinkMovementMethod.getInstance()
        dialog?.findViewById<TextView>(R.id.apply_agreementLink)?.movementMethod = LinkMovementMethod.getInstance()
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
        if (Api.isLogin) {
            job?.let { job ->
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
                    dismiss()
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
        }
        else {
            val intent= Intent(activity, LoginRequiredActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onClickManual(view: View) {
        job?.let { job ->
            job.manualUrl?.let { manualUrl ->
                activity?.let { activity ->
                    JobUtil.openManual(activity, job)
                }
            }
        }
    }

    override fun onClickCautions(view: View) {
        // ページ参照のトラッキングの送出
        job?.let { job ->
            Tracking.logEvent(event= "view_cautions", params= bundleOf())
            Tracking.viewCautions(name= "/places/cautions", title= "物件注意事項画面表示", jobId= job.id, placeId=job.placeId)

            val intent = Intent(activity, jp.co.recruit.erikura.presenters.activities.job.PropertyNotesActivity::class.java)
            intent.putExtra("place_id", job.placeId)
            intent.putExtra("job_kind_id", job.jobKind?.id)
            startActivity(intent)
        }
    }
}

class ApplyDialogFragmentViewModel: ViewModel() {
    var handler: ApplyDialogFragmentEventHandlers? = null
    val job: MutableLiveData<Job> = MutableLiveData()

    val entryQuestion: MutableLiveData<String> = MutableLiveData()
    val entryQuestionVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val entryQuestionAnswer: MutableLiveData<String> = MutableLiveData()
    val checkManual = MutableLiveData<Boolean>()
    val checkCautions = MutableLiveData<Boolean>()
    val checkSummaryTitles = MutableLiveData<Boolean>()
    val checkManualVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val checkCautionsVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val checkSummaryTitlesVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    val isEntryButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(entryQuestionVisibility) { result.value = isValid() }
        result.addSource(entryQuestionAnswer) { result.value = isValid() }
        result.addSource(checkManualVisibility) { result.value = isValid() }
        result.addSource(checkManual) { result.value = isValid() }
        result.addSource(checkCautionsVisibility) { result.value = isValid() }
        result.addSource(checkCautions) { result.value = isValid() }
        result.addSource(checkSummaryTitlesVisibility) { result.value = isValid() }
        result.addSource(checkSummaryTitles) { result.value = isValid() }
    }

    val checkManualLabel = MediatorLiveData<SpannableStringBuilder>().also { result ->
        result.addSource(job) {
            result.value = SpannableStringBuilder().also { str ->
                JobUtil.appendLinkSpan(str, "マニュアル", R.style.linkText_w2) { view ->
                    handler?.onClickManual(view)
                }
                str.append("を確認した")
            }
        }
    }
    val checkCautionsLabel = MediatorLiveData<SpannableStringBuilder>().also { result ->
        result.addSource(job) {
            result.value = SpannableStringBuilder()?.also { str ->
                JobUtil.appendLinkSpan(str, "注意事項(${job.value?.cautionsCount ?: 0}件)", R.style.linkText_w2) { view ->
                    handler?.onClickCautions(view)
                }
                str.append("を確認した")
            }
        }
    }
    val summaryTitlesLabel = MediatorLiveData<String>().also { result ->
        result.addSource(job) {
            result.value = (job.value?.summaryTitles ?: listOf()).mapIndexed { i, title ->
                "(${i + 1}) ${title}"
            }.joinToString(" / ")
        }
    }

    val agreementText = MutableLiveData<SpannableStringBuilder>(
        SpannableStringBuilder().also { str ->
            JobUtil.appendLinkSpan(str, ErikuraApplication.instance.getString(R.string.registerEmail_terms_of_service), R.style.linkText) {
                handler?.onClickTermsOfService(it)
            }
            str.append(ErikuraApplication.instance.getString(R.string.registerEmail_comma))
            JobUtil.appendLinkSpan(str, ErikuraApplication.instance.getString(R.string.registerEmail_privacy_policy), R.style.linkText) {
                handler?.onClickPrivacyPolicy(it)
            }
            str.append(ErikuraApplication.instance.getString(R.string.registerEmail_agree))
        }
    )

    fun setup(job: Job?, handler: ApplyDialogFragmentEventHandlers?) {
        this.handler = handler
        this.job.value = job

        job?.let { job ->
            entryQuestion.value = job.entryQuestion
            if (job.entryQuestion.isNullOrBlank()) {
                entryQuestionVisibility.value = View.GONE
            }
            else {
                entryQuestionVisibility.value = View.VISIBLE
            }

            checkManualVisibility.value = View.VISIBLE
            checkCautionsVisibility.value = if ((job.cautionsCount ?: 0) > 0) { View.VISIBLE } else { View.GONE }
            checkSummaryTitlesVisibility.value = if ((job.summaryTitles ?: listOf()).isEmpty()) { View.GONE } else { View.VISIBLE }
        }
    }

    private fun isValid(): Boolean {
        var valid = true

        if (checkManualVisibility.value == View.VISIBLE) {
            valid = (checkManual.value ?: false) && valid
        }
        if (checkCautionsVisibility.value == View.VISIBLE) {
            valid = (checkCautions.value ?: false) && valid
        }
        if (checkSummaryTitlesVisibility.value == View.VISIBLE) {
            valid = (checkSummaryTitles.value ?: false) && valid
        }

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
    fun onClickManual(view: View)
    fun onClickCautions(view: View)
}