package jp.co.recruit.erikura.presenters.activities.report

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.OutputSummary
import jp.co.recruit.erikura.databinding.ActivityReportFormBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity

class ReportFormActivity : AppCompatActivity(), ReportFormEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportFormViewModel::class.java)
    }

    var job = Job()
    var outputSummaryList: MutableList<OutputSummary> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_form)

        job = intent.getParcelableExtra<Job>("job")
        outputSummaryList = intent.getParcelableArrayListExtra("outputSummaryList")

        val binding: ActivityReportFormBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_form)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        createSummaryItems()
    }

    override fun onClickNext(view: View) {
        // FIXME: 報告箇所画面 or マニュアル外報告画面
    }

    override fun onSummarySelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        viewModel.summaryItems.value?.let {
            if (position == 0) {
                viewModel.summarySelectedItem = null
                viewModel.summaryEditVisibility.value = View.GONE
            } else if (position == viewModel.summaryItems.value?.lastIndex) {
                viewModel.summarySelectedItem = parent.getItemAtPosition(position).toString()
                viewModel.summaryEditVisibility.value = View.VISIBLE
            }else {
                viewModel.summarySelectedItem = job.summaryTitles[position-1]
                viewModel.summaryEditVisibility.value = View.GONE
            }
        }
    }

    override fun onEvaluationSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        viewModel.evaluationSelectedItem = if ( position == 0 ) {null} else {parent.getItemAtPosition(position).toString()}
    }

    override fun onClickManual(view: View) {
        if(job?.manualUrl != null){
            val termsOfServiceURLString = job.manualUrl
            val intent = Intent(this, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(termsOfServiceURLString)
            }
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    private fun createSummaryItems() {
        val summaryTitles: MutableList<String> = mutableListOf()
        summaryTitles.add(ErikuraApplication.instance.getString(R.string.please_select))
        job.summaryTitles.forEachIndexed { index, s ->
            summaryTitles.add("(${index+1}) ${s}")
        }
        summaryTitles.add(ErikuraApplication.instance.getString(R.string.other_hint))
        viewModel.summaryItems.value = summaryTitles
    }
}

class ReportFormViewModel: ViewModel() {
    val summaryItems: MutableLiveData<List<String>> = MutableLiveData(listOf())
    val summaryEditVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val summary: MutableLiveData<String> = MutableLiveData()
    val summaryErrorMsg: MutableLiveData<String> = MutableLiveData()
    val summaryErrorVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val comment: MutableLiveData<String> = MutableLiveData()
    val commentErrorMsg: MutableLiveData<String> = MutableLiveData()
    val commentErrorVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    var summarySelectedItem: String? = null
    var evaluationSelectedItem: String? = null

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(summaryEditVisibility) {result.value = isValid()}
        result.addSource(summary) { result.value = isValid() }
        result.addSource(comment) { result.value = isValid()  }
    }

    private fun isValid(): Boolean {
        var valid = true
        if (summaryEditVisibility.value == View.VISIBLE) {
            valid = isValidSummary() && valid
        }else {
            summaryErrorMsg.value = ""
            summaryErrorVisibility.value = View.GONE
        }
        valid = isValidComment() && valid
        valid = summarySelectedItem != null && evaluationSelectedItem != null && valid
        return valid
    }

    private fun isValidSummary(): Boolean {
        var valid = true
        if (valid && summary.value.isNullOrBlank()) {
            valid = false
            summaryErrorMsg.value = ""
            summaryErrorVisibility.value = View.GONE
        }else if (valid && summary.value?.length?: 0 > 50) {
            valid = false
            summaryErrorMsg.value = ErikuraApplication.instance.getString(R.string.summary_count_error)
            summaryErrorVisibility.value = View.VISIBLE
        }else {
            valid = true
            summaryErrorMsg.value = ""
            summaryErrorVisibility.value = View.GONE
        }
        return valid
    }

    private fun isValidComment(): Boolean {
        var valid = true
        if (valid && comment.value.isNullOrBlank()) {
            valid = false
            commentErrorMsg.value = ""
            commentErrorVisibility.value = View.GONE
        }else if (valid && summary.value?.length?: 0 > 5000) {
            valid = false
            commentErrorMsg.value = ErikuraApplication.instance.getString(R.string.comment_count_error)
            commentErrorVisibility.value = View.VISIBLE
        }else {
            valid = true
            commentErrorMsg.value = ""
            commentErrorVisibility.value = View.GONE
        }
        return valid
    }
}

interface ReportFormEventHandlers {
    fun onClickNext(view: View)
    fun onSummarySelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
    fun onEvaluationSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
    fun onClickManual(view: View)
}