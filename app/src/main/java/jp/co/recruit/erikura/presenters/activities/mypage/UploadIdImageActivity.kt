package jp.co.recruit.erikura.presenters.activities.mypage

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.*
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityUploadIdImageBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.report.StorageAccessConfirmDialogFragment
import jp.co.recruit.erikura.presenters.activities.tutorial.PermitLocationActivity
import java.io.ByteArrayOutputStream
import java.net.SocketTimeoutException

class UploadIdImageActivity : BaseActivity(), UploadIdImageEventHandlers {
    // 身分証種別の要素番号
    private val driverLicenceElementNum = 0
    private val passportElementNum = 3
    private val myNumberElementNum = 4

    // 各画像のフィールド識別リクエストコードとボタンのリクエストコード
    private val jobApplyButtonRequest = ErikuraApplication.JOB_APPLY_BUTTON_REQUEST
    private val frontRequestCode = 1
    private val backRequestCode = 2
    private val passportFrontRequestCode = 3
    private val passportBackRequestCode = 4
    private val myNumberRequestCode = 5

    var idDocument = IdDocument()
    var identifyComparingData = IdentifyComparingData()
    var user = User()
    var fromGallery = false
    var fromWhere: Int = ErikuraApplication.FROM_NOT_FOUND
    var job = Job()
    private val viewModel: UploadIdImageViewModel by lazy {
        ViewModelProvider(this).get(UploadIdImageViewModel::class.java)
    }

    // 身分証の種別リスト(日本語)
    val identityTypeOfList =
        ErikuraApplication.instance.resources.obtainTypedArray(R.array.identity_type_of_list)
    // 身分証の種別リスト（英語）
    val identityTypeOfIdList =
        ErikuraApplication.instance.resources.obtainTypedArray(R.array.identity_type_of_id_list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        identifyComparingData = intent.getParcelableExtra("identifyComparingData")
        user = intent.getParcelableExtra("user")
        fromWhere =
            intent.getIntExtra(ErikuraApplication.FROM_WHERE, ErikuraApplication.FROM_NOT_FOUND)
        viewModel.fromWhere.value = fromWhere
        viewModel.identificationRequired.value = ErikuraConfig.identificationRequired
        if (fromWhere == ErikuraApplication.FROM_ENTRY) {
            job = intent.getParcelableExtra("job")
        }

        val binding: ActivityUploadIdImageBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_upload_id_image)
        binding.lifecycleOwner = this
        viewModel.setupHandler(this)
        binding.handlers = this
        binding.viewModel = viewModel

        // 身分証種別の要素番号
        viewModel.driverLicenceElementNum.value = driverLicenceElementNum
        viewModel.passportElementNum.value = passportElementNum
        viewModel.myNumberElementNum.value = myNumberElementNum

