package jp.co.recruit.erikura.presenters.activities.report

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.ActivityReportEvaluationBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity

class ReportEvaluationActivity : AppCompatActivity(), ReportEvaluationEventHandler {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportEvaluationViewModel::class.java)
    }
    var job = Job()
    var fromConfirm = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_evaluation)

        val binding: ActivityReportEvaluationBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_evaluation)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
    }

    override fun onStart() {
        super.onStart()
        job = intent.getParcelableExtra<Job>("job")
        fromConfirm = intent.getBooleanExtra("fromConfirm", false)
        loadData()
    }

    override fun onClickGood(view: View) {
        viewModel.bad.value = false
    }

    override fun onClickBad(view: View) {
        viewModel.good.value = false
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
    }

}

class ReportEvaluationViewModel: ViewModel() {
    val good: MutableLiveData<Boolean> = MutableLiveData()
    val bad: MutableLiveData<Boolean> = MutableLiveData()
    val comment: MutableLiveData<String> = MutableLiveData()
    val commentErrorMsg: MutableLiveData<String> = MutableLiveData()
    val commentErrorVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(comment) { result.value = isValid()  }
    }

    private fun isValid(): Boolean {
        var valid = true
        if (valid && comment.value?.length?: 0 > 5000) {
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

interface ReportEvaluationEventHandler {
    fun onClickNext(view: View)
    fun onClickGood(view: View)
    fun onClickBad(view: View)
    fun onClickManual(view: View)
}