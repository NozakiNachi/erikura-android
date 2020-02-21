package jp.co.recruit.erikura.presenters.activities.report

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.ActivityReportWorkingTimeBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity

class ReportWorkingTimeActivity : AppCompatActivity(), ReportWorkingTimeEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportWorkingTimeViewModel::class.java)
    }

    var job = Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_working_time)

        job = intent.getParcelableExtra<Job>("job")

        val binding: ActivityReportWorkingTimeBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_working_time)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        createTimeItems()
    }

    override fun onClickManual(view: View) {
        if(job.manualUrl != null){
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
            it.workingMinute = viewModel.timeSelectedItem
            //FIXME: マニュアル外報告画面へ遷移
            Log.v("DEBUG", "マニュアル外報告画面へ遷移")
        }
    }

    override fun onTimeSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        viewModel.timeSelectedItem = position
    }

    private fun createTimeItems() {
        val times: MutableList<String> = mutableListOf()
        times.add("")
        for (min in 1..480) {
            times.add("${min}分")
        }
        viewModel.timeItems.value = times
    }
}

class ReportWorkingTimeViewModel: ViewModel() {
    val timeItems: MutableLiveData<List<String>> = MutableLiveData(listOf())
    var timeSelectedItem: Int = 0
}

interface ReportWorkingTimeEventHandlers {
    fun onClickNext(view: View)
    fun onTimeSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
    fun onClickManual(view: View)
}
