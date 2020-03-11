package jp.co.recruit.erikura.presenters.activities.report

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import io.realm.Realm
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.MediaItem
import jp.co.recruit.erikura.business.models.OutputSummary
import jp.co.recruit.erikura.business.models.Report
import jp.co.recruit.erikura.business.models.OperatorComment
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.storage.PhotoToken
import jp.co.recruit.erikura.databinding.ActivityReportConfirmBinding
import jp.co.recruit.erikura.databinding.FragmentReportImageItemBinding
import jp.co.recruit.erikura.databinding.FragmentReportSummaryItemBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import java.util.*


class ReportConfirmActivity : AppCompatActivity(), ReportConfirmEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportConfirmViewModel::class.java)
    }
    var job = Job()
    private val EDIT_DATA: Int = 1001
    private val GET_FILE: Int = 2001
    private lateinit var reportImageAdapter: ReportImageAdapter
    private lateinit var reportSummaryAdapter: ReportSummaryAdapter
    private val realm: Realm get() = ErikuraApplication.realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_confirm)

        val binding: ActivityReportConfirmBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_confirm)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        job = intent.getParcelableExtra<Job>("job")

        reportImageAdapter = ReportImageAdapter(this, listOf()).also {
            it.onClickListener =  object: ReportImageAdapter.OnClickListener {
                override fun onClick(view: View) {
                    onClickAddPhotoButton(view)
                }
            }
        }
        val reportImageView: RecyclerView = findViewById(R.id.report_confirm_report_images)
        reportImageView.adapter = reportImageAdapter

        reportSummaryAdapter = ReportSummaryAdapter(this, listOf()).also {
            it.onClickListener = object: ReportSummaryAdapter.OnClickListener {
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
        loadData()
    }

    override fun onClickComplete(view: View) {
        if(job.isReportCreatable || job.isReportEditable) {
            val missingPlaces = missingPlaces()
            if (missingPlaces.isEmpty()) {
                checkPhotoToken()
            }else {
                val dialog = MissingPlaceConfirmDialogFragment(missingPlaces).also {
                    it.onClickListener = object: MissingPlaceConfirmDialogFragment.OnClickListener {
                        override fun onClickComplete() {
                            checkPhotoToken()
                            it.dismiss()
                        }
                    }
                }

                dialog.show(supportFragmentManager, "MissingPlace")
            }
        }else {
            val errorMessages = mutableListOf(ErikuraApplication.instance.getString(R.string.report_confirm_over_limit))
            Api(this).displayErrorAlert(errorMessages)
        }

    }

    fun onClickAddPhotoButton(view: View) {
        if(ErikuraApplication.instance.hasStoragePermission(this)) {
            moveToGallery()
        }
        else {
            ErikuraApplication.instance.requestStoragePermission(this)
        }
    }

    fun editSummary(view: View, position: Int) {
        val intent= Intent(this, ReportFormActivity::class.java)
        intent.putExtra("job", job)
        intent.putExtra("pictureIndex", position)
        intent.putExtra("fromConfirm", true)
        startActivityForResult( intent, EDIT_DATA, ActivityOptions.makeSceneTransitionAnimation(this).toBundle() )
    }

    fun showRemoveSummary(view: View, position: Int) {
        val dialog = SummaryRemoveDialogFragment(position).also {
            it.onClickListener = object: SummaryRemoveDialogFragment.OnClickListener {
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
            outputSummaryList.removeAt(position)
            it.outputSummaries = outputSummaryList
            loadData()
        }
    }

    override fun onClickEditEvaluation(view: View) {
        val intent= Intent(this, ReportEvaluationActivity::class.java)
        intent.putExtra("job", job)
        intent.putExtra("fromConfirm", true)
        startActivityForResult( intent, EDIT_DATA, ActivityOptions.makeSceneTransitionAnimation(this).toBundle() )
    }

    override fun onClickEditOtherForm(view: View) {
        val intent= Intent(this, ReportOtherFormActivity::class.java)
        intent.putExtra("job", job)
        intent.putExtra("fromConfirm", true)
        startActivityForResult( intent, EDIT_DATA, ActivityOptions.makeSceneTransitionAnimation(this).toBundle() )
    }

    override fun onClickEditWorkingTime(view: View) {
        val intent= Intent(this, ReportWorkingTimeActivity::class.java)
        intent.putExtra("job", job)
        intent.putExtra("fromConfirm", true)
        startActivityForResult( intent, EDIT_DATA, ActivityOptions.makeSceneTransitionAnimation(this).toBundle() )
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
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
                        val summary = OutputSummary()
                        summary.photoAsset = item
                        // FIXME: 画像アップロード処理の実行
                        var outputSummaryList: MutableList<OutputSummary> = mutableListOf()
                        outputSummaryList = job.report?.outputSummaries?.toMutableList()?: mutableListOf()
                        outputSummaryList.add(summary)
                        job.report?.let {
                            it.outputSummaries = outputSummaryList
                        }
                    }

                    cursor?.close()
                }
            }

        }
    }

    private fun checkPhotoToken() {
        // アップロードが完了しているかの判定
        // token取得処理
        job.report?.let {report ->
            report.outputSummaries.forEach { summary ->
                summary.beforeCleaningPhotoToken = getPhotoToken(summary.photoAsset?.contentUri.toString())
            }
            report.additionalReportPhotoToken = getPhotoToken(report.additionalPhotoAsset?.contentUri.toString())
        }

        if (isCompletedUploadPhotos()) {
            // アップロードが完了しているので作業報告を保存します
            saveReport()
        }else {
            // 画像アップ中モーダル
            val uploadingDialog = UploadingDialogFragment()
            uploadingDialog.isCancelable = false
            uploadingDialog.show(supportFragmentManager, "Uploading")
            // timerで繰り返し処理
            val timer = Timer()
            val timerHandler = Handler()
            var count = 0
            timer.schedule(object : TimerTask() {
                override fun run() {
                    if (viewModel.completedUploadPhotos) {
                        timer.cancel()
                        // FIXME: レポート保存処理
                        uploadingDialog.dismiss()
                        saveReport()
                    }else if(count > 120 ) {
                        timer.cancel()
                        val failedDialog = UploadFailedDialogFragment().also {
                            it.onClickListener = object: UploadFailedDialogFragment.OnClickListener {
                                override fun onClickRetryButton() {
                                    // FIXME: 再試行処理
                                }
                                override fun onClickRemoveButton() {
                                    // レポートを削除して案件詳細画面へ遷移します
                                    removeAllContents()
                                }
                            }
                        }
                        failedDialog.isCancelable = false
                        failedDialog.show(supportFragmentManager, "UploadFailed")
                        uploadingDialog.dismiss()
                    }else {
                        timerHandler.post(Runnable {
                            updateToken()
                            val (numPhotos, numUploadedPhotos) = updateProgress()
                            uploadingDialog.numPhotos = numPhotos
                            uploadingDialog.numUploadedPhotos = numUploadedPhotos

                            count++
                        })
                    }
                }
            }, 1000, 1000) // 実行したい間隔(ミリ秒)
        }
    }

    // 1秒ごとに呼び出される処理
    private fun updateToken() {
        // token取得処理
        job.report?.let {report ->
            report.outputSummaries.forEach { summary ->
                summary.beforeCleaningPhotoToken = getPhotoToken(summary.photoAsset?.contentUri.toString())
            }
            report.additionalReportPhotoToken = getPhotoToken(report.additionalPhotoAsset?.contentUri.toString())
        }
        viewModel.completedUploadPhotos = isCompletedUploadPhotos()
    }

    private fun updateProgress(): Pair<Int, Int> {
        var numPhotos = 0
        var numUploadedPhotos = 0
        job.report?.let { report ->
            report.outputSummaries.forEach { summary ->
                if (summary.photoAsset?.contentUri != null) {
                    numPhotos++
                    if (!summary.beforeCleaningPhotoToken.isNullOrBlank()) {
                        numUploadedPhotos++
                    }
                }
            }
            if (report.additionalPhotoAsset?.contentUri != null) {
                numPhotos++
                if (!report.additionalReportPhotoToken.isNullOrBlank()) {
                    numUploadedPhotos++
                }
            }
        }

        return Pair(numPhotos, numUploadedPhotos)
    }

    private fun getPhotoToken(url: String): String {
        var token = ""
        realm.executeTransaction { realm ->
            var photo = realm.where(PhotoToken::class.java).equalTo("url", url).equalTo("jobId", job.id).findFirst()
            token = photo?.token?: ""
        }
        return token
    }

    private fun isCompletedUploadPhotos(): Boolean {
        var completed = true
        completed = completed && job.report?.isUploadCompleted?: true
        job.report?.let { report ->
            report.outputSummaries.forEach { summary ->
                completed = completed && summary.isUploadCompleted
            }
        }
        return completed
    }

    private fun saveReport() {

    }

    private fun missingPlaces(): List<String> {
        var summaryTitles: MutableList<String> = job.summaryTitles.toMutableList()
        var places: MutableList<String> = mutableListOf()
        job.report?.let { report ->
            report.outputSummaries.forEach { summary ->
                places.add(summary.place?: "")
            }
        }
        var missingPlaces: MutableList<String> = mutableListOf()
        summaryTitles.forEachIndexed { index, s ->
            if (!places.contains(s)) {
                missingPlaces.add("(${index+1}) ${s}")
            }
        }
        return missingPlaces
    }

    private fun moveToGallery() {
        val intent = Intent()
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(intent, GET_FILE )
    }

    private fun loadData() {
        job.report?.let {
            // 実施箇所の更新
            reportImageAdapter.summaries = it.outputSummaries
            reportImageAdapter.notifyDataSetChanged()
            reportSummaryAdapter.summaries = it.outputSummaries
            reportSummaryAdapter.notifyDataSetChanged()
            // 作業時間の更新
            val minute = it.workingMinute?: 0
            viewModel.workingTime.value = if(minute == 0){""}else {"${minute}分"}
            // マニュアル外報告の更新
            val item = it.additionalPhotoAsset?: MediaItem()
            if (item.contentUri != null) {
                val imageView: ImageView = findViewById(R.id.report_confirm_other_image)
                item.loadImage(this, imageView)
                viewModel.otherFormImageVisibility.value = View.VISIBLE
            }else {
                viewModel.otherFormImageVisibility.value = View.GONE
            }
            val additionalComment = it.additionalComment?: ""
            viewModel.otherFormComment.value = additionalComment
            // 案件評価の更新
            val evaluation = it.evaluation?: ""
            when(evaluation) {
                "good" ->
                    viewModel.evaluate.value = true
                "bad" ->
                    viewModel.evaluate.value = false
            }
            viewModel.evaluateButtonVisibility.value = if (evaluation.isNullOrEmpty()) {View.GONE} else {View.VISIBLE}
            val comment = it.comment?: ""
            viewModel.evaluationComment.value = comment

            viewModel.isCompleteButtonEnabled.value = viewModel.isValid(it)
        }
    }

    private fun retry() {

    }

    private fun removeAllContents() {
        job.report = null
        val intent= Intent(this, JobDetailsActivity::class.java)
        intent.putExtra("job", job)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }
}

