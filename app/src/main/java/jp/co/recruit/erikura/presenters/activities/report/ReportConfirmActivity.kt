package jp.co.recruit.erikura.presenters.activities.report

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.MediaItem
import jp.co.recruit.erikura.databinding.ActivityReportConfirmBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity


class ReportConfirmActivity : AppCompatActivity(), ReportConfirmEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportConfirmViewModel::class.java)
    }
    var job = Job()
    private val EDIT_DATA: Int = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_confirm)

        val binding: ActivityReportConfirmBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_confirm)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        job = intent.getParcelableExtra<Job>("job")
        loadData()
    }

    override fun onClickComplete(view: View) {
        // FIXME: 作業報告完了処理
    }

    override fun onClickAddPhotoButton(view: View) {
        // FIXME: ギャラリーへのアクセス
    }

    override fun onClickEditEvaluation(view: View) {
        // FIXME: 評価編集画面へ遷移
    }

    override fun onClickEditOtherForm(view: View) {
        // FIXME: マニュアル外報告編集画面へ遷移
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
            val comment = it.comment?: ""
            viewModel.evaluationComment.value = comment
        }
    }
}

class ReportConfirmViewModel: ViewModel() {
    val workingTime: MutableLiveData<String> = MutableLiveData()
    val otherFormImageVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val otherFormComment: MutableLiveData<String> = MutableLiveData()
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