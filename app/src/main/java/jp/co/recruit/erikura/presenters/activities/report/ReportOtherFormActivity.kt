package jp.co.recruit.erikura.presenters.activities.report

import JobUtil
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.ExifInterface
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.realm.Realm
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.ErikuraConst
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.MediaItem
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.data.storage.PhotoTokenManager
import jp.co.recruit.erikura.data.storage.ReportDraft
import jp.co.recruit.erikura.databinding.ActivityReportOtherFormBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel
import jp.co.recruit.erikura.presenters.util.MessageUtils
import java.util.*

class ReportOtherFormActivity : BaseActivity(), ReportOtherFormEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportOtherFormViewModel::class.java)
    }
    private val realm: Realm get() = ErikuraApplication.realm

    var job = Job()
    var fromConfirm = false
    var fromGallery = false
    var editCompleted = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_other_form)

        val binding: ActivityReportOtherFormBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_report_other_form)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
//        job = intent.getParcelableExtra<Job>("job")
//        ErikuraApplication.instance.reportingJob = job
        if (ErikuraApplication.instance.currentJob != null) {
            job = ErikuraApplication.instance.currentJob!!
        } else {
            // 案件情報が取れない場合
            Intent(this, MapViewActivity::class.java).let {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                it.putStringArrayListExtra(
                    ErikuraApplication.ERROR_MESSAGE_KEY,
                    arrayListOf("お仕事の情報を取得できませんでした。予期せぬエラーによりアプリが終了した可能性ございます。お手数ですがもう一度はじめから操作してください。?")
                )
                this.startActivity(it)
            }
            Log.v(ErikuraApplication.LOG_TAG, "Cannot retrieve job")
            // FirebaseCrashlytics に案件がnull出会ったことを記録します
            val e = Throwable("ErikuraApplication.currentJob is null")
            FirebaseCrashlytics.getInstance().recordException(e)
            return
        }
        fromConfirm = intent.getBooleanExtra("fromConfirm", false)
    }

    override fun onStart() {
        super.onStart()
        ErikuraApplication.instance.currentJob?.let {
            job = it
        }
        if (editCompleted) {
            if (!fromGallery) {
                loadData()
            }
        }
        editCompleted = false
        fromGallery = false

        if (job.reportId == null) {
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event = "view_job_report_others", params = bundleOf())
            Tracking.viewJobDetails(
                name = "/reports/register/additional/${job.id}",
                title = "作業報告画面（マニュアル外）",
                jobId = job.id
            )
        } else {
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event = "view_edit_job_report_others", params = bundleOf())
            Tracking.viewJobDetails(
                name = "/reports/edit/additional/${job.id}",
                title = "作業報告編集画面（マニュアル外）",
                jobId = job.id
            )
        }
        //お手本報告件数が0件の場合非表示
        job.goodExamplesCount?.let { reportExampleCount ->
            if (reportExampleCount == 0) {
                viewModel.reportExamplesButtonVisibility.value = View.GONE
            }
        }
