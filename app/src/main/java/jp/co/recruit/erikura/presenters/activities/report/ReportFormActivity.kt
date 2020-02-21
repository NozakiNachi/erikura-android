package jp.co.recruit.erikura.presenters.activities.report

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
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
    var pictureIndex = 0
    var outputSummaryList: MutableList<OutputSummary> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_form)

        job = intent.getParcelableExtra<Job>("job")
        pictureIndex = intent.getIntExtra("pictureIndex", 0)
        outputSummaryList = job.report?.outputSummaries?.toMutableList()?: mutableListOf()

        val binding: ActivityReportFormBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_form)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        setup()
    }

    override fun onClickNext(view: View) {
        job.report?.let {
            val summaries = it.outputSummaries
            val summary = summaries[pictureIndex]
            if (viewModel.summarySelectedItem == ErikuraApplication.instance.getString(R.string.other_hint)) {
                summary.place = viewModel.summary.value
            }else {
                summary.place = viewModel.summarySelectedItem
            }
            summary.evaluation = viewModel.evaluationSelectedItem
            summary.comment = viewModel.comment.value

            var nextIndex = pictureIndex + 1
            if (nextIndex < summaries.size) {
                val intent= Intent(this, ReportFormActivity::class.java)
                intent.putExtra("job", job)
                intent.putExtra("pictureIndex", nextIndex)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }else {
                val intent= Intent(this, ReportWorkingTimeActivity::class.java)
                intent.putExtra("job", job)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
        }
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

    private fun setup() {
        val max = (job.report?.outputSummaries?.lastIndex?: 0) + 1
        viewModel.title.value = ErikuraApplication.instance.getString(R.string.report_form_caption, pictureIndex+1, max)
        createSummaryItems()
        createImage()
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

    private fun createImage() {
        val imageView: ImageView = findViewById(R.id.report_form_image)
        job.report?.let {
            val item = it.outputSummaries[pictureIndex].photoAsset
            item?.let {
                item.loadImage(this, imageView)
            }
        }
    }
}

class ReportFormViewModel: ViewModel() {
    val title: MutableLiveData<String> = MutableLiveData()
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
        }else if (valid && comment.value?.length?: 0 > 5000) {
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