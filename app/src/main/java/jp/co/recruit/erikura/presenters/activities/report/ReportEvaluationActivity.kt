package jp.co.recruit.erikura.presenters.activities.report

import JobUtil
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.ErikuraConst
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.ActivityReportEvaluationBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel

class ReportEvaluationActivity : BaseActivity(), ReportEvaluationEventHandler {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportEvaluationViewModel::class.java)
    }
    var job = Job()
    var fromConfirm = false
    var editCompleted = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityReportEvaluationBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_evaluation)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        job = intent.getParcelableExtra<Job>("job")
        fromConfirm = intent.getBooleanExtra("fromConfirm", false)
        ErikuraApplication.instance.reportingJob = job
    }

    override fun onStart() {
        super.onStart()
        ErikuraApplication.instance.reportingJob?.let {
            job = it
        }
        if (editCompleted) {
            loadData()
            editCompleted = false
        }

        if (job.reportId == null) {
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_job_report_rating", params= bundleOf())
            Tracking.viewJobDetails(name= "/reports/register/evaluation/${job.id}", title= "作業報告画面（案件評価）", jobId= job.id)
        }else {
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_edit_job_report_rating", params= bundleOf())
            Tracking.viewJobDetails(name= "/reports/edit/evaluation/${job.id}", title= "作業報告編集画面（案件評価）", jobId= job.id)
        }
        //お手本報告件数が0件の場合非表示
        job.goodExamplesCount?.let { reportExampleCount ->
            if (reportExampleCount == 0) {
                viewModel.reportExamplesButtonVisibility.value = View.GONE
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val view = this.currentFocus
        if (view != null) {
            val layout = findViewById<FrameLayout>(R.id.report_evaluation_layout)
            layout.requestFocus()

            val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(layout.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onClickGood(view: View) {
        viewModel.bad.value = false
    }

    override fun onClickBad(view: View) {
        viewModel.good.value = false
    }

    override fun onClickManual(view: View) {
        if(job?.manualUrl != null){
            JobUtil.openManual(this, job!!)
        }
    }

    override fun onClickNext(view: View) {
        job.report?.let {
            if (viewModel.good.value?: false) {
                it.evaluation = "good"
            }else if (viewModel.bad.value?: false) {
                it.evaluation = "bad"
            }else {
                it.evaluation = null
            }
            it.comment = viewModel.comment.value
            editCompleted = true
            if (fromConfirm) {
                val intent= Intent()
                intent.putExtra("job", job)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }else {
                val intent= Intent(this, ReportConfirmActivity::class.java)
                intent.putExtra("job", job)
                startActivity(intent)
            }
        }
    }

    override fun onClickReportExamples(view: View) {
        job?.let { job ->
            JobUtil.openReportExample(this, job)
        }
    }

    private fun loadData() {
        job.report?.let {
            val evaluation = it.evaluation
            when(evaluation) {
                "good" -> {
                    viewModel.good.value = true
                    viewModel.bad.value = false
                }
                "bad" -> {
                    viewModel.good.value = false
                    viewModel.bad.value = true
                }
                else -> {
                    viewModel.good.value = false
                    viewModel.bad.value = false
                }
            }
            viewModel.comment.value = it.comment
        }
        viewModel.commentError.message.value = null
    }
}

class ReportEvaluationViewModel: ViewModel() {
    val good: MutableLiveData<Boolean> = MutableLiveData()
    val bad: MutableLiveData<Boolean> = MutableLiveData()
    val comment: MutableLiveData<String> = MutableLiveData()
    val commentError: ErrorMessageViewModel = ErrorMessageViewModel()

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(comment) {
            result.value = isValid()
        }
    }
    val reportExamplesButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

    private fun isValid(): Boolean {
        var valid = true
        if (valid && comment.value?.length?: 0 > ErikuraConst.maxCommentLength) {
            valid = false
            commentError.message.value = ErikuraApplication.instance.getString(R.string.comment_count_error, ErikuraConst.maxCommentLength)
        }else {
            valid = true
            commentError.message.value = null
        }
        return valid
    }
}

interface ReportEvaluationEventHandler {
    fun onClickNext(view: View)
    fun onClickGood(view: View)
    fun onClickBad(view: View)
    fun onClickManual(view: View)
    fun onClickReportExamples(view: View)
}