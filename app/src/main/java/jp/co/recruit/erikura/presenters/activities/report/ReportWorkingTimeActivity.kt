package jp.co.recruit.erikura.presenters.activities.report

import android.app.Activity
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
    var fromConfirm = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_working_time)

        val binding: ActivityReportWorkingTimeBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_working_time)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
    }

    override fun onStart() {
        super.onStart()
        job = intent.getParcelableExtra<Job>("job")
        fromConfirm = intent.getBooleanExtra("fromConfirm", false)
        createTimeItems()
        loadData()
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
            if (fromConfirm) {
                val intent= Intent()
                intent.putExtra("job", job)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }else {
                val intent= Intent(this, ReportOtherFormActivity::class.java)
                intent.putExtra("job", job)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
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

    private fun loadData() {
        job.report?.let {
            viewModel.timeId.value = it.workingMinute?: 0
        }
    }
}

class ReportWorkingTimeViewModel: ViewModel() {
    val timeItems: MutableLiveData<List<String>> = MutableLiveData(listOf())
    val timeId: MutableLiveData<Int> = MutableLiveData()
    var timeSelectedItem: Int = 0
}

interface ReportWorkingTimeEventHandlers {
    fun onClickNext(view: View)
    fun onTimeSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
    fun onClickManual(view: View)
}
