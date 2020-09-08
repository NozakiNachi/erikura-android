package jp.co.recruit.erikura.presenters.activities.report

import JobUtil
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.MediaItem
import jp.co.recruit.erikura.business.models.OutputSummary
import jp.co.recruit.erikura.business.models.Report
import jp.co.recruit.erikura.data.storage.PhotoTokenManager
import jp.co.recruit.erikura.databinding.ActivityReportImagePickerBinding
import jp.co.recruit.erikura.databinding.FragmentReportImagePickerCellBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.fragments.ImagePickerCellView
import jp.co.recruit.erikura.presenters.util.LocationManager
import jp.co.recruit.erikura.presenters.util.RecyclerViewCursorAdapter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class ReportImagePickerActivity : BaseActivity(), ReportImagePickerEventHandler {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportImagePickerViewModel::class.java)
    }
    private lateinit var adapter: ImagePickerAdapter
    private val locationManager: LocationManager = ErikuraApplication.locationManager
    private var editComplete = true

    var job: Job = Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_image_picker)

        job = intent.getParcelableExtra<Job>("job")
        Log.v("DEBUG", job.toString())
        ErikuraApplication.instance.reportingJob = job

        val binding: ActivityReportImagePickerBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_image_picker)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        // RecyclerView の初期化を行います
        val recyclerView: RecyclerView = findViewById(R.id.report_image_picker_selection)
        recyclerView.setHasFixedSize(true)

        val decorator = object: RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val space = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.0f, resources.displayMetrics).toInt()
                outRect.left = space
                outRect.right = space
                outRect.top = space
                outRect.bottom = space
            }
        }
        recyclerView.addItemDecoration(decorator)
    }

    override fun onStart() {
        super.onStart()
        ErikuraApplication.instance.reportingJob?.let {
            job = it
        }

        if(ErikuraApplication.instance.hasStoragePermission(this)) {
            displayImagePicker()
        }
        else {
            ErikuraApplication.instance.requestStoragePermission(this)
        }

        if (job.reportId == null) {
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_job_report_photo", params= bundleOf())
            Tracking.viewJobDetails(name= "/reports/register/photo/${job.id}", title= "作業報告画面（カメラロール）", jobId= job.id)
        }
        else {
            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_edit_job_report_photo", params= bundleOf())
            Tracking.viewJobDetails(name= "/reports/edit/photo/${job.id}", title= "作業報告編集画面（カメラロール）", jobId= job.id)
        }
        //お手本報告件数が0件の場合非表示
        job.goodExamplesCount?.let { reportExampleCount ->
            if (reportExampleCount == 0) {
                viewModel.reportExamplesButtonVisibility.value = View.GONE
            }
        }

        
    }

    private fun displayImagePicker() {
        adapter = ImagePickerAdapter(this, job, viewModel).also {
            it.onClickListener = object: ImagePickerAdapter.OnClickListener {
                override fun onClick(item: MediaItem, isChecked: Boolean) {
                    onImageSelected(item, isChecked)
                }
            }
        }
        val recyclerView: RecyclerView = findViewById(R.id.report_image_picker_selection)
        recyclerView.adapter = adapter

        if (editComplete) {
            // 選択状態をクリアします
            viewModel.imageMap.clear()
            // レポートの内容をもとに選択状態を復元します
            val outputSummaries = (job.report?.activeOutputSummaries ?: listOf())
            val assetsUrls = outputSummaries.map { it.photoAsset?.contentUri }.toSet()
            adapter.forEach { item ->
                if (assetsUrls.contains(item.contentUri)) {
                    viewModel.imageMap.put(item.id, item)
                }
            }
            viewModel.isNextButtonEnabled.value = viewModel.imageMap.isNotEmpty()
            editComplete = false
        }
    }

    fun onImageSelected(item: MediaItem, isChecked: Boolean) {
        val imageView: ImageView = findViewById(R.id.report_image_picker_preview)
        item.loadImage(this, imageView)

        if (isChecked) {
            viewModel.imageMap.put(item.id, item)
        }else {
            viewModel.imageMap.remove(item.id)
        }
        viewModel.isNextButtonEnabled.value = viewModel.imageMap.isNotEmpty()
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
                    displayImagePicker()
                }else {
                    val dialog = StorageAccessConfirmDialogFragment()
                    dialog.show(supportFragmentManager, "confirm")
                }
            }
        }
    }

    override fun onClickManual(view: View) {
        if(job?.manualUrl != null){
            JobUtil.openManual(this, job!!)
        }
    }

    override fun onClickNext(view: View) {
        val outputSummaryList: MutableList<OutputSummary> = mutableListOf()
        viewModel.imageMap.forEach { (k, v) ->
            val summary = OutputSummary()
            summary.photoAsset = v
            summary.retrieveImageProperties(this)
            outputSummaryList.add(summary)
        }
        // 撮影日時昇順でソートを行います
        outputSummaryList.sortWith(compareBy(OutputSummary::photoTakedAt))

        if (job.report == null) {
            job.report = Report()
        }
        job.report?.let { report ->
            report.outputSummaries = outputSummaryList
            outputSummaryList.forEach { outputSummary ->
                report.uploadPhoto(this, job, outputSummary.photoAsset){
                    PhotoTokenManager.addToken(job, outputSummary.photoAsset?.contentUri.toString(), it)
                }
            }
        }
        editComplete = true

        val intent= Intent(this, ReportFormActivity::class.java)
        intent.putExtra("job", job)
        intent.putExtra("pictureIndex", 0)
        startActivity(intent)
    }

    override fun onClickReportExamples(view: View) {
        job?.let { job ->
            JobUtil.openReportExample(this, job)
        }
    }

}

