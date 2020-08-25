package jp.co.recruit.erikura.presenters.activities.mypage

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Base64
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
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityUploadIdImageBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.tutorial.PermitLocationActivity
import java.io.ByteArrayOutputStream
import java.lang.NullPointerException

class UploadIdImageActivity : BaseActivity(), UploadIdImageEventHandlers {
    // 身分証種別の要素番号
    private val driverLicenceElementNum = 1
    private val passportElementNum = 4
    private val myNumberElementNum = 5

    // 各画像のフィールド識別コード
    private val frontRequestCode = 1
    private val backRequestCode = 2
    private val passportFrontRequestCode = 3
    private val passportBackRequestCode = 4
    private val myNumberRequestCode = 5

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
        fromWhere =
            intent.getIntExtra(ErikuraApplication.FROM_WHERE, ErikuraApplication.FROM_NOT_FOUND)
        if (fromWhere == ErikuraApplication.FROM_ENTRY) {
            job = intent.getParcelableExtra("job")
        }

        val binding: ActivityUploadIdImageBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_upload_id_image)
        binding.lifecycleOwner = this
        binding.handlers = this
        binding.viewModel = viewModel

        // 身分証種別の要素番号
        viewModel.driverLicenceElementNum.value = driverLicenceElementNum
        viewModel.passportElementNum.value = passportElementNum
        viewModel.myNumberElementNum.value = myNumberElementNum
    }

    // 画像選択イベント
    override fun onClickAddFrontPhotoButton(view: View) {
        separateTransitionsForEachButtonType(frontRequestCode)
    }

    override fun onClickAddBackPhotoButton(view: View) {
        separateTransitionsForEachButtonType(backRequestCode)
    }

    override fun onCLickAddPassportFrontPhotoButton(view: View) {
        separateTransitionsForEachButtonType(passportFrontRequestCode)
    }

    override fun onCLickAddPassportBackPhotoButton(view: View) {
        separateTransitionsForEachButtonType(passportBackRequestCode)
    }

    override fun onCLickAddMyNumberPhotoButton(view: View) {
        separateTransitionsForEachButtonType(myNumberRequestCode)
    }

    private fun separateTransitionsForEachButtonType(requestCode: Int) {
        if (ErikuraApplication.instance.hasStoragePermission(this)) {
            moveToGallery(requestCode)
        } else {
            ErikuraApplication.instance.requestStoragePermission(this)
        }
    }

    private fun moveToGallery(requestCode: Int) {
        val intent = Intent()
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(intent, requestCode)
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
                    // val item = MediaItem.from(cursor)
                    // MEMO: cursorを渡すとIDの値が0になるので手動で値を入れています
                    val item = MediaItem.from(cursor)

                    // requestCodeによって画像フィールドを切り分ける
                    when (requestCode) {
                        frontRequestCode -> {
                            viewModel.addFrontPhotoButtonVisibility.value = View.GONE
                            viewModel.removeFrontPhotoButtonVisibility.value = View.VISIBLE
                            val imageView: ImageView = findViewById(R.id.image_front)
                            item.loadImage(this, imageView)
                            viewModel.otherPhotoFront = item
                        }
                        backRequestCode -> {
                            viewModel.addBackPhotoButtonVisibility.value = View.GONE
                            viewModel.removeBackPhotoButtonVisibility.value = View.VISIBLE
                            val imageView: ImageView = findViewById(R.id.image_back)
                            item.loadImage(this, imageView)
                            viewModel.otherPhotoBack = item
                        }
                        passportFrontRequestCode -> {
                            viewModel.addPassportFrontPhotoButtonVisibility.value = View.GONE
                            viewModel.removePassportFrontPhotoButtonVisibility.value = View.VISIBLE
                            val imageView: ImageView = findViewById(R.id.passport_front_image)
                            item.loadImage(this, imageView)
                            viewModel.otherPhotoPassportFront = item
                        }
                        passportBackRequestCode -> {
                            viewModel.addPassportBackPhotoButtonVisibility.value = View.GONE
                            viewModel.removePassportBackPhotoButtonVisibility.value = View.VISIBLE
                            val imageView: ImageView = findViewById(R.id.passport_back_image)
                            item.loadImage(this, imageView)
                            viewModel.otherPhotoPassportBack = item
                        }
                        myNumberRequestCode -> {
                            viewModel.addMyNumberPhotoButtonVisibility.value = View.GONE
                            viewModel.removeMyNumberPhotoButtonVisibility.value = View.VISIBLE
                            val imageView: ImageView = findViewById(R.id.my_number_image)
                            item.loadImage(this, imageView)
                            viewModel.otherPhotoMyNumber = item
                        }
                    }
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
        when (fromWhere) {
            ErikuraApplication.FROM_REGISTER -> {
                // 地図画面へ
                if (ErikuraApplication.instance.isOnboardingDisplayed()) {
                    // 地図画面へ遷移
                    val intent = Intent(this, MapViewActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // 位置情報の許諾、オンボーディングを表示します
                    Intent(this, PermitLocationActivity::class.java).let { intent ->
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
            ErikuraApplication.FROM_CHANGE_USER, ErikuraApplication.FROM_CHANGE_USER_FOR_CHANGE_INFO -> {
                // 元の画面へ
                finish()
            }
            ErikuraApplication.FROM_ENTRY -> {
                // 仕事詳細へ遷移し応募確認ダイアログへ
                val intent = Intent(this, JobDetailsActivity::class.java)
                intent.putExtra("fromIdentify", true)
                intent.putExtra("job", job)
                startActivity(intent)
                finish()
            }
        }

    }

    // 画像削除イベント
    override fun onClickRemoveFrontPhoto(view: View) {
        viewModel.otherPhotoFront = MediaItem()
        val imageView: ImageView = findViewById(R.id.image_front)
        imageView.setImageDrawable(null)
        viewModel.addFrontPhotoButtonVisibility.value = View.VISIBLE
        viewModel.removeFrontPhotoButtonVisibility.value = View.GONE
    }

    override fun onClickRemoveBackPhoto(view: View) {
        viewModel.otherPhotoBack = MediaItem()
        val imageView: ImageView = findViewById(R.id.image_back)
        imageView.setImageDrawable(null)
        viewModel.addBackPhotoButtonVisibility.value = View.VISIBLE
        viewModel.removeBackPhotoButtonVisibility.value = View.GONE
    }

    override fun onCLickRemovePassportFrontPhoto(view: View) {
        viewModel.otherPhotoPassportFront = MediaItem()
        val imageView: ImageView = findViewById(R.id.passport_front_image)
        imageView.setImageDrawable(null)
        viewModel.addPassportFrontPhotoButtonVisibility.value = View.VISIBLE
        viewModel.removePassportFrontPhotoButtonVisibility.value = View.GONE
    }

    override fun onClickRemovePassportBackPhoto(view: View) {
        viewModel.otherPhotoPassportBack = MediaItem()
        val imageView: ImageView = findViewById(R.id.passport_back_image)
        imageView.setImageDrawable(null)
        viewModel.addPassportBackPhotoButtonVisibility.value = View.VISIBLE
        viewModel.removePassportBackPhotoButtonVisibility.value = View.GONE
    }

    override fun onClickRemoveMyNumberPhoto(view: View) {
        viewModel.otherPhotoMyNumber = MediaItem()
        val imageView: ImageView = findViewById(R.id.my_number_image)
        imageView.setImageDrawable(null)
        viewModel.addMyNumberPhotoButtonVisibility.value = View.VISIBLE
        viewModel.removeMyNumberPhotoButtonVisibility.value = View.GONE

    }

    override fun onClickUploadIdImage(view: View) {
        // 身分証種別によってエンコードしてデータをセット
        // 各contentUriはバリデーションチェック済
        when (viewModel.typeOfId.value) {
            passportElementNum -> {
                idDocument.data?.front?.plus(encodeBase64FromImage(viewModel.otherPhotoPassportFront.contentUri!!))
                idDocument.data?.back?.plus(encodeBase64FromImage(viewModel.otherPhotoPassportBack.contentUri!!))
            }
            myNumberElementNum -> {
                idDocument.data?.front?.plus(encodeBase64FromImage(viewModel.otherPhotoMyNumber.contentUri!!))
            }
            else -> {
                idDocument.data?.front?.plus(encodeBase64FromImage(viewModel.otherPhotoFront.contentUri!!))
                if (viewModel.addBackPhotoButtonVisibility.value == View.GONE) {
                    // 裏面もある場合
                    idDocument.data?.back?.plus(encodeBase64FromImage(viewModel.otherPhotoBack.contentUri!!))
                }
            }
        }
        idDocument.type = identityTypeOfList.getString(viewModel.typeOfId.value ?: 0)
        idDocument.comparingData = comparingData

        userId?.let { userId ->
            Api(this).idVerify(userId, idDocument) {
                // 遷移元に応じて身分証確認完了を表示
                moveUploadedIdImage()
            }
        }
    }

    private fun encodeBase64FromImage(uri: Uri): String {
        // uriから読み込み用InputStreamを生成
        val inputStream = contentResolver?.openInputStream(uri)
        // inputStreamからbitmap生成
        val imageBitmap = BitmapFactory.decodeStream(inputStream)
        // bitmapからバイト配列を生成
        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val imageByteArray = stream.toByteArray()
        // バイト配列からBase64文字列を生成
        return Base64.encodeToString(imageByteArray, Base64.DEFAULT)
    }

    private fun moveUploadedIdImage() {
        when (fromWhere) {
            ErikuraApplication.FROM_REGISTER -> {
                //　身分証確認完了画面へ
                val intent = Intent(this, UploadedIdImageActivity::class.java)
                intent.putExtra(ErikuraApplication.FROM_WHERE, fromWhere)
                startActivity(intent)
                finish()
            }
            ErikuraApplication.FROM_CHANGE_USER, ErikuraApplication.FROM_CHANGE_USER_FOR_CHANGE_INFO -> {
                //　会員情報変更画面へ遷移し、身分証確認完了モーダルを表示
                // FIXME 会員情報画面が２重になるかもしれない　戻るボタンを押下して要動作検証
                val intent = Intent(this, ChangeUserInformationActivity::class.java)
                intent.putExtra(ErikuraApplication.FROM_WHERE, fromWhere)
                startActivity(intent)
                finish()
            }
            ErikuraApplication.FROM_ENTRY -> {
                //　身分証確認完了画面へ
                val intent = Intent(this, UploadedIdImageActivity::class.java)
                intent.putExtra(ErikuraApplication.FROM_WHERE, fromWhere)
                intent.putExtra("job", job)
                startActivity(intent)
                finish()
            }
        }
    }
}

class UploadIdImageViewModel : ViewModel() {
    val driverLicenceElementNum = MutableLiveData<Int>()
    val passportElementNum = MutableLiveData<Int>()
    val myNumberElementNum = MutableLiveData<Int>()

    // 身分証の種別
    var type: MutableLiveData<String> = MutableLiveData<String>()
    var typeOfId: MutableLiveData<Int> = MutableLiveData<Int>()

    // 表面
    var otherPhotoFront: MediaItem = MediaItem()
    val addFrontPhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val removeFrontPhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    // 裏面
    var otherPhotoBack: MediaItem = MediaItem()
    val addBackPhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val removeBackPhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    // パスポート表面
    var otherPhotoPassportFront: MediaItem = MediaItem()
    val addPassportFrontPhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val removePassportFrontPhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    // パスポート裏面
    var otherPhotoPassportBack: MediaItem = MediaItem()
    val addPassportBackPhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val removePassportBackPhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    // マイナンバー
    var otherPhotoMyNumber: MediaItem = MediaItem()
    val addMyNumberPhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val removeMyNumberPhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    // 各画像フィールド表示のvisibility
    var normalImageSelectionVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(typeOfId) {
            // パスポート、マイナンバー以外の場合表示
            if (isNotPassportOrMyNumber()) {
                result.value = View.VISIBLE
            } else {
                result.value = View.GONE
            }
        }
    }
    var passportImageSelectionVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(typeOfId) { id ->
            // パスポートの場合表示
            if (id == passportElementNum.value) {
                result.value = View.VISIBLE
            } else {
                result.value = View.GONE
            }
        }
    }
    var myNumberImageSelectionVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(typeOfId) { id ->
            // パスポートの場合表示
            if (id == myNumberElementNum.value) {
                result.value = View.VISIBLE
            } else {
                result.value = View.GONE
            }
        }
    }

    //　送信ボタンの活性、非活性
    val isUploadIdImageButtonEnabled = MediatorLiveData<Boolean>().also { result ->
        result.addSource(typeOfId) { result.value = isValid() }
        result.addSource(addFrontPhotoButtonVisibility) { result.value = isValid() }
        result.addSource(addBackPhotoButtonVisibility) { result.value = isValid() }
        result.addSource(addPassportFrontPhotoButtonVisibility) { result.value = isValid() }
        result.addSource(addPassportBackPhotoButtonVisibility) { result.value = isValid() }
        result.addSource(addMyNumberPhotoButtonVisibility) { result.value = isValid() }
    }

    private fun isValid(): Boolean {
        var valid = false
        valid = isValidTypeOfId()
        if (valid) {
            valid = isValidPhoto()
        }
        return valid
    }

    // 身分証種別によって画像の数をバリデーション、画像サイズ
    private fun isValidTypeOfId(): Boolean {
        return !(typeOfId.value == 0 || typeOfId.value == null)
    }

    private fun isValidPhoto(): Boolean {
        // 身分証種別ごと画像選択済みでかつ画像URLを取得できているか
        when (typeOfId) {
            passportElementNum -> {
                return ((addPassportFrontPhotoButtonVisibility.value == View.GONE) && (addPassportBackPhotoButtonVisibility.value == View.GONE)
                        && (otherPhotoPassportFront.contentUri != null) && (otherPhotoPassportBack.contentUri != null))
            }
            myNumberElementNum -> {
                return ((addMyNumberPhotoButtonVisibility.value == View.GONE) && (otherPhotoMyNumber.contentUri != null))
            }
            else -> {
                if (addBackPhotoButtonVisibility.value == View.GONE) {
                    //　裏面も選択されている場合
                    return ((otherPhotoBack.contentUri != null) && ((addFrontPhotoButtonVisibility.value == View.GONE) && (otherPhotoFront.contentUri != null)))
                } else {
                    return ((addFrontPhotoButtonVisibility.value == View.GONE) && (otherPhotoFront.contentUri != null))
                }
            }
        }
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

interface UploadIdImageEventHandlers {
    fun onClickClose(view: View)
    fun onClickSkip(view: View)

    //　画像選択イベント
    fun onClickAddFrontPhotoButton(view: View)
    fun onClickAddBackPhotoButton(view: View)
    fun onCLickAddPassportFrontPhotoButton(view: View)
    fun onCLickAddPassportBackPhotoButton(view: View)
    fun onCLickAddMyNumberPhotoButton(view: View)

    // 画像削除イベント
    fun onClickRemoveFrontPhoto(view: View)
    fun onClickRemoveBackPhoto(view: View)
    fun onCLickRemovePassportFrontPhoto(view: View)
    fun onClickRemovePassportBackPhoto(view: View)
    fun onClickRemoveMyNumberPhoto(view: View)

    // 送信イベント
    fun onClickUploadIdImage(view: View)
}