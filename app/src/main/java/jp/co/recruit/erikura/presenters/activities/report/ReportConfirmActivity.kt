package jp.co.recruit.erikura.presenters.activities.report

import JobUtil
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.*
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.storage.PhotoTokenManager
import jp.co.recruit.erikura.databinding.ActivityReportConfirmBinding
import jp.co.recruit.erikura.databinding.FragmentReportImageItemBinding
import jp.co.recruit.erikura.databinding.FragmentReportSummaryItemBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.OwnJobsActivity
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.util.setOnSafeClickListener
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ReportConfirmActivity : BaseActivity(), ReportConfirmEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportConfirmViewModel::class.java)
    }
    var job = Job()
    private val EDIT_DATA: Int = 1001
    private val GET_FILE: Int = 2001
    private lateinit var reportImageAdapter: ReportImageAdapter
    private lateinit var reportSummaryAdapter: ReportSummaryAdapter
    private val realm: Realm get() = ErikuraApplication.realm
    lateinit var positions: Array<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_confirm)

        val binding: ActivityReportConfirmBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_report_confirm)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        job = intent.getParcelableExtra<Job>("job")
        ErikuraApplication.instance.reportingJob = job

        reportImageAdapter = ReportImageAdapter(this, listOf()).also {
            it.onClickListener = object : ReportImageAdapter.OnClickListener {
                override fun onClick(view: View) {
                    onClickAddPhotoButton(view)
                }
            }
        }
        val reportImageView: RecyclerView = findViewById(R.id.report_confirm_report_images)
        reportImageView.adapter = reportImageAdapter

        reportSummaryAdapter = ReportSummaryAdapter(this, listOf()).also {
            it.onClickListener = object : ReportSummaryAdapter.OnClickListener {
                override fun onClickEditButton(view: View, position: Int) {
                    editSummary(view, position)
                }

                override fun onClickRemoveButton(view: View, position: Int) {
                    showRemoveSummary(view, position)
                }
            }
        }
        val reportSummaryView: RecyclerView = findViewById(R.id.report_confirm_report_summaries)
        reportSummaryView.setHasFixedSize(true)
        reportSummaryView.adapter = reportSummaryAdapter
    }

    override fun onStart() {
        super.onStart()
        ErikuraApplication.instance.reportingJob?.let {
            job = it
        }

        loadData()

        if (job.reportId == null) {
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_job_report_confirm", params= bundleOf())
            Tracking.viewJobDetails(name= "/reports/register/confirm/${job.id}", title= "作業報告確認画面", jobId= job.id)
        }else {
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_edit_job_report_confirm", params= bundleOf())
            Tracking.viewJobDetails(name= "/reports/edit/confirm/${job.id}", title= "作業報告編集確認画面", jobId= job.id)
        }
    }

    override fun onClickComplete(view: View) {
        if (job.isReportCreatable || job.isReportEditable) {
            val missingPlaces = missingPlaces()
            if (missingPlaces.isEmpty()) {
                checkPhotoToken()
            } else {
                val dialog = MissingPlaceConfirmDialogFragment.newInstance(missingPlaces).also {
                    it.onClickListener =
                        object : MissingPlaceConfirmDialogFragment.OnClickListener {
                            override fun onClickComplete() {
                                it.dismiss()
                                checkPhotoToken()
                            }
                        }
                }

                dialog.show(supportFragmentManager, "MissingPlace")
            }
        } else {
            val errorMessages =
                mutableListOf(ErikuraApplication.instance.getString(R.string.report_confirm_over_limit))
            Api(this).displayErrorAlert(errorMessages)
        }

    }

    fun onClickAddPhotoButton(view: View) {
        if (ErikuraApplication.instance.hasStoragePermission(this)) {
            moveToGallery()
        } else {
            ErikuraApplication.instance.requestStoragePermission(this)
        }
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

    fun editSummary(view: View, position: Int) {
        val intent = Intent(this, ReportFormActivity::class.java)
        positions.forEachIndexed { index, i ->
            if (i == position) {
                intent.putExtra("pictureIndex", index)
            }
        }
        intent.putExtra("job", job)
        intent.putExtra("fromConfirm", true)
        startActivityForResult(
            intent,
            EDIT_DATA,
            ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
        )
    }

    fun showRemoveSummary(view: View, position: Int) {
        val dialog = SummaryRemoveDialogFragment.newInstance(position).also {
            it.onClickListener = object : SummaryRemoveDialogFragment.OnClickListener {
                override fun onClickRemoveButton() {
                    removeSummary(position)
                    it.dismiss()
                }
            }
        }

        dialog.show(supportFragmentManager, "Remove")
    }

    fun removeSummary(position: Int) {
        job.report?.let {
            var outputSummaryList: MutableList<OutputSummary> = mutableListOf()
            outputSummaryList = it.outputSummaries.toMutableList()
            positions.forEachIndexed { index, i ->
                if (i == position) {
                    outputSummaryList[index].willDelete = true
                }
            }
//            outputSummaryList.removeAt(position)
            it.outputSummaries = outputSummaryList
            loadData()
        }
    }

    override fun onClickEditEvaluation(view: View) {
        val intent = Intent(this, ReportEvaluationActivity::class.java)
        intent.putExtra("job", job)
        intent.putExtra("fromConfirm", true)
        startActivityForResult(
            intent,
            EDIT_DATA,
            ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
        )
    }

    override fun onClickEditOtherForm(view: View) {
        val intent = Intent(this, ReportOtherFormActivity::class.java)
        intent.putExtra("job", job)
        intent.putExtra("fromConfirm", true)
        startActivityForResult(
            intent,
            EDIT_DATA,
            ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
        )
    }

    override fun onClickEditWorkingTime(view: View) {
        val intent = Intent(this, ReportWorkingTimeActivity::class.java)
        intent.putExtra("job", job)
        intent.putExtra("fromConfirm", true)
        startActivityForResult(
            intent,
            EDIT_DATA,
            ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
        )
    }

    override fun onClickManual(view: View) {
        if (job?.manualUrl != null) {
            JobUtil.openManual(this, job!!)
        }
    }

    override fun onClickReportExamples(view: View) {
        job?.let { job ->
            JobUtil.openReportExample(this, job)
        }
    }

    @SuppressLint("SimpleDateFormat")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            // 編集画面から戻ってきたとき
            EDIT_DATA -> {
                data?.let {
                    job = data.getParcelableExtra<Job>("job")
                }
            }
            // ギャラリーから戻ってきたとき
            GET_FILE -> {
                val uri = data?.data
                uri?.let {
                    MediaItem.createFrom(this, uri)?.let { item ->
                        val summary = OutputSummary()
                        summary.photoAsset = item

                        val cr = contentResolver.openInputStream(uri)
                        val exifInterface = ExifInterface(cr)
                        val takenAtString = exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                        val takenAt = takenAtString?.let {
                            SimpleDateFormat("yyyy:MM:dd HH:mm").parse(it)
                        } ?: item.dateTaken?.let {
                            Date(item.dateTaken)
                        } ?: item.dateAdded?.let {
                            Date(item.dateAdded * 1000)    // 秒単位なので、x1000してミリ秒にする
                        }
                        val latitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
                        val latitudeRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
                        val longitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
                        val longitudeRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)
                        summary.photoTakedAt = takenAt
                        summary.latitude = latitude?.let { lat ->
                            latitudeRef?.let { ref ->
                                MediaItem.exifLatitudeToDegrees(ref, lat)
                            }
                        }
                        summary.longitude = longitude?.let { lon ->
                            longitudeRef?.let { ref ->
                                MediaItem.exifLongitudeToDegrees(ref, lon)
                            }
                        }

                        var outputSummaryList: MutableList<OutputSummary> = mutableListOf()
                        outputSummaryList =
                            job.report?.outputSummaries?.toMutableList() ?: mutableListOf()
                        outputSummaryList.add(summary)
                        job.report?.let {
                            it.outputSummaries = outputSummaryList
                            it.uploadPhoto(this, job, summary.photoAsset){ token ->
                                PhotoTokenManager.addToken(job, summary.photoAsset?.contentUri.toString(), token)
                            }
                        }
                    }
                }
            }

        }
    }

    private fun checkPhotoToken() {
        if (isCompletedUploadPhotos()) {
            // アップロードが完了しているので作業報告を保存します
            saveReport()
        } else {
            // アップロードが完了するのを待ちます
            waitUpload()
        }
    }



    private fun waitUpload() {
        val maxCount = 100

        // 画像アップ中モーダルの表示
        val uploadingDialog = UploadingDialogFragment()
        uploadingDialog.isCancelable = false
        uploadingDialog.show(supportFragmentManager, "Uploading")

        val observable: Observable<Int> = Observable.create {
            try {
                var count = 0

                // アップロードが完了する、もしくはアップロード中のものがなくなるまで繰り返します
                while (!(isCompletedUploadPhotos() || !isUploadingPhotos())) {
                    if (count < maxCount) {
                        this.runOnUiThread {
                            // 画像アップの進捗表示更新
                            val (numPhotos, numUploadedPhotos) = updateProgress()
                            uploadingDialog.numPhotos = numPhotos
                            uploadingDialog.numUploadedPhotos = numUploadedPhotos
                        }

                        ErikuraApplication.instance.waitUpload()

                        count++
                    } else {
                        break
                    }
                }

                it.onNext(count)
                it.onComplete()
            }
            catch (e: IOException) {
                Log.e("Error in waiting upload", e.message, e)
                it.onError(e)
            }
        }

        observable.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { count ->
                    Log.d("Upload Next", count.toString())
                    uploadingDialog.dismiss()
                    if (!isCompletedUploadPhotos()) {
                        // 画像アップ不可モーダル表示
                        val failedDialog = UploadFailedDialogFragment().also {
                            it.onClickListener = object : UploadFailedDialogFragment.OnClickListener {
                                override fun onClickRetryButton() {
                                    it.dismiss()
                                    retry()
                                }

                                override fun onClickRemoveButton() {
                                    it.dismiss()
                                    // レポートを削除して案件詳細画面へ遷移します
                                    removeAllContents()
                                }
                            }
                        }
                        failedDialog.isCancelable = false
                        failedDialog.show(supportFragmentManager, "UploadFailed")
                    } else {
                        saveReport()
                    }
                },
                onError = { e ->
                    Log.e("Upload Error", e.message, e)
                    uploadingDialog.dismiss()
                    // 画像アップ不可モーダル表示
                    val failedDialog = UploadFailedDialogFragment().also {
                        it.onClickListener = object : UploadFailedDialogFragment.OnClickListener {
                            override fun onClickRetryButton() {
                                it.dismiss()
                                retry()
                            }

                            override fun onClickRemoveButton() {
                                it.dismiss()
                                // レポートを削除して案件詳細画面へ遷移します
                                removeAllContents()
                            }
                        }
                    }
                    failedDialog.isCancelable = false
                    failedDialog.show(supportFragmentManager, "UploadFailed")
                }
            )
    }

    private fun updateProgress(): Pair<Int, Int> {
        var numPhotos = 0
        var numUploadedPhotos = 0
        job.report?.let { report ->
            report.activeOutputSummaries.forEach { summary ->
                if (summary.photoAsset?.contentUri != null) {
                    numPhotos++
                    if (summary.isUploadCompleted(job)) {
                        numUploadedPhotos++
                    }
                }
            }
            if (report.additionalPhotoAsset?.contentUri != null) {
                numPhotos++
                if (report.isUploadCompleted(job)) {
                    numUploadedPhotos++
                }
            }
        }

        return Pair(numPhotos, numUploadedPhotos)
    }

    private fun isCompletedUploadPhotos(): Boolean {
        var completed = true
        job.report?.let {
            completed = it.isUploadCompleted(job) and it.isOutputSummaryPhotoUploadCompleted(job)
        }
        return completed
    }

    private fun isUploadingPhotos(): Boolean {
        return job.report?.let {
            it.isUploading() || it.isOutputSummaryPhotoUploading()
        } ?: false
    }

    private fun saveReport() {
        Api(this).report(job) {
            // アップロード完了
            if (job.reportId != null) {
                // ページ参照のトラッキングの送出
                Tracking.logEvent(event= "view_edit_job_report_finish", params= bundleOf())
                Tracking.viewJobDetails(name= "/reports/edit/completed/${job.id}", title= "作業報告編集完了画面", jobId= job.id)
            }
            else {
                // ページ参照のトラッキングの送出
                Tracking.logEvent(event= "view_job_report_finish", params= bundleOf())
                Tracking.viewJobDetails(name= "/reports/register/completed/${job.id ?: 0}", title= "作業報告完了画面", jobId= job.id ?: 0)
            }

            // アップロード用のトークンをクリアします
            PhotoTokenManager.clearToken(job)

            // 作業報告のトラッキングの送出
            Tracking.logEvent(event= "job_report", params= bundleOf())
            Tracking.jobEntry(name= "job_report", title= "", job= job)
            Tracking.logEventFB(event= "ReportJob")

            Intent(this, OwnJobsActivity::class.java).let { intent ->
//                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra(OwnJobsActivity.EXTRA_FROM_REPORT_COMPLETED_KEY, true)
                startActivity(intent)
            }
        }
    }

    private fun missingPlaces(): List<String> {
        var summaryTitles: MutableList<String> = job.summaryTitles.toMutableList()
        var places: MutableList<String> = mutableListOf()
        job.report?.let { report ->
            report.activeOutputSummaries.forEach { summary ->
                places.add(summary.place ?: "")
            }
        }
        var missingPlaces: MutableList<String> = mutableListOf()
        summaryTitles.forEachIndexed { index, s ->
            if (!places.contains(s)) {
                missingPlaces.add("(${index + 1}) ${s}")
            }
        }
        return missingPlaces
    }

    private fun moveToGallery() {
        val intent = Intent()
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(intent, GET_FILE)
    }

    private fun loadData() {
        job.report?.let {
            // 実施箇所の更新
            var summaries = mutableListOf<OutputSummary>()
            positions = Array(it.outputSummaries.count(), {-1})
            var i = 0
            it.outputSummaries.forEachIndexed {index, summary ->
                if (!summary.willDelete) {
                    positions[index] = i
                    summaries.add(summary)
                    i++
                }
            }
            reportImageAdapter.summaries = summaries
            reportImageAdapter.notifyDataSetChanged()
            reportSummaryAdapter.summaries = summaries
            reportSummaryAdapter.notifyDataSetChanged()
            // 作業時間の更新
            val minute = it.workingMinute ?: 0
            viewModel.workingTime.value = if (minute == 0) {
                ""
            } else {
                "${minute}分"
            }
            // マニュアル外報告の更新
            val item = it.additionalPhotoAsset ?: MediaItem()
            if (item.contentUri != null) {
                val imageView: ImageView = findViewById(R.id.report_confirm_other_image)
                if (it.additionalReportPhotoUrl!= null) {
                    item.loadImageFromString(this, imageView)
                }else {
                    item.loadImage(this, imageView)
                }
                viewModel.otherFormImageVisibility.value = View.VISIBLE
            } else {
                viewModel.otherFormImageVisibility.value = View.GONE
            }
            val additionalComment = it.additionalComment ?: ""
            viewModel.otherFormComment.value = additionalComment
            // 案件評価の更新
            val evaluation = it.evaluation ?: ""
            when (evaluation) {
                "good" ->
                    viewModel.evaluate.value = true
                "bad" ->
                    viewModel.evaluate.value = false
            }
            viewModel.evaluateButtonVisibility.value = if (evaluation.isNullOrEmpty() || evaluation == "unanswered") {
                View.GONE
            } else {
                View.VISIBLE
            }
            val comment = it.comment ?: ""
            viewModel.evaluationComment.value = comment

            viewModel.isCompleteButtonEnabled.value = viewModel.isValid(it)
        }
        //お手本報告件数が0件の場合非表示
        job.goodExamplesCount?.let { reportExampleCount ->
            if (reportExampleCount == 0) {
                viewModel.reportExamplesButtonVisibility.value = View.GONE
            }
        }
    }

    private fun retry() {
        job.report?.let { report ->
            report.activeOutputSummaries.forEach { outputSummary ->
                if (!outputSummary.isUploadCompleted(job)) {
                    report.uploadPhoto(this, job, outputSummary.photoAsset) { token ->
                        PhotoTokenManager.addToken(
                            job,
                            outputSummary.photoAsset?.contentUri.toString(),
                            token
                        )
                    }
                }
            }
            if (report.additionalPhotoAsset != null) {
                if (!report.isUploadCompleted(job)) {
                    report.uploadPhoto(this, job, report.additionalPhotoAsset) { token ->
                        PhotoTokenManager.addToken(
                            job,
                            report.additionalPhotoAsset?.contentUri.toString(),
                            token
                        )
                    }
                }
            }
        }
        waitUpload()
    }

    private fun removeAllContents() {
        val intent = Intent(this, JobDetailsActivity::class.java)
        intent.putExtra("job", job)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }
}

