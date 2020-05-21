package jp.co.recruit.erikura.presenters.activities.job

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityWorkingFinishedBinding
import jp.co.recruit.erikura.databinding.DialogAlertAbleEndBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.OwnJobsActivity
import jp.co.recruit.erikura.presenters.activities.report.ReportImagePickerActivity
import java.util.*

class WorkingFinishedActivity : BaseActivity(), WorkingFinishedEventHandlers {
    private val viewModel: WorkingFinishedViewModel by lazy {
        ViewModelProvider(this).get(WorkingFinishedViewModel::class.java)
    }

    var job: Job = Job()
    var message: String? = null
    private lateinit var recommendedJobsAdapter: JobListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        job = intent.getParcelableExtra<Job>("job")
        message = intent.getStringExtra("message")
        viewModel.message.value = message
        Log.v("DEBUG", job.toString())

        val binding: ActivityWorkingFinishedBinding = DataBindingUtil.setContentView(this, R.layout.activity_working_finished)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        recommendedJobsAdapter = JobListAdapter(this, listOf(), null).also {
            it.onClickListner =  object: JobListAdapter.OnClickListener {
                override fun onClick(job: Job) {
                    onJobSelected(job)
                }
            }
        }
        val jobList: RecyclerView = findViewById(R.id.applyCompleted_recommend)
        jobList.setHasFixedSize(true)
        jobList.adapter = recommendedJobsAdapter
        jobList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        jobList.addItemDecoration(JobListItemDecorator())
    }

    override fun onStart() {
        super.onStart()
        //警告メッセージがある場合ダイアログを表示する
        if (message != null) {
            val binding: DialogAlertAbleEndBinding = DataBindingUtil.inflate(
                LayoutInflater.from(this), R.layout.dialog_alert_able_end, null, false)
            binding.lifecycleOwner = this
            binding.viewModel = viewModel
            val dialog = AlertDialog.Builder(this)
                .setView(binding.root)
                .setPositiveButton("確認", null)
                .create()
            dialog.show()
            val confirmation: Button = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            confirmation.setOnClickListener(View.OnClickListener {
                fun onClick(view: View) {
                    dialog.dismiss()
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        // jobの再取得
        Api(this).reloadJob(job) {
            job = it
            Api(this).recommendedJobs(job) { jobsList ->
                viewModel.recommendedJobs = jobsList
                recommendedJobsAdapter.jobs = viewModel.recommendedJobs
                recommendedJobsAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onClickReport(view: View) {
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "push_report_job", params= bundleOf())
        Tracking.track(name= "push_report_job")

        if (job.entry?.limitAt ?: Date() > Date()) {
            val intent = Intent(this, ReportImagePickerActivity::class.java)
            intent.putExtra("job", job)
            startActivity(intent)
        }else {
            val errorMessages =
                mutableListOf(ErikuraApplication.instance.getString(R.string.jobDetails_overLimit))
            Api(this).displayErrorAlert(errorMessages)
        }
    }

    override fun onClickAppliedJobs(view: View) {
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "push_display_job_list", params= bundleOf())
        Tracking.track(name= "push_display_job_list")

        Intent(this, OwnJobsActivity::class.java).let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra("fromWorkingFinished", true)
            startActivity(intent)
        }
    }

    fun onJobSelected(job: Job) {
        val intent= Intent(this, JobDetailsActivity::class.java)
        intent.putExtra("job", job)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
    }
}

class WorkingFinishedViewModel: ViewModel() {
    var recommendedJobs: List<Job> = listOf()
    var message: MutableLiveData<String> = MutableLiveData()
}

interface WorkingFinishedEventHandlers {
    fun onClickReport(view: View)
    fun onClickAppliedJobs(view: View)
}