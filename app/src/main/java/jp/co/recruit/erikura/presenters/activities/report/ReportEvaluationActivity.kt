package jp.co.recruit.erikura.presenters.activities.report

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.ActivityReportEvaluationBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity

class ReportEvaluationActivity : AppCompatActivity(), ReportEvaluationEventHandler {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportEvaluationViewModel::class.java)
    }
    var job = Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_evaluation)

        job = intent.getParcelableExtra<Job>("job")

        val binding: ActivityReportEvaluationBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_evaluation)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
    }

    override fun onClickGood(view: View) {

    }

    override fun onClickBad(view: View) {

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
        //FIXME: 作業報告確認画面へ遷移
    }

}

class ReportEvaluationViewModel: ViewModel() {
    val good: MutableLiveData<Boolean> = MutableLiveData()
    val bad: MutableLiveData<Boolean> = MutableLiveData()
    val comment: MutableLiveData<String> = MutableLiveData()
    val commentErrorMsg: MutableLiveData<String> = MutableLiveData()
    val commentErrorVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
}

interface ReportEvaluationEventHandler {
    fun onClickNext(view: View)
    fun onClickGood(view: View)
    fun onClickBad(view: View)
    fun onClickManual(view: View)
}