class ReportConfirmViewModel : ViewModel() {
    val workingTime: MutableLiveData<String> = MutableLiveData()
    val otherFormImageVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val otherFormComment: MutableLiveData<String> = MutableLiveData()
    val evaluate: MutableLiveData<Boolean> = MutableLiveData()
    val evaluateButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val evaluationComment: MutableLiveData<String> = MutableLiveData()

    val isCompleteButtonEnabled: MutableLiveData<Boolean> = MutableLiveData()
    val reportExamplesButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

    fun isValid(report: Report): Boolean {
        var valid = true
        var summaryNotDeletedCount = 0
        summaryNotDeletedCount = report.activeOutputSummaries.count()
        if (summaryNotDeletedCount > 0) {
            report.activeOutputSummaries.forEach {
                valid = valid && isValidSummary(it)
            }
        } else {
            valid = false
        }
        return valid
    }

    private fun isValidSummary(summary: OutputSummary): Boolean {
        return (summary.photoAsset?.contentUri != null && !summary.place.isNullOrBlank() && !summary.comment.isNullOrBlank()) || summary.willDelete
    }
}

interface ReportConfirmEventHandlers {
    fun onClickComplete(view: View)
    fun onClickEditOtherForm(view: View)
    fun onClickEditWorkingTime(view: View)
    fun onClickEditEvaluation(view: View)
    fun onClickManual(view: View)
    fun onClickReportExamples(view: View)
}

