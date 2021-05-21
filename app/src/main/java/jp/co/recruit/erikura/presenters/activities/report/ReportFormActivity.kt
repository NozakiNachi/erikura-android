package jp.co.recruit.erikura.presenters.activities.report

import JobUtil
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import com.bumptech.glide.Glide
import com.google.firebase.crashlytics.FirebaseCrashlytics
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.ErikuraConst
import jp.co.recruit.erikura.business.models.EvaluateType
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.OutputSummary
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.data.storage.ReportDraft
import jp.co.recruit.erikura.databinding.ActivityReportFormBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel
import kotlinx.android.synthetic.main.activity_report_form.*
import org.apache.commons.lang.builder.ToStringBuilder

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

        viewModel.fixedPhraseItems.observe(this, Observer{ items ->
            val currentPhrase = viewModel.fixedPhraseSelectedItem.value

            val adapter = ArrayAdapter<String>(this@ReportFormActivity, R.layout.custom_dropdown_item, items.toTypedArray())
            adapter.setDropDownViewResource(R.layout.custom_dropdown_item)
            report_form_fixed_phrases.adapter = adapter

            if (items.contains(currentPhrase)) {
                viewModel.fixedPhraseId.value = items.indexOf(currentPhrase)
            }
        })

//        job = intent.getParcelableExtra<Job>("job")
//        ErikuraApplication.instance.reportingJob = job
        // ReportingJob は常に存在しているはずなので、!! を用いる
        if (ErikuraApplication.instance.currentJob != null) {
            job = ErikuraApplication.instance.currentJob!!
        }
        else {
            // 案件情報が取れない場合
            Intent(this, MapViewActivity::class.java).let {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                it.putStringArrayListExtra(
                    ErikuraApplication.ERROR_MESSAGE_KEY,
                    arrayListOf("お仕事の情報を取得できませんでした。予期せぬエラーによりアプリが終了した可能性ございます。お手数ですがもう一度はじめから操作してください。")
                )
                this.startActivity(it)
            }
            Log.v(ErikuraApplication.LOG_TAG,  "Cannot retrieve job")
            // FirebaseCrashlytics に案件がnull出会ったことを記録します
            val e = Throwable("ErikuraApplication.currentJob is null")
            FirebaseCrashlytics.getInstance().recordException(e)
            return
        }
        pictureIndex = intent.getIntExtra("pictureIndex", 0)
        fromConfirm = intent.getBooleanExtra("fromConfirm", false)
    }

    override fun onStart() {
        super.onStart()
        ErikuraApplication.instance.currentJob?.let {
            job = it
        }
        outputSummaryList = job.report?.outputSummaries?.toMutableList() ?: mutableListOf()

        if (outputSummaryList.isEmpty()) {
            // 実施箇所の選択ができていない状態
            FirebaseCrashlytics.getInstance().setCustomKey("job", ToStringBuilder.reflectionToString(job))
            FirebaseCrashlytics.getInstance().setCustomKey("report", ToStringBuilder.reflectionToString(job?.report))
            FirebaseCrashlytics.getInstance().recordException(RuntimeException("summaryList is blank: job=${ToStringBuilder.reflectionToString(job)}, report=${ToStringBuilder.reflectionToString(job?.report)}"))
            finish()
            return
        }
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
        else {
            createImage()
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

//        FirebaseCrashlytics.getInstance().recordException(MemoryTraceException(this.javaClass.name, getAvailableMemory()))
    }

    override fun onStop() {
        super.onStop()
        val imageView: ImageView = findViewById(R.id.report_form_image)
        Glide.with(this).clear(imageView)

        // GCをかけておきます
        System.gc()
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

    override fun onBackPressed() {
        if (!fromConfirm) {
            // 編集中の内容を保存します
            fillSummary()

            val summaries = job.report?.outputSummaries ?: listOf()
            var prevIndex = pictureIndex - 1
            while(prevIndex >= 0 && summaries[prevIndex].willDelete)
                prevIndex--
            if (prevIndex >= 0) {
                JobUtils.saveReportDraft(job, step = ReportDraft.ReportStep.SummaryForm, summaryIndex = prevIndex)
                // 写真が残っている場合
                val intent= Intent(this, ReportFormActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("job", job)
                intent.putExtra("pictureIndex", prevIndex)
                startActivity(intent)
            } else {
                JobUtil.displaySuspendReportConfirmation(this, ReportDraft.ReportStep.SummaryForm, job, 0)
            }
        }
        else {
            // 確認画面から遷移した場合は onBackPress のデフォルト動作を行います
            super.onBackPressed()
        }
    }

    override fun onClickNext(view: View) {
        job.report?.let {
            val summaries = it.outputSummaries

            fillSummary()
            editCompleted = true

            var nextIndex = pictureIndex + 1
            while(nextIndex < summaries.size && summaries[nextIndex].willDelete)
                nextIndex++


            if (fromConfirm) {
                JobUtils.saveReportDraft(job, step = ReportDraft.ReportStep.Confirm)
                // 確認画面から来た場合は、確認画面に戻ります
                val intent= Intent()
                intent.putExtra("job", job)
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else if (nextIndex < summaries.size) {
                JobUtils.saveReportDraft(job, step = ReportDraft.ReportStep.SummaryForm, summaryIndex = nextIndex)
                // 写真が残っている場合
                val intent= Intent(this, ReportFormActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("job", job)
                intent.putExtra("pictureIndex", nextIndex)
                startActivity(intent)
            } else {
                JobUtils.saveReportDraft(job, step = ReportDraft.ReportStep.WorkingTimeForm)
                // 写真が残っていない場合
                val intent= Intent(this, ReportWorkingTimeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("job", job)
                startActivity(intent)
            }
        }
    }

    private fun fillSummary() {
        job.report?.let {
            val summaries = it.outputSummaries

            val summary = summaries[pictureIndex]
            if (viewModel.summarySelectedItem.value == ErikuraApplication.instance.getString(R.string.other_hint)) {
                summary.place = viewModel.summary.value
            }else {
                summary.place = viewModel.summarySelectedItem.value
            }
            summary.evaluation = viewModel.evaluationSelectedItem.value.toString().toLowerCase()

            if (viewModel.fixedPhraseSelectedItem.value == ErikuraApplication.instance.getString(R.string.other_hint)) {
                summary.comment = viewModel.comment.value
            }
            else {
                summary.comment = viewModel.fixedPhraseSelectedItem.value
            }
        }
    }

    override fun onSummarySelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.summaryItems.value?.let {
            if (position == 0) {
                viewModel.summarySelectedItem.value = null
                viewModel.summaryEditVisibility.value = View.GONE
            } else if (position == viewModel.summaryItems.value?.lastIndex) {
                viewModel.summarySelectedItem.value = parent?.getItemAtPosition(position).toString()
                viewModel.summaryEditVisibility.value = View.VISIBLE
            }else {
                viewModel.summarySelectedItem.value = job.summaryTitles[position-1]
                viewModel.summaryEditVisibility.value = View.GONE
            }
        }
    }

    override fun onEvaluationSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val evaluateType = viewModel.evaluateTypes[position]
        if (viewModel.evaluationSelectedItem.value != evaluateType) {
            viewModel.fixedPhraseId.value = 0
            viewModel.fixedPhraseSelectedItem.value = null
            viewModel.evaluationSelectedItem.value = evaluateType
        }
    }

    override fun onFixedPhraseSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.fixedPhraseItems.value?.let { phrases ->
            if (position == 0) {
                viewModel.fixedPhraseSelectedItem.value = null
                viewModel.commentEditVisibility.value = View.GONE
            }
            else if (position == phrases.lastIndex) {
                viewModel.fixedPhraseSelectedItem.value = parent?.getItemAtPosition(position).toString()
                viewModel.commentEditVisibility.value = View.VISIBLE
            }
            else {
                viewModel.fixedPhraseSelectedItem.value = phrases[position]
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

    override fun onClickClose(view: View) {
        // 編集中の内容を保存します
        fillSummary()

        // 現在表示している実施箇所のインデックスで下書き保存します
        JobUtil.displaySuspendReportConfirmation(this, ReportDraft.ReportStep.SummaryForm, job, pictureIndex)
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
        val width = imageView.layoutParams.width / ErikuraApplication.instance.resources.displayMetrics.density
        val height = imageView.layoutParams.height / ErikuraApplication.instance.resources.displayMetrics.density
        job.report?.let {
            val summary = it.outputSummaries[pictureIndex]
            val item = summary.photoAsset
            item?.let {
                if (summary.beforeCleaningPhotoUrl != null ) {
                    item.loadImageFromString(this, imageView, width.toInt(), height.toInt())
                }else {
                    item.loadImage(this, imageView, width.toInt(), height.toInt())
                }
            }
        }
    }

    private fun loadData() {
        viewModel.job.value = job
        var summaryIndex = job.summaryTitles.count() + 1
        job.report?.let {
            val summary = it.outputSummaries[pictureIndex]
            viewModel.summarySelectedItem.value = null
            job.summaryTitles.forEachIndexed { index, s ->
                if (s == summary.place) {
                    summaryIndex = index + 1
                    viewModel.summarySelectedItem.value = s
                }
            }
            viewModel.summaryId.value = if (summary.place.isNullOrEmpty()){0} else {summaryIndex}
            if (summaryIndex == job.summaryTitles.count() + 1) {
                viewModel.summary.value = summary.place
                viewModel.summarySelectedItem.value = ErikuraApplication.instance.getString(R.string.other_hint)
                viewModel.summaryEditVisibility.value = View.VISIBLE
            }
            val evaluate = EvaluateType.valueOf(summary.evaluation?.toUpperCase()?: "UNSELECTED")
            viewModel.evaluationSelectedItem.value = evaluate
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

            viewModel.fixedPhraseSelectedItem.value = null
            val phrases = viewModel.retrieveReportFixedPhrases()
            var phraseIndex = phrases.count() + 1
            phrases.forEachIndexed { index, phrase ->
                if (phrase == summary.comment) {
                    phraseIndex = index + 1
                    viewModel.fixedPhraseSelectedItem.value = phrase
                }
            }
            viewModel.fixedPhraseId.value = if(summary.comment.isNullOrEmpty()) { 0 } else { phraseIndex }
            if (phraseIndex == phrases.count() + 1) {
                viewModel.fixedPhraseSelectedItem.value = ErikuraApplication.instance.getString(R.string.other_hint)
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
    val job: MutableLiveData<Job> = MutableLiveData()

    val title: MutableLiveData<String> = MutableLiveData()
    val summaryItems: MutableLiveData<List<String>> = MutableLiveData(listOf())
    val summaryId: MutableLiveData<Int> = MutableLiveData()
    val summaryEditVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val summary: MutableLiveData<String> = MutableLiveData()
    val summaryError: ErrorMessageViewModel = ErrorMessageViewModel()
    val statusId: MutableLiveData<Int> = MutableLiveData()
    val fixedPhraseId = MutableLiveData<Int>()
    val comment: MutableLiveData<String> = MutableLiveData()
    val commentError: ErrorMessageViewModel = ErrorMessageViewModel()
    val commentEditVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val evaluateTypes = EvaluateType.values()
    val evaluateLabels: List<String> = evaluateTypes.map { ErikuraApplication.applicationContext.getString(it.resourceId) }

    val summarySelectedItem = MutableLiveData<String>()
    val evaluationSelectedItem = MutableLiveData<EvaluateType>(EvaluateType.UNSELECTED)
    val fixedPhraseSelectedItem = MutableLiveData<String>()

    val fixedPhraseItems = MediatorLiveData<List<String>>().also { result ->
        result.addSource(job) {
            result.value = buildFixedPhraseItems()
        }
        result.addSource(evaluationSelectedItem) {
            result.value = buildFixedPhraseItems()
        }
    }

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(summaryEditVisibility) {result.value = isValid() }
        result.addSource(summary) { result.value = isValid() }
        result.addSource(statusId) { result.value = isValid() }
        result.addSource(commentEditVisibility) { result.value = isValid() }
        result.addSource(comment) { result.value = isValid() }
    }
    val reportExamplesButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

    val closeButtonVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(job) {
            job.value?.report?.id?.also {
                result.value = View.GONE
            } ?: run{
                result.value = View.VISIBLE
            }
        }
    }

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
        valid = summarySelectedItem.value != null && valid
        valid = evaluationSelectedItem.value != null && valid
        valid = fixedPhraseSelectedItem.value != null && valid
        return valid
    }

    private fun isValidSummary(): Boolean {
        var valid = true
        if (valid && summary.value.isNullOrBlank()) {
            valid = false
            summaryError.message.value = null
        }else if (valid && summary.value?.length?: 0 > ErikuraConst.maxOutputSummaryTitleLength) {
            valid = false
            summaryError.message.value = ErikuraApplication.instance.getString(R.string.summary_count_error, ErikuraConst.maxOutputSummaryTitleLength)
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

    private fun buildFixedPhraseItems(): List<String> {
        val items: MutableList<String> = mutableListOf()
        items.add(ErikuraApplication.instance.getString(R.string.please_select))
        items.addAll(retrieveReportFixedPhrases())
        if (evaluationSelectedItem?.value != EvaluateType.UNSELECTED) {
            items.add(ErikuraApplication.instance.getString(R.string.other_hint))
        }
        return items
    }

    fun retrieveReportFixedPhrases(): List<String> {
        return job.value?.jobKind?.let { jobKind ->
            evaluationSelectedItem.value?.let { evaluation ->
                when (evaluation) {
                    EvaluateType.UNSELECTED -> listOf()
                    EvaluateType.BAD -> jobKind.reportFixedPhrasesC
                    EvaluateType.ORDINARY -> jobKind.reportFixedPhrasesB
                    EvaluateType.GOOD -> jobKind.reportFixedPhrasesA
                }
            }
        } ?: listOf()
    }
}

interface ReportFormEventHandlers {
    fun onClickNext(view: View)
    fun onSummarySelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    fun onEvaluationSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    fun onFixedPhraseSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    fun onClickManual(view: View)
    fun onClickReportExamples(view: View)
    fun onClickClose(view: View)
}