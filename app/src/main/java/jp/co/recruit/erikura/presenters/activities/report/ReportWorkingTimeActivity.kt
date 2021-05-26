package jp.co.recruit.erikura.presenters.activities.report

import JobUtil
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.data.storage.ReportDraft
import jp.co.recruit.erikura.databinding.ActivityReportWorkingTimeBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity

class ReportWorkingTimeActivity : BaseActivity(), ReportWorkingTimeEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportWorkingTimeViewModel::class.java)
    }

    var job = Job()
    var fromConfirm = false
    var editCompleted = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_working_time)

        val binding: ActivityReportWorkingTimeBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_working_time)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        if (ErikuraApplication.instance.currentJob != null) {
            job = ErikuraApplication.instance.currentJob!!
        }
        else {
            // 案件情報が取れない場合
            Intent(this, MapViewActivity::class.java).let {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                it.putStringArrayListExtra(
                    ErikuraApplication.ERROR_MESSAGE_KEY,
                    arrayListOf("お仕事の情報を取得できませんでした。予期せぬエラーによりアプリが終了した可能性ございます。お手数ですがもう一度はじめから操作してください。?")
                )
                this.startActivity(it)
            }
            Log.v(ErikuraApplication.LOG_TAG,  "Cannot retrieve job")
            // FirebaseCrashlytics に案件がnull出会ったことを記録します
            val e = Throwable("ErikuraApplication.currentJob is null")
            FirebaseCrashlytics.getInstance().recordException(e)
            return
        }
        fromConfirm = intent.getBooleanExtra("fromConfirm", false)
        ErikuraApplication.instance.currentJob = job
    }

    override fun onStart() {
        super.onStart()
        ErikuraApplication.instance.currentJob?.let {
            job = it
        }
        createTimeItems()
        if (editCompleted) {
            loadData()
            editCompleted = false
        }

        if (job.reportId == null) {
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_job_report_time", params= bundleOf())
            Tracking.viewJobDetails(name= "/reports/register/time/${job.id}", title= "作業報告画面（作業時間）", jobId= job.id)
        }else {
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_edit_job_report_time", params= bundleOf())
            Tracking.viewJobDetails(name= "/reports/edit/time/${job.id}", title= "作業報告編集画面（作業時間）", jobId= job.id)
        }
        //お手本報告件数が0件の場合非表示
        job.goodExamplesCount?.let { reportExampleCount ->
            if (reportExampleCount == 0) {
                viewModel.reportExamplesButtonVisibility.value = View.GONE
            }
        }
        viewModel.job.value = job
//        FirebaseCrashlytics.getInstance().recordException(MemoryTraceException(this.javaClass.name, getAvailableMemory()))
    }

    override fun onStop() {
        super.onStop()

        // GCをかけておきます
        System.gc()
    }

    override fun onClickManual(view: View) {
        if(job.manualUrl != null){
            JobUtil.openManual(this, job!!)
        }
    }

    override fun onBackPressed() {
        if (!fromConfirm) {
            job.report?.let { report ->
                report.workingMinute = viewModel.timeSelectedItem

                val summaries = job.report?.outputSummaries ?: listOf()
                var pictureIndex = summaries.count() - 1
                while (pictureIndex >= 0 && summaries[pictureIndex]?.willDelete == true) {
                    pictureIndex--
                }

                if (pictureIndex >= 0) {
                    JobUtils.saveReportDraft(
                        job,
                        step = ReportDraft.ReportStep.SummaryForm,
                        summaryIndex = pictureIndex
                    )
                    val intent = Intent(this, ReportFormActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    intent.putExtra("job", job)
                    intent.putExtra("pictureIndex", pictureIndex)
                    startActivity(intent)
                } else {
                    // 実施箇所が１件もない場合、作業報告中断モーダルを表示する
                    JobUtil.displaySuspendReportConfirmation(this, ReportDraft.ReportStep.WorkingTimeForm, job, null)
                }
            }
        }
        else {
            super.onBackPressed()
        }
    }

    override fun onClickNext(view: View) {
        job.report?.let {
            it.workingMinute = viewModel.timeSelectedItem
            editCompleted = true


            if (fromConfirm) {
                JobUtils.saveReportDraft(job, step = ReportDraft.ReportStep.Confirm)
                val intent= Intent()
                intent.putExtra("job", job)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }else {
                JobUtils.saveReportDraft(job, step = ReportDraft.ReportStep.OtherForm)
                val intent= Intent(this, ReportOtherFormActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("job", job)
                startActivity(intent)
            }
        }
    }

    override fun onTimeSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.timeSelectedItem = position
    }

    override fun onClickReportExamples(view: View) {
        job?.let { job ->
            JobUtil.openReportExample(this, job)
        }
    }

    override fun onClickClose(view: View) {
        job.report?.workingMinute = viewModel.timeSelectedItem
        JobUtil.displaySuspendReportConfirmation(this, ReportDraft.ReportStep.WorkingTimeForm, job, null)
    }


    private fun createTimeItems() {
        val times: MutableList<String> = mutableListOf()
        times.add("")
        for (min in 1..480) {
            times.add("${min}分")
        }
        viewModel.timeItems.value = times
    }

    private fun loadData() {
        job.report?.let {
            viewModel.timeId.value = it.workingMinute?: 0
        }
    }
}

class ReportWorkingTimeViewModel: ViewModel() {
    val job: MutableLiveData<Job> = MutableLiveData()
    val timeItems: MutableLiveData<List<String>> = MutableLiveData(listOf())
    val timeId: MutableLiveData<Int> = MutableLiveData()
    var timeSelectedItem: Int = 0
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
}

interface ReportWorkingTimeEventHandlers {
    fun onClickNext(view: View)
    fun onTimeSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
    fun onClickManual(view: View)
    fun onClickReportExamples(view: View)
    fun onClickClose(view: View)
}