// 実施箇所の一覧
class ReportImageItemViewModel(activity: Activity, view: View, mediaItem: MediaItem?, isUrlExist: Boolean) :
    ViewModel() {
    private val imageView: ImageView = view.findViewById(R.id.report_image_item)
    val imageVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val addPhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    init {
        if (mediaItem != null) {
            if(isUrlExist) {
                mediaItem.loadImageFromString(activity, imageView)
            }else {
                mediaItem.loadImage(activity, imageView)
            }
        } else {
            imageVisibility.value = View.GONE
            addPhotoButtonVisibility.value = View.VISIBLE
        }
    }
}

class ReportImageViewHolder(val binding: FragmentReportImageItemBinding) :
    RecyclerView.ViewHolder(binding.root)

class ReportImageAdapter(val activity: FragmentActivity, var summaries: List<OutputSummary>) :
    RecyclerView.Adapter<ReportImageViewHolder>() {
    var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportImageViewHolder {
        val binding = DataBindingUtil.inflate<FragmentReportImageItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.fragment_report_image_item,
            parent,
            false
        )

        val view = binding.root
        val height = (parent.measuredWidth - 40) / 3
        (view.layoutParams as? RecyclerView.LayoutParams)?.let { layoutParams ->
            layoutParams.height = height
            view.layoutParams = layoutParams
        }

        return ReportImageViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return summaries.count() + 1
    }

    override fun onBindViewHolder(holder: ReportImageViewHolder, position: Int) {
        val view = holder.binding.root
        holder.binding.lifecycleOwner = activity
        if (position < summaries.count()) {
            holder.binding.viewModel =
                ReportImageItemViewModel(activity, view, summaries[position].photoAsset, !summaries[position].beforeCleaningPhotoUrl.isNullOrBlank())
        } else {
            holder.binding.viewModel = ReportImageItemViewModel(activity, view, null, false)
            val button =
                holder.binding.root.findViewById<Button>(R.id.report_image_add_photo_button)
            button.setOnSafeClickListener {
                onClickListener?.apply {
                    onClick(view)
                }
            }
        }

    }

    interface OnClickListener {
        fun onClick(view: View)
    }
}

