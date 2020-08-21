package jp.co.recruit.erikura.presenters.activities.mypage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.ComparingData
import jp.co.recruit.erikura.business.models.IdDocument
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.MediaItem
import jp.co.recruit.erikura.databinding.ActivityUploadIdImageBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.tutorial.PermitLocationActivity

class UploadIdImageActivity : BaseActivity(), UploadIdImageEventHandlers{
    private val driverLicenceElementNum = 1
    private val passportElementNum = 4
    private val myNumberElementNum = 5

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
    val identityTypeOfList =
        ErikuraApplication.instance.resources.obtainTypedArray(R.array.identity_type_of_list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        comparingData = intent.getParcelableExtra("comparingData")
        userId = intent.getIntExtra("userId", 0)
        fromWhere = intent.getIntExtra(ErikuraApplication.FROM_WHERE, ErikuraApplication.FROM_NOT_FOUND)
        if (fromWhere == ErikuraApplication.FROM_ENTRY) {
            job = intent.getParcelableExtra("job")
        }

        val binding: ActivityUploadIdImageBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_upload_id_image)
        binding.lifecycleOwner = this
        binding.handlers = this
        binding.viewModel = viewModel

        // 種別の要素番号
        viewModel.driverLicenceElementNum.value = driverLicenceElementNum
        viewModel.passportElementNum.value = passportElementNum
        viewModel.myNumberElementNum.value = myNumberElementNum
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

        // FIXME 下記をメソッド化
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
                // 上記までメソッド化
                // FIXME 下記は各ボタンのイベントごとに分けて画像のラベルを切り分ける
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

    override fun onClickSkip(view: View) {
        //遷移元によって遷移先を切り分ける
        when(fromWhere) {
            ErikuraApplication.FROM_REGISTER -> {
                // 地図画面へ
                if (ErikuraApplication.instance.isOnboardingDisplayed()) {
                    // 地図画面へ遷移
                    val intent = Intent(this, MapViewActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
                else {
                    // 位置情報の許諾、オンボーディングを表示します
                    Intent(this, PermitLocationActivity::class.java).let { intent ->
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
            ErikuraApplication.FROM_CHANGE_USER -> {
                // 元の画面へ
                finish()
            }
            ErikuraApplication.FROM_CHANGE_USER_FOR_CHANGE_INFO -> {
                // 元の画面へ
                finish()
            }
            ErikuraApplication.FROM_ENTRY -> {
                // 仕事詳細へ遷移し応募確認ダイアログへ
                val intent= Intent(this, JobDetailsActivity::class.java)
                intent.putExtra("fromIdentify", true)
                intent.putExtra("job", job)
                startActivity(intent)
                finish()
            }
        }

    }

    override fun onClickUploadIdImage(view: View) {
        // FIXME 選択した内容を代入
        // 画像はリサイズしてBase64にエンコードする
        idDocument.type = identityTypeOfList.getString(viewModel.typeOfId.value ?: 0)
    }
}

class UploadIdImageViewModel : ViewModel() {
    val driverLicenceElementNum = MutableLiveData<Int>()
    val passportElementNum = MutableLiveData<Int>()
    val myNumberElementNum = MutableLiveData<Int>()
    // 身分証の種別
    var type: MutableLiveData<String> = MutableLiveData<String>()
    var typeOfId: MutableLiveData<Int> = MutableLiveData<Int>()
    var normalImageSelectionField = MediatorLiveData<Int>().also { result ->
        result.addSource(typeOfId) {
            // パスポート、マイナンバー以外の場合表示
            if (isNotPassportOrMyNumber()) {
                result.value = View.VISIBLE
            } else {
                result.value = View.GONE
            }
        }
    }
    // 身分証種別によって画像の数をバリデーション、画像サイズ
    private fun isValidTypeOfId(): Boolean {
        return !(typeOfId.value == 0 || typeOfId.value == null)
    }

    // パスポート、マイナンバー以外の場合
    private fun isNotPassportOrMyNumber(): Boolean {
        var isNotPassportOrMyNumber = false
        if (!((typeOfId == passportElementNum) || (typeOfId == myNumberElementNum))) {
            isNotPassportOrMyNumber = true
        }
        return isNotPassportOrMyNumber
    }
}

interface UploadIdImageEventHandlers{
    fun onClickClose(view: View)
    fun onClickSkip(view: View)
    //　FIXME 画像追加ボタンを必須と非必須に分ける
    fun onClickAddPhotoButton(view: View)
    fun onClickUploadIdImage(view: View)
}