        // 応募の際身分証確認が必須かのフラグ
        viewModel.identificationRequired.value = ErikuraConfig.identificationRequired

    }

    override fun onStart() {
        super.onStart()

        // 身分証確認必須フラグを取得
        ErikuraConfig.loaded = false
        ErikuraConfig.load(this, onError = { messages ->
            viewModel.identificationRequired.value = ErikuraConfig.identificationRequired
        })
        // viewModelの値変更でvisibilityが変わらないので実値入力で切り替える
        if ( (fromWhere == ErikuraApplication.FROM_ENTRY) && (ErikuraConfig.identificationRequired == 1) ){
            viewModel.skipButtonVisibility.value = View.GONE
        } else {
            viewModel.skipButtonVisibility.value = View.VISIBLE
        }

        this.findViewById<TextView>(R.id.agreementLink)?.movementMethod = LinkMovementMethod.getInstance()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_user_verifications_id_document", params= bundleOf())
        Tracking.view("/user/verifications/id_document",  "身分証確認画面")
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

    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.Android.externalstorage.documents" == uri.getAuthority()
    }

    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.Android.providers.downloads.documents" == uri.getAuthority()
    }

    fun isMediaDocument(uri: Uri): Boolean {
        return "com.Android.providers.media.documents" == uri.getAuthority()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            uri?.let {
                MediaItem.createFrom(this, uri)?.let { item ->
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
            }
            fromGallery = true
        }

        if (requestCode == jobApplyButtonRequest && resultCode == AppCompatActivity.RESULT_OK) {
            val displayApplyDialog: Boolean? = data?.getBooleanExtra("displayApplyDialog", false)
            if (displayApplyDialog == true) {
                // 身分確認完了、あとで行う　の場合
                val intent = Intent()
                intent.putExtra("displayApplyDialog", true)
                setResult(RESULT_OK, intent)
                finish()
            } else {
                // 戻るボタンの場合
                val intent = Intent()
                setResult(RESULT_OK, intent)
                finish()
            }
        }

    }

    // 戻るボタンの制御
    override fun onBackPressed() {
        when (fromWhere) {
            ErikuraApplication.FROM_CHANGE_USER, ErikuraApplication.FROM_CHANGE_USER_FOR_CHANGE_INFO -> {
                // 元の画面へ iOSでは乗っかってる画面を消して
                // 会員情報変更画面に戻る場合画面を更新するのでAndroidも更新するために画面を再生成
                val intent = Intent(this, ChangeUserInformationActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            ErikuraApplication.FROM_ENTRY -> {
                //　応募経由の場合のみonActivityResultで画面を遷移
                val intent = Intent()
                setResult(RESULT_OK, intent)
                finish()
            }
            else -> {
                finish()
            }
        }
    }

    override fun onClickSkip(view: View) {
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "skip_user_verifications_id_document", params= bundleOf())
        Tracking.trackUserId( "skip_user_verifications_id_document",  user)
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
                // 元の画面へ iOSでは乗っかってる画面を消して
                // 会員情報変更画面に戻る場合画面を更新するのでAndroidも更新するために画面を再生成
                val intent = Intent(this, ChangeUserInformationActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            ErikuraApplication.FROM_ENTRY -> {
                // 仕事詳細へ遷移し応募確認ダイアログへ
                val intent = Intent()
                intent.putExtra("displayApplyDialog", true)
                setResult(RESULT_OK, intent)
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

    override fun onClickTermsOfService(view: View) {
        try {
            val termsOfServiceURLString =
                BuildConfig.SERVER_BASE_URL + BuildConfig.TERMS_OF_SERVICE_PATH
            val intent = Intent(this, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(termsOfServiceURLString)
            }
            startActivity(intent)
        }
        catch (e: ActivityNotFoundException) {
            Api(this).displayErrorAlert(listOf("PDFビューワーが見つかりません。\nPDFビューワーアプリをインストールしてください。"))
        }
    }

    override fun onClickPrivacyPolicy(view: View) {
        try {
            val privacyPolicyURLString =
                BuildConfig.SERVER_BASE_URL + BuildConfig.PRIVACY_POLICY_PATH
            val intent = Intent(this, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(privacyPolicyURLString)
            }
            startActivity(intent)
        }
        catch (e: ActivityNotFoundException) {
            Api(this).displayErrorAlert(listOf("PDFビューワーが見つかりません。\nPDFビューワーアプリをインストールしてください。"))
        }
    }

    override fun onClickUploadIdImage(view: View) {
        val api = Api(this)
        // リサイズ中に表示するスピナー
        api.showProgressAlert()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "send_id_document", params= bundleOf())
        Tracking.trackUserId( "send_id_document",  user)
        // 身分証種別によってエンコードしてデータをセット
        // 各contentUriはバリデーションチェック済
        when (viewModel.typeOfId.value) {
            passportElementNum -> {
                //パスポートは２枚とも表面扱い
                idDocument.identifyImageData = IdentifyImageData(front = listOf(encodeBase64FromImage(resizeImage(viewModel.otherPhotoPassportFront))),
                    back = listOf(encodeBase64FromImage(resizeImage(viewModel.otherPhotoPassportBack))))
            }
            myNumberElementNum -> {
                idDocument.identifyImageData = IdentifyImageData(front = listOf(encodeBase64FromImage(resizeImage(viewModel.otherPhotoMyNumber))))
            }
            else -> {
                idDocument.identifyImageData = IdentifyImageData(front = listOf(encodeBase64FromImage(resizeImage(viewModel.otherPhotoFront))),
                    back = listOf(encodeBase64FromImage(resizeImage(viewModel.otherPhotoBack))))
            }
        }
        idDocument.type = identityTypeOfIdList.getString(viewModel.typeOfId.value ?: 0)
        idDocument.identifyComparingData = identifyComparingData
        user.id?.let { userId ->
            try {
                val errorHandler: (List<String>?) -> Unit = { messages ->
                    api.displayErrorAlert(messages)
                    //　リサイズ中表示していたスピナーを削除
                    api.hideProgressAlert()
                }
                api.idVerify(userId, idDocument, onError = errorHandler) { result ->
                    //　リサイズ中表示していたスピナーを削除
                    api.hideProgressAlert()
                    if (result) {
                        // 遷移元に応じて身分証確認完了を表示
                        moveUploadedIdImage()
                    } else {
                        // 身分確認API失敗の場合 ダイアログを表示
                        val dialog = AlertDialog.Builder(this)
                            .setView(R.layout.dialog_failed_upload_id_image)
                            .setCancelable(true)
                            .setOnDismissListener {
                                it.dismiss()
                                finish()
                            }
                        dialog.show()
                    }
                }
            } catch(e: SocketTimeoutException) {
                api.hideProgressAlert()
                Log.e("ERROR", e.message, e)
            }
        }
    }

    override fun onClickSpinner(view: View?) {
        // 身分証の種別を選択する度に画像をリセットします

        viewModel.otherPhotoFront = MediaItem()
        val frontImageView: ImageView = findViewById(R.id.image_front)
        frontImageView.setImageDrawable(null)
        viewModel.addFrontPhotoButtonVisibility.value = View.VISIBLE
        viewModel.removeFrontPhotoButtonVisibility.value = View.GONE


        viewModel.otherPhotoBack = MediaItem()
        val backImageView: ImageView = findViewById(R.id.image_back)
        backImageView.setImageDrawable(null)
        viewModel.addBackPhotoButtonVisibility.value = View.VISIBLE
        viewModel.removeBackPhotoButtonVisibility.value = View.GONE

        viewModel.otherPhotoPassportFront = MediaItem()
        val passportFrontImageView: ImageView = findViewById(R.id.passport_front_image)
        passportFrontImageView.setImageDrawable(null)
        viewModel.addPassportFrontPhotoButtonVisibility.value = View.VISIBLE
        viewModel.removePassportFrontPhotoButtonVisibility.value = View.GONE

        viewModel.otherPhotoPassportBack = MediaItem()
        val passportBackImageView: ImageView = findViewById(R.id.passport_back_image)
        passportBackImageView.setImageDrawable(null)
        viewModel.addPassportBackPhotoButtonVisibility.value = View.VISIBLE
        viewModel.removePassportBackPhotoButtonVisibility.value = View.GONE

        viewModel.otherPhotoMyNumber = MediaItem()
        val myNumberImageView: ImageView = findViewById(R.id.my_number_image)
        myNumberImageView.setImageDrawable(null)
        viewModel.addMyNumberPhotoButtonVisibility.value = View.VISIBLE
        viewModel.removeMyNumberPhotoButtonVisibility.value = View.GONE
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

                }else {
                    val dialog = StorageAccessConfirmDialogFragment.newInstance(true)
                    dialog.show(supportFragmentManager, "confirm")
                }
            }
        }
    }


    private fun resizeImage(item: MediaItem): ByteArray {
        var imageByteArray: ByteArray? = null
        // uriから読み込み用InputStreamを生成
        val inputStream = contentResolver?.openInputStream(item.contentUri!!)
        // inputStreamからbitmap生成
        val imageBitmap = BitmapFactory.decodeStream(inputStream)
        // bitmapをjpeg形式に圧縮しバイト配列を生成
        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, ErikuraApplication.ID_IMAGE_QUALITY, stream)
        imageByteArray = stream.toByteArray()
        // ファイルサイズをMBに置き換えます
        val itemMbSize = imageByteArray.size /  1024.0 / 1024.0
        // 4MB超えている場合はリサイズします
        if (itemMbSize > 4) {
            var height = imageBitmap.height
            var width = imageBitmap.width
            val maxPx = ErikuraApplication.ID_IMAGE_MAX_SIZE
            if (height > width) {
                // 縦幅が長辺の場合
                val ratio = width.toDouble() / height
                height = maxPx
                width = (ratio * maxPx).toInt()
            } else {
                // 横幅が長辺の場合
                val ratio = height.toDouble() / width
                height = (ratio * maxPx).toInt()
                width = maxPx
            }
            item.resizeIdentifyImage(this, height, width) { bytes ->
                imageByteArray = bytes
            }
        }
        return imageByteArray!!
    }

    private fun encodeBase64FromImage(imageByteArray: ByteArray): String {
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
                val intent = Intent(this, ChangeUserInformationActivity::class.java)
                intent.putExtra(ErikuraApplication.FROM_WHERE, fromWhere)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            ErikuraApplication.FROM_ENTRY -> {
                //　身分証確認完了画面へ
                val intent = Intent(this, UploadedIdImageActivity::class.java)
                intent.putExtra(ErikuraApplication.FROM_WHERE, fromWhere)
                intent.putExtra("job", job)
                startActivityForResult(intent, ErikuraApplication.JOB_APPLY_BUTTON_REQUEST)
            }
        }
    }
}