class ReportImagePickerViewModel: ViewModel() {
    val imageMap: MutableMap<Long, MediaItem> = HashMap()
    val isNextButtonEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
    val reportExamplesButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
}

class ImagePickerCellViewModel: ViewModel() {
    val checked = MutableLiveData<Boolean>(false)

    fun loadData(job: Job, item: MediaItem, viewModel: ReportImagePickerViewModel) {
        checked.value = viewModel.imageMap.containsKey(item.id)
    }
}

class ImagePickerViewHolder(val binding: FragmentReportImagePickerCellBinding): RecyclerView.ViewHolder(binding.root)

class ImagePickerAdapter(val activity: FragmentActivity, val job: Job, val viewModel: ReportImagePickerViewModel): RecyclerViewCursorAdapter<ImagePickerViewHolder>(null) {

    var onClickListener: OnClickListener? = null

    init {
        // cursor を作成して、それを this.cursor に設定する
        this.cursor = activity.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATE_TAKEN
            ),
            MediaStore.MediaColumns.SIZE + ">0",
            arrayOf<String>(),
            "datetaken DESC"
        )
    }

    fun forEach(callback: (item: MediaItem) -> Unit) {
        // 選択済み画像の対応を行います
        this.cursor?.let { cursor ->
            cursor.moveToFirst()
            while(!cursor.isAfterLast) {
                val item = MediaItem.from(cursor)
                callback(item)
                cursor.moveToNext()
            }
            cursor.moveToFirst()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagePickerViewHolder {
        val binding = DataBindingUtil.inflate<FragmentReportImagePickerCellBinding>(
            LayoutInflater.from(parent.context),
            R.layout.fragment_report_image_picker_cell,
            parent, false
        )
        val view = binding.root

        val aspect = 1.0
        val width = parent.measuredWidth
        val height = (width * aspect).toInt()
        (view.layoutParams as? RecyclerView.LayoutParams)?.let { layoutParams ->
            layoutParams.height = height
            view.layoutParams = layoutParams
        }

        return ImagePickerViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ImagePickerViewHolder, position: Int, cursor: Cursor) {
        val binding = viewHolder.binding
        viewHolder.binding.lifecycleOwner = activity
        viewHolder.binding.viewModel = ImagePickerCellViewModel()

        // もしくは画像のロード処理を実施する
        val view = binding.root
        val item = MediaItem.from(cursor)

        val cellView: ImagePickerCellView = view.findViewById(R.id.report_image_picker_cell)
        cellView.toggleClickListener = object: ImagePickerCellView.ToggleClickListener {
            override fun onClick(isChecked: Boolean) {
                onClickListener?.apply {
                    onClick(item, isChecked)
                }
            }
        }
        item.loadImage(activity, cellView.imageView)
        binding.viewModel!!.loadData(job, item, viewModel)
    }

    interface OnClickListener {
        fun onClick(item: MediaItem, isChecked: Boolean)
    }
}

interface ReportImagePickerEventHandler {
    fun onClickManual(view: View)
    fun onClickNext(view: View)
    fun onClickReportExamples(view: View)
}