class ReportConfirmViewModel: ViewModel() {
    val workingTime: MutableLiveData<String> = MutableLiveData()
    val otherFormImageVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val otherFormComment: MutableLiveData<String> = MutableLiveData()
    val evaluate: MutableLiveData<Boolean> = MutableLiveData()
    val evaluateButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val evaluationComment: MutableLiveData<String> = MutableLiveData()

    val isCompleteButtonEnabled: MutableLiveData<Boolean> = MutableLiveData()

    var completedUploadPhotos = false

    fun isValid(report: Report): Boolean {
        var valid = true
        if (report.outputSummaries.count() > 0) {
            report.outputSummaries.forEach {
                valid = valid && isValidSummary(it)
            }
        }else {
            valid = false
        }
        return valid
    }

    private fun isValidSummary(summary: OutputSummary): Boolean {
        return summary.photoAsset?.contentUri != null && !summary.place.isNullOrBlank() && !summary.comment.isNullOrBlank()
    }
}

interface ReportConfirmEventHandlers {
    fun onClickComplete(view: View)
    fun onClickEditOtherForm(view: View)
    fun onClickEditWorkingTime(view: View)
    fun onClickEditEvaluation(view: View)
    fun onClickManual(view: View)
}

// 実施箇所の一覧
class ReportImageItemViewModel(activity: Activity, view: View, mediaItem: MediaItem?): ViewModel() {
    private val imageView: ImageView = view.findViewById(R.id.report_image_item)
    val imageVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val addPhotoButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)
    init {
        if (mediaItem != null) {
            mediaItem.loadImage(activity, imageView)
        }else {
            imageVisibility.value = View.GONE
            addPhotoButtonVisibility.value = View.VISIBLE
        }
    }
}