class UploadIdImageViewModel : ViewModel() {
    var handler: UploadIdImageEventHandlers? = null
    val agreementText = MutableLiveData<SpannableStringBuilder>(
        SpannableStringBuilder().also { str ->
            JobUtil.appendLinkSpan(str, ErikuraApplication.instance.getString(R.string.registerEmail_terms_of_service), R.style.linkText) {
                handler?.onClickTermsOfService(it)
            }
            str.append(ErikuraApplication.instance.getString(R.string.registerEmail_comma))
            JobUtil.appendLinkSpan(str, ErikuraApplication.instance.getString(R.string.registerEmail_privacy_policy, ErikuraConfig.ppTermsTitle), R.style.linkText) {
                handler?.onClickPrivacyPolicy(it)
            }
            str.append(ErikuraApplication.instance.getString(R.string.registerEmail_agree))
        }
    )
    val driverLicenceElementNum = MutableLiveData<Int>()
    val passportElementNum = MutableLiveData<Int>()
    val myNumberElementNum = MutableLiveData<Int>()

    // 遷移元
    val fromWhere: MutableLiveData<Int> = MutableLiveData()
    // 身分証確認必須フラグ
    val identificationRequired: MutableLiveData<Int> = MutableLiveData()

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
        result.addSource(typeOfId) { id->
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

    var skipButtonVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(fromWhere) {
            if ((fromWhere.value == ErikuraApplication.FROM_ENTRY) && (identificationRequired.value == 1)) {
                result.value = View.GONE
            } else {
                result.value = View.VISIBLE
            }
        }
        result.addSource(identificationRequired) {
            if ((fromWhere.value == ErikuraApplication.FROM_ENTRY) && (identificationRequired.value == 1)) {
                result.value = View.GONE
            } else {
                result.value = View.VISIBLE
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

    // 身分証種別によって画像の数をバリデーション
    private fun isValidTypeOfId(): Boolean {
        return !(typeOfId.value == null)
    }

    private fun isValidPhoto(): Boolean {
        // 身分証種別ごと画像選択済みでかつ画像URLを取得できているか
        when (typeOfId.value) {
            passportElementNum.value -> {
                return ((addPassportFrontPhotoButtonVisibility.value == View.GONE) && (addPassportBackPhotoButtonVisibility.value == View.GONE)
                        && (otherPhotoPassportFront.contentUri != null) && (otherPhotoPassportBack.contentUri != null))
            }
            myNumberElementNum.value -> {
                return ((addMyNumberPhotoButtonVisibility.value == View.GONE) && (otherPhotoMyNumber.contentUri != null))
            }
            else -> {
                return ((addBackPhotoButtonVisibility.value == View.GONE) && (otherPhotoBack.contentUri != null)
                        && (addFrontPhotoButtonVisibility.value == View.GONE) && (otherPhotoFront.contentUri != null))
            }
        }
    }

    // パスポート、マイナンバー以外の場合
    private fun isNotPassportOrMyNumber(): Boolean {
        var isNotPassportOrMyNumber = false
        if (!((typeOfId.value == passportElementNum.value) || (typeOfId.value == myNumberElementNum.value))) {
            isNotPassportOrMyNumber = true
        }
        return isNotPassportOrMyNumber
    }

    fun setupHandler(handler: UploadIdImageEventHandlers?) {
        this.handler = handler
    }
}

interface UploadIdImageEventHandlers {
    fun onClickSkip(view: View)
    fun onClickTermsOfService(view: View)
    fun onClickPrivacyPolicy(view: View)

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

    //画像種別選択イベント
    fun onClickSpinner(view: View?)

    // 送信イベント
    fun onClickUploadIdImage(view: View)
}