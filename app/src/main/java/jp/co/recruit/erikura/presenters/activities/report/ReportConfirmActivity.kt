package jp.co.recruit.erikura.presenters.activities.report

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.ActivityReportConfirmBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity


class ReportConfirmActivity : AppCompatActivity(), ReportConfirmEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportConfirmViewModel::class.java)
    }
    var job = Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_confirm)

        job = intent.getParcelableExtra<Job>("job")

        val binding: ActivityReportConfirmBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_confirm)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
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
        // FIXME: 作業時間編集画面へ遷移
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
}

class ReportConfirmViewModel: ViewModel() {

}

interface ReportConfirmEventHandlers {
    fun onClickComplete(view: View)
    fun onClickAddPhotoButton(view: View)
    fun onClickEditOtherForm(view: View)
    fun onClickEditWorkingTime(view: View)
    fun onClickEditEvaluation(view: View)
    fun onClickManual(view: View)
}