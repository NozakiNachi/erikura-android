package jp.co.recruit.erikura.presenters.activities.job

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import jp.co.recruit.erikura.business.models.ErikuraConfig
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
    var cautionsCount: Int? = null
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
        if (!(message.isNullOrEmpty())) {
            val binding: DialogAlertAbleEndBinding = DataBindingUtil.inflate(
                LayoutInflater.from(this), R.layout.dialog_alert_able_end, null, false)
            binding.lifecycleOwner = this
            binding.viewModel = viewModel
            val dialog = AlertDialog.Builder(this)
                .setView(binding.root)
                .create()
            dialog.show()
        }
    }

    override fun onResume() {
        super.onResume()
        // jobの再取得
        Api(this).reloadJob(job) { get_job->
            job = get_job
            cautionsCount = get_job.cautionsCount
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

    override fun onClickTransitionWebModal(view: View) {
        // WEB遷移確認モーダルを表示する
        BaseActivity.currentActivity?.let { activity ->
            val dialog = AlertDialog.Builder(activity)
                .setView(R.layout.dialog_confirm_transition_web)
                .setCancelable(false)
                .create()
            dialog.show()
            val button: Button = dialog.findViewById(R.id.open_button)
            button.setOnClickListener(View.OnClickListener {
                //開く場合
                dialog.dismiss()
                Api(activity).createToken() { token ->
                    //カスタマWebの作業報告新規作成画面を開く
                    val jobNewReportURLString = ErikuraConfig.jobNewReportURLString(job.id, token)
                    Uri.parse(jobNewReportURLString)?.let { uri ->
                        try {
                            Intent(Intent.ACTION_VIEW, uri).let { intent ->
                                intent.setPackage("com.android.chrome")
                                startActivity(intent)
                            }
                        } catch (e: ActivityNotFoundException) {
                            Intent(Intent.ACTION_VIEW, uri).let { intent ->
                                startActivity(intent)
                            }
                        }
                    }
                }
            })
            val cancelButton: Button = dialog.findViewById(R.id.cancel_button)
            cancelButton.setOnClickListener(View.OnClickListener {
                //キャンセル場合
                dialog.dismiss()
            })
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
    fun onClickTransitionWebModal(view: View)
}