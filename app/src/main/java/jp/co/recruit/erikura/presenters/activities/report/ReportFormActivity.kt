package jp.co.recruit.erikura.presenters.activities.report

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.EvaluateType
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.OutputSummary
import jp.co.recruit.erikura.databinding.ActivityReportFormBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel


class ReportFormActivity : BaseActivity(), ReportFormEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportFormViewModel::class.java)
    }

    var job = Job()
    var fromConfirm = false
    var pictureIndex = 0
    var outputSummaryList: MutableList<OutputSummary> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_form)

        val binding: ActivityReportFormBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_form)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        job = intent.getParcelableExtra<Job>("job")
        ErikuraApplication.instance.reportingJob = job
        pictureIndex = intent.getIntExtra("pictureIndex", 0)
        fromConfirm = intent.getBooleanExtra("fromConfirm", false)
    }

    override fun onStart() {
        super.onStart()
        ErikuraApplication.instance.reportingJob?.let {
            job = it
        }
        outputSummaryList = job.report?.outputSummaries?.toMutableList()?: mutableListOf()

        // 戻ってきた場合に、該当の場所詳細が削除されていれば処理をスキップします
        if (outputSummaryList[pictureIndex].willDelete) {
            finish()
            return
        }

        // 戻るボタンの対策として、この時点でロードされている
        setup()

        if (job.reportId != null) {
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_job_report_point", params= bundleOf())
            Tracking.viewJobDetails(name= "/reports/register/detail/${job.id}", title= "作業報告画面（箇所）", jobId= job.id)
        }else {
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_edit_job_report_point", params= bundleOf())
            Tracking.viewJobDetails(name= "/reports/edit/detail/${job.id}", title= "作業報告編集画面（箇所）", jobId= job.id)
        }
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
            summary.evaluation = viewModel.evaluationSelectedItem.toString().toLowerCase()
            summary.comment = viewModel.comment.value

            var nextIndex = pictureIndex + 1
            while(nextIndex < summaries.size && summaries[nextIndex].willDelete)
                nextIndex++

            if (fromConfirm) {
                // 確認画面から来た場合は、確認画面に戻ります
                val intent= Intent()
                intent.putExtra("job", job)
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else if (nextIndex < summaries.size) {
                // 写真が残っている場合
                val intent= Intent(this, ReportFormActivity::class.java)
                intent.putExtra("job", job)
                intent.putExtra("pictureIndex", nextIndex)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            } else {
                // 写真が残っていない場合
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
        val evaluateType = viewModel.evaluateTypes[position]
        viewModel.evaluationSelectedItem = evaluateType
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
//        val max = (job.report?.outputSummaries?.lastIndex?: 0) + 1
        var max = 0
        var pictureIndexNotDeleted = pictureIndex
        job.report?.outputSummaries?.forEachIndexed { index, summary ->
            if (summary.willDelete) {
                if (index < pictureIndex) {
                    pictureIndexNotDeleted--
                }
            }else {
                max++
            }
        }
        viewModel.title.value = ErikuraApplication.instance.getString(R.string.report_form_caption, pictureIndexNotDeleted+1, max)
        createSummaryItems()
        createImage()
        loadData()
        viewModel.summaryError.message.value = null
        viewModel.commentError.message.value = null
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
            val summary = it.outputSummaries[pictureIndex]
            val item = summary.photoAsset
            item?.let {
                if (summary.beforeCleaningPhotoUrl != null ) {
                    item.loadImageFromString(this, imageView)
                }else {
                    item.loadImage(this, imageView)
                }
            }
        }
    }

    private fun loadData() {
        var summaryIndex = job.summaryTitles.count() + 1
        job.report?.let {
            val summary = it.outputSummaries[pictureIndex]
            viewModel.summarySelectedItem = null
            job.summaryTitles.forEachIndexed { index, s ->
                if (s == summary.place) {
                    summaryIndex = index + 1
                    viewModel.summarySelectedItem = s
                }
            }
            viewModel.summaryId.value = if (summary.place.isNullOrEmpty()){0} else {summaryIndex}
            if (summaryIndex == job.summaryTitles.count() + 1) {
                viewModel.summary.value = summary.place
                viewModel.summarySelectedItem = ErikuraApplication.instance.getString(R.string.other_hint)
                viewModel.summaryEditVisibility.value = View.VISIBLE
            }
            val evaluate = EvaluateType.valueOf(summary.evaluation?.toUpperCase()?: "UNSELECTED")
            when(evaluate) {
                EvaluateType.BAD -> {
                    viewModel.statusId.value = viewModel.evaluateTypes.indexOf(EvaluateType.BAD)
                }
                EvaluateType.ORDINARY -> {
                    viewModel.statusId.value = viewModel.evaluateTypes.indexOf(EvaluateType.ORDINARY)
                }
                EvaluateType.GOOD -> {
                    viewModel.statusId.value = viewModel.evaluateTypes.indexOf(EvaluateType.GOOD)
                }
                else -> {
                    viewModel.statusId.value = viewModel.evaluateTypes.indexOf(EvaluateType.UNSELECTED)
                }
            }
            viewModel.comment.value = summary.comment
        }
    }
}

class ReportFormViewModel: ViewModel() {
    val title: MutableLiveData<String> = MutableLiveData()
    val summaryItems: MutableLiveData<List<String>> = MutableLiveData(listOf())
    val summaryId: MutableLiveData<Int> = MutableLiveData()
    val summaryEditVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val summary: MutableLiveData<String> = MutableLiveData()
    val summaryError: ErrorMessageViewModel = ErrorMessageViewModel()
    val statusId: MutableLiveData<Int> = MutableLiveData()
    val comment: MutableLiveData<String> = MutableLiveData()
    val commentError: ErrorMessageViewModel = ErrorMessageViewModel()
    val evaluateTypes = EvaluateType.values()
    val evaluateLabels: List<String> = evaluateTypes.map { ErikuraApplication.applicationContext.getString(it.resourceId) }

    var summarySelectedItem: String? = null
    var evaluationSelectedItem: EvaluateType = EvaluateType.UNSELECTED

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
            summaryError.message.value = null
        }
        valid = isValidComment() && valid
        valid = summarySelectedItem != null && evaluationSelectedItem != null && valid
        return valid
    }

    private fun isValidSummary(): Boolean {
        var valid = true
        if (valid && summary.value.isNullOrBlank()) {
            valid = false
            summaryError.message.value = null
        }else if (valid && summary.value?.length?: 0 > 50) {
            valid = false
            summaryError.message.value = ErikuraApplication.instance.getString(R.string.summary_count_error)
        }else {
            valid = true
            summaryError.message.value = null
        }
        return valid
    }

    private fun isValidComment(): Boolean {
        var valid = true
        if (valid && comment.value.isNullOrBlank()) {
            valid = false
            commentError.message.value = null
        }else if (valid && comment.value?.length?: 0 > 5000) {
            valid = false
            commentError.message.value = ErikuraApplication.instance.getString(R.string.comment_count_error)
        }else {
            valid = true
            commentError.message.value = null
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