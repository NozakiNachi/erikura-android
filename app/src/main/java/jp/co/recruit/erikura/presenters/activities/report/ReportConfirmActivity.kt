package jp.co.recruit.erikura.presenters.activities.report

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.MediaItem
import jp.co.recruit.erikura.business.models.OutputSummary
import jp.co.recruit.erikura.databinding.ActivityReportConfirmBinding
import jp.co.recruit.erikura.databinding.FragmentReportImageItemBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity


class ReportConfirmActivity : AppCompatActivity(), ReportConfirmEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportConfirmViewModel::class.java)
    }
    var job = Job()
    private val EDIT_DATA: Int = 1001
    private lateinit var reportImageAdapter: ReportImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_confirm)

        val binding: ActivityReportConfirmBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_confirm)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        job = intent.getParcelableExtra<Job>("job")

        reportImageAdapter = ReportImageAdapter(this, listOf())
        val reportImageView: RecyclerView = findViewById(R.id.report_confirm_report_images)
        reportImageView.adapter = reportImageAdapter

        loadData()
    }

    override fun onClickComplete(view: View) {
        // FIXME: 作業報告完了処理
    }

    override fun onClickAddPhotoButton(view: View) {
        // FIXME: ギャラリーへのアクセス
    }

    override fun onClickEditEvaluation(view: View) {
        val intent= Intent(this, ReportEvaluationActivity::class.java)
        intent.putExtra("job", job)
        intent.putExtra("fromConfirm", true)
        startActivityForResult( intent, EDIT_DATA, ActivityOptions.makeSceneTransitionAnimation(this).toBundle() )
    }

    override fun onClickEditOtherForm(view: View) {
        val intent= Intent(this, ReportOtherFormActivity::class.java)
        intent.putExtra("job", job)
        intent.putExtra("fromConfirm", true)
        startActivityForResult( intent, EDIT_DATA, ActivityOptions.makeSceneTransitionAnimation(this).toBundle() )
    }

    override fun onClickEditWorkingTime(view: View) {
        val intent= Intent(this, ReportWorkingTimeActivity::class.java)
        intent.putExtra("job", job)
        intent.putExtra("fromConfirm", true)
        startActivityForResult( intent, EDIT_DATA, ActivityOptions.makeSceneTransitionAnimation(this).toBundle() )
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == EDIT_DATA) {
            if (resultCode == Activity.RESULT_OK) {
                data?.let {
                    job = data.getParcelableExtra<Job>("job")
                    loadData()
                }
            }
        }
    }

    private fun loadData() {
        job.report?.let {
            reportImageAdapter.summaries = it.outputSummaries
            val minute = it.workingMinute?: 0
            viewModel.workingTime.value = if(minute == 0){""}else {"${minute}分"}
            val item = it.additionalPhotoAsset?: MediaItem()
            if (item.contentUri != null) {
                val imageView: ImageView = findViewById(R.id.report_confirm_other_image)
                item.loadImage(this, imageView)
                viewModel.otherFormImageVisibility.value = View.VISIBLE
            }else {
                viewModel.otherFormImageVisibility.value = View.GONE
            }
            val additionalComment = it.additionalComment?: ""
            viewModel.otherFormComment.value = additionalComment
            val evaluation = it.evaluation?: ""
            when(evaluation) {
                "good" ->
                    viewModel.evaluate.value = true
                "bad" ->
                    viewModel.evaluate.value = false
            }
            viewModel.evaluateButtonVisibility.value = if (evaluation.isNullOrEmpty()) {View.GONE} else {View.VISIBLE}
            val comment = it.comment?: ""
            viewModel.evaluationComment.value = comment
        }
    }
}

class ReportConfirmViewModel: ViewModel() {
    val workingTime: MutableLiveData<String> = MutableLiveData()
    val otherFormImageVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val otherFormComment: MutableLiveData<String> = MutableLiveData()
    val evaluate: MutableLiveData<Boolean> = MutableLiveData()
    val evaluateButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val evaluationComment: MutableLiveData<String> = MutableLiveData()
}

interface ReportConfirmEventHandlers {
    fun onClickComplete(view: View)
    fun onClickAddPhotoButton(view: View)
    fun onClickEditOtherForm(view: View)
    fun onClickEditWorkingTime(view: View)
    fun onClickEditEvaluation(view: View)
    fun onClickManual(view: View)
}

// 実施箇所の一覧
class ReportImageItemViewModel(activity: Activity, view: View, mediaItem: MediaItem?): ViewModel() {
    private val imageView: ImageView = view.findViewById(R.id.report_image_item)
    init {
        mediaItem?.let {
            mediaItem.loadImage(activity, imageView)
        }
    }
}

class ReportImageViewHolder(val binding: FragmentReportImageItemBinding): RecyclerView.ViewHolder(binding.root)

class ReportImageAdapter(val activity: FragmentActivity, var summaries: List<OutputSummary>): RecyclerView.Adapter<ReportImageViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportImageViewHolder {
        val binding = DataBindingUtil.inflate<FragmentReportImageItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.fragment_report_image_item,
            parent,
            false
        )

        val view = binding.root
        val height = (parent.measuredWidth-40) / 3
        (view.layoutParams as? RecyclerView.LayoutParams)?.let { layoutParams ->
            layoutParams.height = height
            view.layoutParams = layoutParams
        }

        return ReportImageViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return summaries.count()
    }

    override fun onBindViewHolder(holder: ReportImageViewHolder, position: Int) {
        val view = holder.binding.root
        holder.binding.lifecycleOwner = activity
        holder.binding.viewModel = ReportImageItemViewModel(activity, view, summaries[position].photoAsset)
    }
}