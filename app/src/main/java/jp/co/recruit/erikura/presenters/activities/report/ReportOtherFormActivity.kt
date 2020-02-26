package jp.co.recruit.erikura.presenters.activities.report

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.MediaItem
import jp.co.recruit.erikura.databinding.ActivityReportOtherFormBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
import android.provider.MediaStore


class ReportOtherFormActivity : AppCompatActivity(), ReportOtherFormEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportOtherFormViewModel::class.java)
    }

    var job = Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_other_form)

        job = intent.getParcelableExtra<Job>("job")

        val binding: ActivityReportOtherFormBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_other_form)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
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
        val imageView: ImageView = findViewById(R.id.report_other_image)
        imageView.setImageDrawable(null)
        viewModel.addPhotoButtonVisibility.value = View.VISIBLE
        viewModel.removePhotoButtonVisibility.value = View.GONE
        viewModel.otherPhoto = MediaItem()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode) {
            ErikuraApplication.instance.REQUEST_PERMISSION -> {
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
        val intent = Intent()
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(intent, 1000 )
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

    }

    override fun onClickNext(view: View) {
        job.report?.let {
            if (viewModel.otherPhoto.contentUri != null) {
                it.additionalPhotoAsset = viewModel.otherPhoto
                it.additionalComment = viewModel.comment.value
            }

            val intent= Intent(this, ReportEvaluationActivity::class.java)
            intent.putExtra("job", job)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

    }
}

class ReportOtherFormViewModel: ViewModel() {
    val addPhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val removePhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val comment: MutableLiveData<String> = MutableLiveData()
    val commentErrorMsg: MutableLiveData<String> = MutableLiveData()
    val commentErrorVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

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
            commentErrorMsg.value = ""
            commentErrorVisibility.value = View.GONE
        }else if (valid && comment.value?.length?: 0 > 5000) {
            valid = false
            commentErrorMsg.value = ErikuraApplication.instance.getString(R.string.comment_count_error)
            commentErrorVisibility.value = View.VISIBLE
        }else {
            valid = true
            commentErrorMsg.value = ""
            commentErrorVisibility.value = View.GONE
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