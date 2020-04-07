package jp.co.recruit.erikura.presenters.activities.report

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.realm.Realm
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.MediaItem
import jp.co.recruit.erikura.data.storage.PhotoToken
import jp.co.recruit.erikura.databinding.ActivityReportOtherFormBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
import jp.co.recruit.erikura.presenters.activities.mypage.ErrorMessageViewModel


class ReportOtherFormActivity : BaseActivity(), ReportOtherFormEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportOtherFormViewModel::class.java)
    }
    private val realm: Realm get() = ErikuraApplication.realm

    var job = Job()
    var fromConfirm = false
    var fromGallery = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_other_form)

        val binding: ActivityReportOtherFormBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_other_form)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
    }

    override fun onStart() {
        super.onStart()
        job = intent.getParcelableExtra<Job>("job")
        fromConfirm = intent.getBooleanExtra("fromConfirm", false)
        if(!fromGallery) {
            loadData()
        }
        fromGallery = false

        if (job.reportId == null) {
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_job_report_others", params= bundleOf())
            Tracking.viewJobDetails(name= "/reports/register/additional/${job.id}", title= "作業報告画面（マニュアル外）", jobId= job.id)
        }else {
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_edit_job_report_others", params= bundleOf())
            Tracking.viewJobDetails(name= "/reports/edit/additional/${job.id}", title= "作業報告編集画面（マニュアル外）", jobId= job.id)
        }
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

    override fun onClickAddPhotoButton(view: View) {
        if(ErikuraApplication.instance.hasStoragePermission(this)) {
            moveToGallery()
        }
        else {
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

        when(requestCode) {
            ErikuraApplication.REQUEST_EXTERNAL_STORAGE_PERMISSION_ID -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    moveToGallery()
                }else {
                    val dialog = StorageAccessConfirmDialogFragment()
                    dialog.show(supportFragmentManager, "confirm")
                }
            }
        }
    }

    private fun moveToGallery() {
        // MEMO: アップロード済みの画像と区別するためにURLを空にします
        job.report?.additionalReportPhotoUrl = null
        val intent = Intent()
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(intent, 1000 )
    }

    private fun loadData() {
        job.report?.let {
            val item = it.additionalPhotoAsset?: MediaItem()
            val comment = it.additionalComment
            if (item.contentUri != null) {
                viewModel.addPhotoButtonVisibility.value = View.GONE
                viewModel.removePhotoButtonVisibility.value = View.VISIBLE
                val imageView: ImageView = findViewById(R.id.report_other_image)
                if (it.additionalReportPhotoUrl != null ) {
                    item.loadImageFromString(this, imageView)
                }else {
                    item.loadImage(this, imageView)
                }
                viewModel.otherPhoto = item
                viewModel.comment.value = comment
            }else {
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
                val cursor = this.contentResolver.query(
                    uri,
                    arrayOf(
                        MediaStore.Files.FileColumns._ID,
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.MIME_TYPE,
                        MediaStore.MediaColumns.SIZE
                    ),
                    MediaStore.MediaColumns.SIZE + ">0",
                    arrayOf<String>(),
                    "datetaken DESC"
                )

                cursor?.moveToFirst()
                cursor?.let {
                    // val item = MediaItem.from(cursor)
                    // MEMO: cursorを渡すとIDの値が0になるので手動で値を入れています
                    val uriString = uri.toString()
                    val arr = uriString.split("%3A")
                    val id = arr.last().toLong()
                    val mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE))
                    val size = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE))
                    val item = MediaItem(id = id, mimeType = mimeType, size = size, contentUri = uri)
                    viewModel.addPhotoButtonVisibility.value = View.GONE
                    viewModel.removePhotoButtonVisibility.value = View.VISIBLE
                    val imageView: ImageView = findViewById(R.id.report_other_image)
                    item.loadImage(this, imageView)
                    viewModel.otherPhoto = item
                }

                cursor?.close()
            }
        }

        fromGallery = true

    }

    override fun onClickNext(view: View) {
        job.report?.let {
            it.additionalPhotoAsset = viewModel.otherPhoto
            it.additionalComment = viewModel.comment.value
            if (it.additionalPhotoAsset != null) {
                it.additionalReportPhotoWillDelete = false
                it.uploadPhoto(this, job, it.additionalPhotoAsset) { token ->
//                    it.additionalReportPhotoToken = token
                    addPhotoToken(it.additionalPhotoAsset?.contentUri.toString(), token)
                }
            }else {
                it.additionalReportPhotoWillDelete = true
            }

            if (fromConfirm) {
                val intent= Intent()
                intent.putExtra("job", job)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }else {
                val intent= Intent(this, ReportEvaluationActivity::class.java)
                intent.putExtra("job", job)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
        }

    }

    private fun addPhotoToken(url: String, token: String) {
        realm.executeTransaction { realm ->
            var photo = realm.createObject(PhotoToken::class.java, token)
            photo.url = url
            photo.jobId = job.id
        }
    }
}

class ReportOtherFormViewModel: ViewModel() {
    val addPhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val removePhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val comment: MutableLiveData<String> = MutableLiveData()
//    val commentErrorMsg: MutableLiveData<String> = MutableLiveData()
//    val commentErrorVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val commentError: ErrorMessageViewModel = ErrorMessageViewModel()

    var otherPhoto: MediaItem = MediaItem()

    val isNextButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(addPhotoButtonVisibility) {result.value = isValid()}
        result.addSource(comment) { result.value = isValid()  }
    }

    private fun isValid(): Boolean {
        var valid = true
        if (addPhotoButtonVisibility.value == View.VISIBLE && comment.value.isNullOrBlank()) {
            valid = true
        }else {
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
//            commentErrorMsg.value = ""
//            commentErrorVisibility.value = View.GONE
            commentError.message.value = null
        }else if (valid && comment.value?.length?: 0 > 5000) {
            valid = false
//            commentErrorMsg.value = ErikuraApplication.instance.getString(R.string.comment_count_error)
//            commentErrorVisibility.value = View.VISIBLE
            commentError.message.value = ErikuraApplication.instance.getString(R.string.comment_count_error)
        }else {
            valid = true
//            commentErrorMsg.value = ""
//            commentErrorVisibility.value = View.GONE
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
}