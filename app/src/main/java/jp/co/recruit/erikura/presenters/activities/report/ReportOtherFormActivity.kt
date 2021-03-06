package jp.co.recruit.erikura.presenters.activities.report

import JobUtil
import android.annotation.SuppressLint
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
import jp.co.recruit.erikura.business.util.ExifUtils
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.data.storage.PhotoTokenManager
import jp.co.recruit.erikura.data.storage.ReportDraft
import jp.co.recruit.erikura.databinding.ActivityReportOtherFormBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel
import jp.co.recruit.erikura.presenters.util.MessageUtils
import java.text.SimpleDateFormat
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
            // ?????????????????????????????????
            Intent(this, MapViewActivity::class.java).let {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                it.putStringArrayListExtra(
                    ErikuraApplication.ERROR_MESSAGE_KEY,
                    arrayListOf("???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????")
                )
                this.startActivity(it)
            }
            Log.v(ErikuraApplication.LOG_TAG, "Cannot retrieve job")
            // FirebaseCrashlytics ????????????null????????????????????????????????????
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
            // ?????????????????????????????????????????????
            Tracking.logEvent(event = "view_job_report_others", params = bundleOf())
            Tracking.viewJobDetails(
                name = "/reports/register/additional/${job.id}",
                title = "??????????????????????????????????????????",
                jobId = job.id
            )
        } else {
            // ?????????????????????????????????????????????
            Tracking.logEvent(event = "view_edit_job_report_others", params = bundleOf())
            Tracking.viewJobDetails(
                name = "/reports/edit/additional/${job.id}",
                title = "????????????????????????????????????????????????",
                jobId = job.id
            )
        }
        //????????????????????????0?????????????????????
        job.goodExamplesCount?.let { reportExampleCount ->
            if (reportExampleCount == 0) {
                viewModel.reportExamplesButtonVisibility.value = View.GONE
            }
        }
        viewModel.job.value = job
//        FirebaseCrashlytics.getInstance().recordException(MemoryTraceException(this.javaClass.name, getAvailableMemory()))
    }

    override fun onStop() {
        super.onStop()

        // GC????????????????????????
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
                viewModel.comment.value = comment
            }
        }
        viewModel.commentError.message.value = null
    }

    @SuppressLint("SimpleDateFormat")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            uri?.let {
                MediaItem.createFrom(this, uri)?.let { item ->
                    val exifInterface = ExifUtils.exifInterface(this, uri)
                    val (imageWidth, imageHeight) = ExifUtils.size(this, uri, exifInterface)
                    val takenAt = ExifUtils.takenAt(exifInterface)

                    val isOld = takenAt?.let { takenAt ->
                        job.entryAt()?.let { entryAt ->
                            takenAt < entryAt
                        } ?: false
                    } ?: false

                    when {
                        // ????????????????????????????????????????????????????????????
                        (imageHeight > imageWidth) -> {
                            MessageUtils.displayAlert(this, listOf("???????????????????????????????????????"))
                        }
                        // ??????????????????????????????????????????????????????????????????????????????
                        isOld -> {
                            JobUtil.displayOldPictureWarning(this, takenAt) {
                                addOtherPicture(item)
                            }
                        }
                        else -> {
                            // ??????????????????????????????????????????????????????????????????????????????
                            addOtherPicture(item)
                        }
                    }
                }
            }
        }
        fromGallery = true
    }

    private fun addOtherPicture(item: MediaItem) {
        viewModel.otherPhoto = item
        viewModel.addPhotoButtonVisibility.value = View.GONE
        viewModel.removePhotoButtonVisibility.value = View.VISIBLE
        val imageView: ImageView = findViewById(R.id.report_other_image)
        val width =
            imageView.layoutParams.width / ErikuraApplication.instance.resources.displayMetrics.density
        val height =
            imageView.layoutParams.height / ErikuraApplication.instance.resources.displayMetrics.density
        item.loadImage(this, imageView, width.toInt(), height.toInt())
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
                    // ????????????????????????????????? URL ????????????????????? => ?????????????????????????????????
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

    override fun onClickClose(view: View) {
        fillReport()

        JobUtil.displaySuspendReportConfirmation(this, ReportDraft.ReportStep.OtherForm, job, null)
    }

}

class ReportOtherFormViewModel : ViewModel() {
    val job: MutableLiveData<Job> = MutableLiveData()
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

    val closeButtonVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(job) {
            job.value?.report?.id?.also {
                result.value = View.GONE
            } ?: run{
                result.value = View.VISIBLE
            }
        }
    }

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
    fun onClickClose(view: View)
}