//        FirebaseCrashlytics.getInstance().recordException(MemoryTraceException(this.javaClass.name, getAvailableMemory()))
    }

    override fun onStop() {
        super.onStop()

        // GCをかけておきます
        System.gc()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val view = this.currentFocus
        if (view != null) {
            val layout = findViewById<FrameLayout>(R.id.report_other_form_layout)
            layout.requestFocus()

            val imm: InputMethodManager =
                getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(layout.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onClickManual(view: View) {
        if (job?.manualUrl != null) {
            JobUtil.openManual(this, job!!)
        }
    }

    override fun onClickAddPhotoButton(view: View) {
        if (ErikuraApplication.instance.hasStoragePermission(this)) {
            moveToGallery()
        } else {
            ErikuraApplication.instance.requestStoragePermission(this)
        }

    }

    override fun onClickRemovePhoto(view: View) {
        viewModel.otherPhoto = MediaItem()
        val imageView: ImageView = findViewById(R.id.report_other_image)
        imageView.setImageDrawable(null)
        viewModel.addPhotoButtonVisibility.value = View.VISIBLE
        viewModel.removePhotoButtonVisibility.value = View.GONE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            ErikuraApplication.REQUEST_EXTERNAL_STORAGE_PERMISSION_ID -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    moveToGallery()
                } else {
                    val dialog = StorageAccessConfirmDialogFragment()
                    dialog.show(supportFragmentManager, "confirm")
                }
            }
        }
    }

    private fun moveToGallery() {
        val intent = Intent()
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(intent, 1000)
    }

    private fun loadData() {
        job.report?.let {
            val item = it.additionalPhotoAsset ?: MediaItem()
            val comment = it.additionalComment
            if (item.contentUri != null) {
                viewModel.addPhotoButtonVisibility.value = View.GONE
                viewModel.removePhotoButtonVisibility.value = View.VISIBLE
                val imageView: ImageView = findViewById(R.id.report_other_image)
                val width =
                    imageView.layoutParams.width / ErikuraApplication.instance.resources.displayMetrics.density
                val height =
                    imageView.layoutParams.height / ErikuraApplication.instance.resources.displayMetrics.density
                if (it.additionalReportPhotoUrl != null) {
                    item.loadImageFromString(this, imageView, width.toInt(), height.toInt())
                } else {
                    item.loadImage(this, imageView, width.toInt(), height.toInt())
                }
                viewModel.otherPhoto = item
                viewModel.comment.value = comment
            } else {
                viewModel.addPhotoButtonVisibility.value = View.VISIBLE
                viewModel.removePhotoButtonVisibility.value = View.GONE
                viewModel.otherPhoto = MediaItem()
            }
        }
        viewModel.commentError.message.value = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            uri?.let {
                MediaItem.createFrom(this, uri)?.let { item ->
                    val cr = this.contentResolver.openInputStream(uri)
                    val exifInterface = ExifInterface(cr)
                    val (imageWidth, imageHeight) = item.getWidthAndHeight(this, exifInterface)
                    var oldPictureFlag = false
                    var takenAt: Date? = null
                    item.dateTaken?.let { dateTaken ->
                        takenAt = Date(dateTaken)
                        val entryAt: Date? = if (job.entry?.fromPreEntry == true) {
                            // 先行応募で応募済みの場合
                            job.workingStartAt
                        } else {
                            // 通常案件の場合
                            job.entry?.createdAt
                        }
                        // 撮影日時が応募日時より古い場合
                        oldPictureFlag = takenAt!! < entryAt
                    }
                    // 横より縦の方が長い時アラートを表示します
                    if (imageHeight > imageWidth) {
                        MessageUtils.displayAlert(this, listOf("横長の画像のみ選択できます"))
                    } else if (oldPictureFlag) {
                        val dialog = AlertDialog.Builder(this)
                            .setView(R.layout.dialog_notice_old_taken_picture)
                            .setCancelable(false)
                            .create()
                        dialog.show()
                        val warningCaption: TextView? =
                            dialog.findViewById(R.id.dialog_warning_caption)
                        warningCaption?.setText(
                            String.format(ErikuraApplication.instance.getString(R.string.notice_old_taken_picture_caption),
                                takenAt?.let { it1 -> JobUtil.getFormattedDateJp(it1) })
                        )

                        val selectButton: Button = dialog.findViewById(R.id.select_button)
                        selectButton.setOnClickListener(View.OnClickListener {
                            addOtherPicture(item)
                            dialog.dismiss()
                        })
                        val cancelButton: Button = dialog.findViewById(R.id.cancel_button)
                        cancelButton.setOnClickListener(View.OnClickListener {
                            dialog.dismiss()
                        })
                    } else {
                        addOtherPicture(item)
                    }
                }
            }
        }
        fromGallery = true
    }

    private fun addOtherPicture(item: MediaItem) {
        viewModel.addPhotoButtonVisibility.value = View.GONE
        viewModel.removePhotoButtonVisibility.value = View.VISIBLE
        val imageView: ImageView = findViewById(R.id.report_other_image)
        val width =
            imageView.layoutParams.width / ErikuraApplication.instance.resources.displayMetrics.density
        val height =
            imageView.layoutParams.height / ErikuraApplication.instance.resources.displayMetrics.density
        item.loadImage(this, imageView, width.toInt(), height.toInt())
        viewModel.otherPhoto = item
    }

    override fun onBackPressed() {
        if (!fromConfirm) {
            fillReport()
            JobUtils.saveReportDraft(job, step = ReportDraft.ReportStep.WorkingTimeForm)

            val intent = Intent(this, ReportWorkingTimeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("job", job)
            startActivity(intent)
        } else {
            super.onBackPressed()
        }
    }

    override fun onClickNext(view: View) {
        job.report?.let {
            fillReport()
            editCompleted = true


            if (fromConfirm) {
                JobUtils.saveReportDraft(job, step = ReportDraft.ReportStep.Confirm)
                val intent = Intent()
                intent.putExtra("job", job)
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else {
                JobUtils.saveReportDraft(job, step = ReportDraft.ReportStep.EvaluationForm)
                val intent = Intent(this, ReportEvaluationActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("job", job)
                startActivity(intent)
            }
        }
    }

    private fun fillReport() {
        job.report?.let {
            it.additionalPhotoAsset = viewModel.otherPhoto
            it.additionalComment = viewModel.comment.value
            if (it.additionalPhotoAsset!!.contentUri != null) {
                it.additionalReportPhotoWillDelete = false
                if (it.additionalReportPhotoUrl == it.additionalPhotoAsset?.contentUri.toString()) {
                    // レポートが保持している URL と一致している => 画像が変更されていない
                } else {
                    it.additionalReportPhotoUrl = null
                    it.uploadPhoto(this, job, it.additionalPhotoAsset) { token ->
                        PhotoTokenManager.addToken(
                            job,
                            it.additionalPhotoAsset?.contentUri.toString(),
                            token
                        )
                    }
                }
            } else {
                it.additionalReportPhotoWillDelete = true
            }
        }
    }


    override fun onClickReportExamples(view: View) {
        job?.let { job ->
            JobUtil.openReportExample(this, job)
        }
    }

}

class ReportOtherFormViewModel : ViewModel() {
    val addPhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val removePhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val comment: MutableLiveData<String> = MutableLiveData()

    //    val commentErrorMsg: MutableLiveData<String> = MutableLiveData()
//    val commentErrorVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val commentError: ErrorMessageViewModel = ErrorMessageViewModel()

    var otherPhoto: MediaItem = MediaItem()

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(addPhotoButtonVisibility) { result.value = isValid() }
        result.addSource(comment) { result.value = isValid() }
    }
    val reportExamplesButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

    private fun isValid(): Boolean {
        var valid = true
        if (addPhotoButtonVisibility.value == View.VISIBLE && comment.value.isNullOrBlank()) {
            valid = true
        } else {
            valid = isValidPhoto() && valid
            valid = isValidComment() && valid
        }
        return valid
    }

    private fun isValidPhoto(): Boolean {
        return otherPhoto.contentUri != null
    }

    private fun isValidComment(): Boolean {
        var valid = true
        if (valid && comment.value.isNullOrBlank()) {
            valid = false
            commentError.message.value = null
        } else if (valid && comment.value?.length ?: 0 > ErikuraConst.maxCommentLength) {
            valid = false
            commentError.message.value = ErikuraApplication.instance.getString(
                R.string.comment_count_error,
                ErikuraConst.maxCommentLength
            )
        } else {
            valid = true
            commentError.message.value = null
        }
        return valid
    }
}

interface ReportOtherFormEventHandlers {
    fun onClickNext(view: View)
    fun onClickAddPhotoButton(view: View)
    fun onClickRemovePhoto(view: View)
    fun onClickManual(view: View)
    fun onClickReportExamples(view: View)
}