// 実施箇所
class ReportSummaryItemViewModel(
    activity: FragmentActivity,
    view: View,
    val summary: OutputSummary,
    summariesCount: Int,
    position: Int,
    jobDetails: Boolean
) : ViewModel() {
    val buttonsVisible: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val evaluationVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    private val imageView: ImageView = view.findViewById(R.id.report_summary_item_image)
    val summaryTitle: MutableLiveData<String> = MutableLiveData()
    val summaryName: MutableLiveData<String> = MutableLiveData()
    val summaryStatus: MutableLiveData<String> = MutableLiveData()
    val summaryComment: MutableLiveData<String> = MutableLiveData()
    val editSummaryButtonText: MutableLiveData<String> = MutableLiveData()
    val removeSummaryButtonText: MutableLiveData<String> = MutableLiveData()
    val commentCountVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val commentCount: MutableLiveData<String> = MutableLiveData()
    val goodCountVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val goodCount: MutableLiveData<String> = MutableLiveData()
    // 運営からのコメント
    val operatorComment: MutableLiveData<String> = MutableLiveData()
    val operatorCommentCreatedAt: MutableLiveData<String> = MutableLiveData()

    init {
        summary.photoAsset?.let {
            if (summary.beforeCleaningPhotoUrl != null){
                it.loadImageFromString(activity, imageView)
            }else {
                it.loadImage(activity, imageView)
            }
        }

        summaryTitle.value = ErikuraApplication.instance.getString(
            R.string.report_form_caption,
            position + 1,
            summariesCount
        )
        summaryName.value = summary.place
        val evaluateType = EvaluateType.valueOf(summary.evaluation?.toUpperCase()?: "UNSELECTED")
        when(evaluateType) {
            EvaluateType.UNSELECTED -> {
                summaryStatus.value = ""
            }
            else -> {
                summaryStatus.value = ErikuraApplication.instance.getString(evaluateType.resourceId)
            }
        }
        summaryComment.value = summary.comment
        editSummaryButtonText.value =
            ErikuraApplication.instance.getString(R.string.edit_summary, position + 1)
        removeSummaryButtonText.value =
            ErikuraApplication.instance.getString(R.string.remove_summary, position + 1)

        if (jobDetails) {
            buttonsVisible.value = View.GONE
            if (summary.operatorComments.isNotEmpty()) {
                commentCount.value = "${summary.operatorComments.count()}件"
                commentCountVisibility.value = View.VISIBLE
                evaluationVisible.value = View.VISIBLE
                operatorComment.value = summary.operatorComments.first().body
                operatorCommentCreatedAt.value = JobUtils.DateFormats.simple.format(summary.operatorComments.first().createdAt)
            }
            if (summary.operatorLikes) {
                goodCount.value = "1件"
                goodCountVisibility.value = View.VISIBLE
            }
        }
    }
}