class ReportImageViewHolder(val binding: FragmentReportImageItemBinding): RecyclerView.ViewHolder(binding.root)

class ReportImageAdapter(val activity: FragmentActivity, var summaries: List<OutputSummary>): RecyclerView.Adapter<ReportImageViewHolder>() {
    var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportImageViewHolder {
        val binding = DataBindingUtil.inflate<FragmentReportImageItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.fragment_report_image_item,
            parent,
            false
        )

        val view = binding.root
        val height = (parent.measuredWidth-40) / 3
        (view.layoutParams as? RecyclerView.LayoutParams)?.let { layoutParams ->
            layoutParams.height = height
            view.layoutParams = layoutParams
        }

        return ReportImageViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return summaries.count()+1
    }

    override fun onBindViewHolder(holder: ReportImageViewHolder, position: Int) {
        val view = holder.binding.root
        holder.binding.lifecycleOwner = activity
        if (position < summaries.count()) {
            holder.binding.viewModel = ReportImageItemViewModel(activity, view, summaries[position].photoAsset)
        }else {
            holder.binding.viewModel = ReportImageItemViewModel(activity, view, null)
            val button = holder.binding.root.findViewById<Button>(R.id.report_image_add_photo_button)
            button.setOnClickListener {
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
class ReportSummaryItemViewModel(activity: Activity, view: View, val summary: OutputSummary, summariesCount: Int, position: Int, jobDetails: Boolean): ViewModel() {

    val goodExist: Boolean get() = summary.operatorLikes
    val commentCount: Int get() = summary.operatorComments.count()
    val hasComment: Boolean get() = commentCount > 0
    val goodVisible: Int get() = if (goodExist) { View.VISIBLE } else { View.GONE }
    val commentVisible: Int get() = if (hasComment) { View.VISIBLE } else { View.GONE }
    val buttonsVisible: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val evaluationVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    val goodCommentsVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    private val imageView: ImageView = view.findViewById(R.id.report_summary_item_image)
    val summaryTitle: MutableLiveData<String> = MutableLiveData()
    val summaryName: MutableLiveData<String> = MutableLiveData()
    val summaryStatus: MutableLiveData<String> = MutableLiveData()
    val summaryComment: MutableLiveData<String> = MutableLiveData()
    val editSummaryButtonText: MutableLiveData<String> = MutableLiveData()
    val removeSummaryButtonText: MutableLiveData<String> = MutableLiveData()
    val summaryOperatorComment: MutableLiveData<List<OperatorComment>> = MutableLiveData()

    init {
        summary.photoAsset?.let {
            it.loadImage(activity, imageView)
        }

        summaryTitle.value = ErikuraApplication.instance.getString(R.string.report_form_caption, position+1, summariesCount)
        summaryName.value = summary.place
        summaryStatus.value = summary.evaluation
        summaryComment.value = summary.comment
        editSummaryButtonText.value = ErikuraApplication.instance.getString(R.string.edit_summary, position+1)
        removeSummaryButtonText.value = ErikuraApplication.instance.getString(R.string.remove_summary, position+1)
        summaryOperatorComment.value = summary.operatorComments

        if (jobDetails) {
            buttonsVisible.value = View.GONE
            evaluationVisible.value = View.VISIBLE
            goodCommentsVisible.value = View.VISIBLE
        }
    }
}

class ReportSummaryViewHolder(val binding: FragmentReportSummaryItemBinding): RecyclerView.ViewHolder(binding.root)

class ReportSummaryAdapter(val activity: FragmentActivity, var summaries: List<OutputSummary>, val jobDetails: Boolean = false): RecyclerView.Adapter<ReportSummaryViewHolder>() {
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
        holder.binding.viewModel = ReportSummaryItemViewModel(activity, view, summaries[position], summaries.count(), position, jobDetails)
        val editButton = holder.binding.root.findViewById<Button>(R.id.edit_report_summary_item)
        editButton.setOnClickListener {
            onClickListener?.apply {
                onClickEditButton(view, position)
            }
        }
        val removeButton = holder.binding.root.findViewById<Button>(R.id.remove_report_summary_item)
        removeButton.setOnClickListener {
            onClickListener?.apply {
                onClickRemoveButton(view, position)
            }
        }
    }

    interface OnClickListener {
        fun onClickEditButton(view: View, position: Int)
        fun onClickRemoveButton(view: View, position: Int)
    }
}