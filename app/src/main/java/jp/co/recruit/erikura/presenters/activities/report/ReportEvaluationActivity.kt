package jp.co.recruit.erikura.presenters.activities.report

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.storage.Asset
import jp.co.recruit.erikura.databinding.ActivityReportEvaluationBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
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
        setContentView(R.layout.activity_report_evaluation)

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

    }

    override fun onClickGood(view: View) {
        viewModel.bad.value = false
    }

    override fun onClickBad(view: View) {
        viewModel.good.value = false
    }

    override fun onClickManual(view: View) {
        if(job?.manualUrl != null){
            val manualUrl = job.manualUrl
            val assetsManager = ErikuraApplication.assetsManager
            Api(this).showProgressAlert()
            assetsManager.fetchAsset(this, manualUrl!!, Asset.AssetType.Pdf) { asset ->
                val intent = Intent(this, WebViewActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse(asset.url)
                }
                Api(this).hideProgressAlert()
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
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
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
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
//    val commentErrorMsg: MutableLiveData<String> = MutableLiveData()
//    val commentErrorVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val commentError: ErrorMessageViewModel = ErrorMessageViewModel()

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(comment) { result.value = isValid()  }
    }

    private fun isValid(): Boolean {
        var valid = true
        if (valid && comment.value?.length?: 0 > 5000) {
            valid = false
//            commentErrorMsg.value = ErikuraApplication.instance.getString(R.string.comment_count_error)
//            commentErrorVisibility.value = View.VISIBLE
            commentError.message.value = ErikuraApplication.instance.getString(R.string.comment_count_error)
        }else {
            valid = true
//            commentErrorMsg.value = ""
//            commentErrorVisibility.value = View.GONE
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
}