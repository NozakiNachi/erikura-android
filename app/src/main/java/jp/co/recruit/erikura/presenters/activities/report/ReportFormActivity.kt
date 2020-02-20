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
        // FIXME: 箇所が選択されたときの処理
    }

    override fun onStatusSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        // FIXME: 作業後の状態が選択されたときの処理
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
    val comment: MutableLiveData<String> = MutableLiveData()

    val summarySelectedItem: String? = null
    val statusSelectedItem: String? = null

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(summaryEditVisibility) {result.value = isValid()}
        result.addSource(summary) { result.value = isValid() }
        result.addSource(comment) { result.value = isValid()  }
    }

    private fun isValid(): Boolean {
        var valid = true
        valid = isValidSummary() && valid
        valid = isValidComment() && valid
        return valid
    }

    private fun isValidSummary(): Boolean {
        return true
    }

    private fun isValidComment(): Boolean {
        return true
    }

    private fun isValidSelect(): Boolean {
        return true
    }
}

interface ReportFormEventHandlers {
    fun onClickNext(view: View)
    fun onSummarySelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
    fun onStatusSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
    fun onClickManual(view: View)
}