package jp.co.recruit.erikura.presenters.activities.report

import JobUtil
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.FrameLayout
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
import jp.co.recruit.erikura.business.models.*
import jp.co.recruit.erikura.databinding.ActivityReportFormBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel

class ReportFormActivity : BaseActivity(), ReportFormEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportFormViewModel::class.java)
    }

    var job = Job()
    var fromConfirm = false
    var pictureIndex = 0
    var outputSummaryList: MutableList<OutputSummary> = mutableListOf()
    var editCompleted: Boolean = true

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
        if (editCompleted) {
            setup()
            editCompleted = false
        }

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

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val view = this.currentFocus
        if (view != null) {
            val layout = findViewById<FrameLayout>(R.id.report_form_layout)
            layout.requestFocus()

            val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(layout.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
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

            if (viewModel.commentSelectedItem == ErikuraApplication.instance.getString(R.string.other_hint)) {
                summary.comment = viewModel.comment.value
            }
            else {
                summary.comment = viewModel.commentSelectedItem
            }
            editCompleted = true

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
                startActivity(intent)
            } else {
                // 写真が残っていない場合
                val intent= Intent(this, ReportWorkingTimeActivity::class.java)
                intent.putExtra("job", job)
                startActivity(intent)
            }
        }
    }

    override fun onSummarySelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.summaryItems.value?.let {
            if (position == 0) {
                viewModel.summarySelectedItem = null
                viewModel.summaryEditVisibility.value = View.GONE
            } else if (position == viewModel.summaryItems.value?.lastIndex) {
                viewModel.summarySelectedItem = parent?.getItemAtPosition(position).toString()
                viewModel.summaryEditVisibility.value = View.VISIBLE
            }else {
                viewModel.summarySelectedItem = job.summaryTitles[position-1]
                viewModel.summaryEditVisibility.value = View.GONE
            }
        }
    }

    override fun onEvaluationSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val evaluateType = viewModel.evaluateTypes[position]
        viewModel.evaluationSelectedItem = evaluateType
    }

    override fun onCommentSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.commentItems.value?.let {
            if (position == 0) {
                viewModel.commentSelectedItem = null
                viewModel.commentEditVisibility.value = View.GONE
            }
            else if (position == viewModel.commentItems.value?.lastIndex) {
                viewModel.commentSelectedItem = parent?.getItemAtPosition(position).toString()
                viewModel.commentEditVisibility.value = View.VISIBLE
            }
            else {
                viewModel.commentSelectedItem = (ErikuraConfig.jobReportFixedPhraseA + ErikuraConfig.jobReportFixedPhraseB)[position - 1]
                viewModel.commentEditVisibility.value = View.GONE
            }
        }
    }

    override fun onClickManual(view: View) {
        if(job?.manualUrl != null){
            JobUtil.openManual(this, job!!)
        }
    }

    override fun onClickReportExamples(view: View) {
        job?.let { job ->
            JobUtil.openReportExample(this, job)
        }
    }

    private fun setup() {
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
        createCommentItems()
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

    private fun createCommentItems() {
        val fixedPhrases : MutableList<String> = mutableListOf()
        fixedPhrases.add(ErikuraApplication.instance.getString(R.string.please_select))
        (ErikuraConfig.jobReportFixedPhraseA + ErikuraConfig.jobReportFixedPhraseB).forEachIndexed { index, s ->
            fixedPhrases.add(s)
        }
        fixedPhrases.add(ErikuraApplication.instance.getString(R.string.other_hint))
        viewModel.commentItems.value = fixedPhrases
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
        var commentIndex = (ErikuraConfig.jobReportFixedPhraseA + ErikuraConfig.jobReportFixedPhraseB).count() + 1
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

            viewModel.commentSelectedItem = null
            (ErikuraConfig.jobReportFixedPhraseA + ErikuraConfig.jobReportFixedPhraseB).forEachIndexed { index, s ->
                if (s == summary.comment) {
                    commentIndex = index + 1
                    viewModel.commentSelectedItem = s
                }
            }
            viewModel.commentId.value = if (summary.comment.isNullOrEmpty()) { 0 } else { commentIndex }
            if (commentIndex ==  (ErikuraConfig.jobReportFixedPhraseA + ErikuraConfig.jobReportFixedPhraseB).count() + 1) {
                viewModel.comment.value = summary.comment
                viewModel.commentSelectedItem = ErikuraApplication.instance.getString(R.string.other_hint)
                viewModel.commentEditVisibility.value = View.VISIBLE
            }

            viewModel.comment.value = summary.comment
        }
        //お手本報告件数が0件の場合非表示
        job.goodExamplesCount?.let { reportExampleCount ->
            if (reportExampleCount == 0) {
                viewModel.reportExamplesButtonVisibility.value = View.GONE
            }
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
    val commentItems: MutableLiveData<List<String>> = MutableLiveData(listOf())
    val commentId: MutableLiveData<Int> = MutableLiveData()
    val comment: MutableLiveData<String> = MutableLiveData()
    val commentError: ErrorMessageViewModel = ErrorMessageViewModel()
    val commentEditVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val evaluateTypes = EvaluateType.values()
    val evaluateLabels: List<String> = evaluateTypes.map { ErikuraApplication.applicationContext.getString(it.resourceId) }

    var summarySelectedItem: String? = null
    var evaluationSelectedItem: EvaluateType = EvaluateType.UNSELECTED
    var commentSelectedItem: String? = null

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(summaryEditVisibility) {result.value = isValid() }
        result.addSource(summary) { result.value = isValid() }
        result.addSource(statusId) { result.value = isValid() }
        result.addSource(commentEditVisibility) { result.value = isValid() }
        result.addSource(comment) { result.value = isValid() }
    }
    val reportExamplesButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

    private fun isValid(): Boolean {
        var valid = true
        if (summaryEditVisibility.value == View.VISIBLE) {
            valid = isValidSummary() && valid
        }else {
            summaryError.message.value = null
        }
        valid = isValidStatusId() && valid
        if (commentEditVisibility.value == View.VISIBLE) {
            valid = isValidComment() && valid
        }
        else {
            commentError.message.value = null
        }
        valid = summarySelectedItem != null && valid
        valid = evaluationSelectedItem != null && valid
        valid = commentSelectedItem != null && valid
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

    private fun isValidStatusId(): Boolean {
        return !(statusId.value == 0 || statusId.value == null)
    }

    private fun isValidComment(): Boolean {
        var valid = true
        if (valid && comment.value.isNullOrBlank()) {
            valid = false
            commentError.message.value = null
        }else if (valid && comment.value?.length?: 0 > ErikuraConst.maxOutputSummaryCommentLength) {
            valid = false
            commentError.message.value = ErikuraApplication.instance.getString(R.string.comment_count_error, ErikuraConst.maxOutputSummaryCommentLength)
        }else {
            valid = true
            commentError.message.value = null
        }
        return valid
    }
}

interface ReportFormEventHandlers {
    fun onClickNext(view: View)
    fun onSummarySelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    fun onEvaluationSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    fun onCommentSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    fun onClickManual(view: View)
    fun onClickReportExamples(view: View)
}