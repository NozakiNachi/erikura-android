package jp.co.recruit.erikura.presenters.activities.mypage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.ComparingData
import jp.co.recruit.erikura.business.models.IdDocument
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.MediaItem
import jp.co.recruit.erikura.presenters.activities.BaseActivity

class UploadIdImageActivity : BaseActivity(), UploadIdImageEventHandlers{
    var idDocument = IdDocument()
    var comparingData = ComparingData()
    var userId: Int? = null
    var fromGallery = false
    var fromWhere: Int? = null
    var job = Job()
    private val viewModel: UploadIdImageViewModel by lazy {
        ViewModelProvider(this).get(UploadIdImageViewModel::class.java)
    }

    // 身分証の種別リスト
    val typeOfIdList =
        ErikuraApplication.instance.resources.obtainTypedArray(R.array.type_of_id_list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        comparingData = intent.getParcelableExtra("comparingData")
        userId = intent.getIntExtra("userId", 0)
        fromWhere = intent.getIntExtra(ErikuraApplication.FROM_WHERE, ErikuraApplication.FROM_NOT_FOUND)
        if (fromWhere == ErikuraApplication.FROM_ENTRY) {
            job = intent.getParcelableExtra("job")
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
                val id = DocumentsContract.getDocumentId(uri)
                val cursor = contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(
                        MediaStore.Files.FileColumns._ID,
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.MIME_TYPE,
                        MediaStore.MediaColumns.SIZE,
                        MediaStore.Files.FileColumns.DATE_ADDED,
                        MediaStore.MediaColumns.DATE_TAKEN
                    ),
                    "_id=?", arrayOf(id.split(":")[1]), null
                )
                cursor?.moveToFirst()
                cursor?.let {
                    // FIXME 対象となるviewModelのvisibility、画像データの反映を切り分ける必要がある
                    // val item = MediaItem.from(cursor)
                    // MEMO: cursorを渡すとIDの値が0になるので手動で値を入れています
                    val item = MediaItem.from(cursor)
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

    // 戻るボタンの制御
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onClickClose(view: View) {
        finish()
    }

    override fun onClickUploadIdImage(view: View) {
        // FIXME 選択した内容を代入
        idDocument.type = typeOfIdList.getString(viewModel.typeOfId.value ?: 0)
    }
}

class UploadIdImageViewModel : ViewModel() {
    // 身分証の種別
    var type: MutableLiveData<String> = MutableLiveData<String>()
    var typeOfId: MutableLiveData<Int> = MutableLiveData<Int>()
    // 身分証種別によって画像の数をバリデーション、画像サイズ
    private fun isValidTypeOfId(): Boolean {
        return !(typeOfId.value == 0 || typeOfId.value == null)
    }

}

interface UploadIdImageEventHandlers{
    fun onClickClose(view: View)
    //　FIXME 画像追加ボタンを必須と非必須に分ける
    fun onClickAddPhotoButton(view: View)
    fun onClickUploadIdImage(view: View)
}