class ReportSummaryViewHolder(val binding: FragmentReportSummaryItemBinding) :
    RecyclerView.ViewHolder(binding.root)

class ReportSummaryAdapter(
    val activity: FragmentActivity,
    var summaries: List<OutputSummary>,
    val jobDetails: Boolean = false
) : RecyclerView.Adapter<ReportSummaryViewHolder>() {
    var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportSummaryViewHolder {
        val binding = DataBindingUtil.inflate<FragmentReportSummaryItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.fragment_report_summary_item,
            parent,
            false
        )

        return ReportSummaryViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return summaries.count()
    }

    override fun onBindViewHolder(holder: ReportSummaryViewHolder, position: Int) {
        val view = holder.binding.root
        holder.binding.lifecycleOwner = activity
        holder.binding.viewModel = ReportSummaryItemViewModel(
            activity,
            view,
            summaries[position],
            summaries.count(),
            position,
            jobDetails
        )
        val editButton = holder.binding.root.findViewById<Button>(R.id.edit_report_summary_item)
        editButton.setOnSafeClickListener {
            onClickListener?.apply {
                onClickEditButton(view, position)
            }
        }
        val removeButton = holder.binding.root.findViewById<Button>(R.id.remove_report_summary_item)
        removeButton.setOnSafeClickListener {
            onClickListener?.apply {
                onClickRemoveButton(view, position)
            }
        }

        // MEMO: 運営からのコメントは実施箇所につき1つの想定
//        val commentView: RecyclerView = holder.binding.root.findViewById(R.id.summaryItem_operatorComments)
//        val operatorCommentsAdapter = OperatorCommentAdapter(activity, listOf())
//        commentView.adapter = operatorCommentsAdapter
//        operatorCommentsAdapter.operatorComments = summaries[position].operatorComments
//        operatorCommentsAdapter.notifyDataSetChanged()
    }

    interface OnClickListener {
        fun onClickEditButton(view: View, position: Int)
        fun onClickRemoveButton(view: View, position: Int)
    }
}