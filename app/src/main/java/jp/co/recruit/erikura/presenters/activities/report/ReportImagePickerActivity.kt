package jp.co.recruit.erikura.presenters.activities.report

import JobUtil
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.*
import jp.co.recruit.erikura.business.util.ExifUtils
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.data.storage.PhotoTokenManager
import jp.co.recruit.erikura.data.storage.ReportDraft
import jp.co.recruit.erikura.databinding.ActivityReportImagePickerBinding
import jp.co.recruit.erikura.databinding.FragmentReportImagePickerCellBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.fragments.ImagePickerCellView
import jp.co.recruit.erikura.presenters.util.LocationManager
import jp.co.recruit.erikura.presenters.util.MessageUtils
import jp.co.recruit.erikura.presenters.util.RecyclerViewCursorAdapter
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
        ErikuraApplication.instance.currentJob = job

        val binding: ActivityReportImagePickerBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_report_image_picker)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        // RecyclerView ???????????????????????????
        val recyclerView: RecyclerView = findViewById(R.id.report_image_picker_selection)
        recyclerView.setHasFixedSize(true)

        val decorator = object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val space = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    1.0f,
                    resources.displayMetrics
                ).toInt()
                outRect.left = space
                outRect.right = space
                outRect.top = space
                outRect.bottom = space
            }
        }
        recyclerView.addItemDecoration(decorator)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    System.gc()
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        ErikuraApplication.instance.currentJob?.let {
            job = it
        }
        viewModel.job.value = job

        if (ErikuraApplication.instance.hasStoragePermission(this)) {
            displayImagePicker()
        } else {
            ErikuraApplication.instance.requestStoragePermission(this)
        }

        if (job.reportId == null) {
            // ?????????????????????????????????????????????
            Tracking.logEvent(event = "view_job_report_photo", params = bundleOf())
            Tracking.viewJobDetails(
                name = "/reports/register/photo/${job.id}",
                title = "??????????????????????????????????????????",
                jobId = job.id
            )
        } else {
            // ?????????????????????????????????????????????
            Tracking.logEvent(event = "view_edit_job_report_photo", params = bundleOf())
            Tracking.viewJobDetails(
                name = "/reports/edit/photo/${job.id}",
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

//        FirebaseCrashlytics.getInstance().recordException(MemoryTraceException(this.javaClass.name, getAvailableMemory()))
    }

    override fun onStop() {
        super.onStop()

        val recyclerView: RecyclerView = findViewById(R.id.report_image_picker_selection)
        recyclerView.adapter = null
        val imageView: ImageView = findViewById(R.id.report_image_picker_preview)
        Glide.with(this).clear(imageView)

        // GC????????????????????????
        System.gc()
    }

    private fun displayImagePicker() {
        adapter = ImagePickerAdapter(this, job, viewModel).also {
            it.onClickListener = object : ImagePickerAdapter.OnClickListener {
                override fun onClick(item: MediaItem, isChecked: Boolean) {
                    onImageSelected(item, isChecked)
                }
            }
            it.onModifyDataListener = object : ImagePickerAdapter.OnClickListener {
                override fun onClick(item: MediaItem, isChecked: Boolean) {
                    adapter.notifyDataSetChanged()
                }
            }
        }
        val recyclerView: RecyclerView = findViewById(R.id.report_image_picker_selection)
        recyclerView.adapter = adapter

        if (editComplete) {
            // ?????????????????????????????????
            viewModel.imageMap.clear()
            // ???????????????????????????????????????????????????????????????
            val outputSummaries = (job.report?.activeOutputSummaries ?: listOf())
            val assetsUrls = outputSummaries.map { it.photoAsset?.contentUri }.toSet()
            adapter.forEach { item ->
                if (assetsUrls.contains(item.contentUri)) {
                    viewModel.check(item)
                }
            }
            viewModel.isNextButtonEnabled.value = viewModel.imageMap.isNotEmpty()
            editComplete = false
        }
        // ??????????????????????????????????????????
        checkPhotoExistence()
    }

    fun onImageSelected(item: MediaItem, isChecked: Boolean) {
        val imageView: ImageView = findViewById(R.id.report_image_picker_preview)
        val width =
            imageView.layoutParams.width / ErikuraApplication.instance.resources.displayMetrics.density
        val height =
            imageView.layoutParams.height / ErikuraApplication.instance.resources.displayMetrics.density
        item.loadImage(this, imageView, width.toInt(), height.toInt())

        if (isChecked) {
            viewModel.check(item)
        } else {
            viewModel.uncheck(item)
        }
        viewModel.isNextButtonEnabled.value = viewModel.imageMap.isNotEmpty()
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
                    displayImagePicker()
                } else {
                    val dialog = StorageAccessConfirmDialogFragment()
                    dialog.show(supportFragmentManager, "confirm")
                }
            }
        }
    }

    override fun onClickManual(view: View) {
        if (job?.manualUrl != null) {
            JobUtil.openManual(this, job!!)
        }
    }

    override fun onBackPressed() {
        // ?????????????????????????????????????????????
        val intent = Intent(this, JobDetailsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("job", job)
        startActivity(intent)
    }

    override fun onClickNext(view: View) {
        // ??????????????????????????????????????????
        checkPhotoExistence()
        if (viewModel.imageMap.isEmpty()) {
            return
        }

        val outputSummaryList: MutableList<OutputSummary> = mutableListOf()
        viewModel.imageMap.forEach { (k, v) ->
            val summary = OutputSummary()
            summary.photoAsset = v
            summary.retrieveImageProperties(this)
            outputSummaryList.add(summary)
        }
        // ?????????????????????????????????????????????
        outputSummaryList.sortWith(compareBy(OutputSummary::photoTakedAt))

        if (job.report == null) {
            job.report = Report()
        }
        job.report?.let { report ->
            report.outputSummaries = outputSummaryList
            outputSummaryList.forEach { outputSummary ->
                report.uploadPhoto(this, job, outputSummary.photoAsset) {
                    PhotoTokenManager.addToken(
                        job,
                        outputSummary.photoAsset?.contentUri.toString(),
                        it
                    )
                }
            }
        }
        editComplete = true

        // ??????????????????????????????
        JobUtils.saveReportDraft(job, step = ReportDraft.ReportStep.SummaryForm, summaryIndex = 0)

        val intent = Intent(this, ReportFormActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("job", job)
        intent.putExtra("pictureIndex", 0)
        startActivity(intent)
    }

    override fun onClickReportExamples(view: View) {
        job?.let { job ->
            JobUtil.openReportExample(this, job)
        }
    }

    /**
     * ?????????????????????????????????????????????????????????
     */
    private fun checkPhotoExistence() {
        // ?????????????????????????????? imaveMap ?????????????????????
        var removedIds: MutableList<Long> = mutableListOf()
        viewModel.imageMap.forEach { (itemId, item) ->
            if (!item.isExists(this)) {
                // ????????????????????????????????????
                removedIds.add(itemId)
            }
        }
        removedIds.forEach { id -> viewModel.imageMap.remove(id) }
        // ?????????????????????????????????????????????
        viewModel.selectedCount.value = viewModel.imageMap.keys.size
    }
}

class ReportImagePickerViewModel : ViewModel() {
    val job: MutableLiveData<Job> = MutableLiveData()
    val imageMap: MutableMap<Long, MediaItem> = HashMap()
    val selectedCount: MutableLiveData<Int> = MutableLiveData(0)
    val isNextButtonEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
    val reportExamplesButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

    val summaryTitlesLabel = MediatorLiveData<String>().also { result ->
        result.addSource(job) {
            result.value = (job.value?.summaryTitles ?: listOf()).mapIndexed { i, title ->
                "(${i + 1}) ${title}"
            }.joinToString(" / ")
        }
    }

    fun check(item: MediaItem) {
        this.imageMap.put(item.id, item)
        this.selectedCount.value = this.imageMap.keys.size
    }

    fun uncheck(item: MediaItem) {
        this.imageMap.remove(item.id)
        this.selectedCount.value = this.imageMap.keys.size
    }
}

class ImagePickerCellViewModel : ViewModel() {
    val checked = MutableLiveData<Boolean>(false)

    fun loadData(job: Job, item: MediaItem, viewModel: ReportImagePickerViewModel) {
        checked.value = viewModel.imageMap.containsKey(item.id)
    }
}

class ImagePickerViewHolder(
    val binding: FragmentReportImagePickerCellBinding,
    val width: Int,
    val height: Int
) : RecyclerView.ViewHolder(binding.root)

class ImagePickerAdapter(
    val activity: FragmentActivity,
    val job: Job,
    val viewModel: ReportImagePickerViewModel
) : RecyclerViewCursorAdapter<ImagePickerViewHolder>(null) {

    var onClickListener: OnClickListener? = null
    var onModifyDataListener: OnClickListener? = null

    init {
        // cursor ??????????????????????????? this.cursor ???????????????
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
        // ??????????????????????????????????????????
        this.cursor?.let { cursor ->
            var processed = 0
            try {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val item = MediaItem.from(cursor)
                    callback(item)
                    processed++
                    cursor.moveToNext()
                }
                cursor.moveToFirst()
            } catch (e: Exception) {
                Tracking.logEvent(
                    event = "image_picker_each", params = bundleOf(
                        Pair("processed", processed)
                    )
                )
                Thread.sleep(500)
                // ????????????????????????
                throw e
            }
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

        return ImagePickerViewHolder(binding, height, height)
    }

    override fun onBindViewHolder(
        viewHolder: ImagePickerViewHolder,
        position: Int,
        cursor: Cursor
    ) {
        try {
            val binding = viewHolder.binding
            viewHolder.binding.lifecycleOwner = activity
            viewHolder.binding.viewModel = ImagePickerCellViewModel()

            // ???????????????????????????????????????????????????
            val view = binding.root
            val item = MediaItem.from(cursor)

            val cellView: ImagePickerCellView = view.findViewById(R.id.report_image_picker_cell)
            cellView.toggleClickListener = object : ImagePickerCellView.ToggleClickListener {
                @SuppressLint("SimpleDateFormat")
                override fun onClick(button: ToggleButton, isChecked: Boolean) {
                    var isChecked = isChecked
                    if (isChecked) {
                        // ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                        val uri = item.contentUri ?: Uri.EMPTY
                        val exifInterface = ExifUtils.exifInterface(activity, uri)
                        val takenAt = ExifUtils.takenAt(exifInterface)
                        val (width, height) = ExifUtils.size(activity, uri, exifInterface)

                        val isOld = takenAt?.let { takenAt ->
                            job.entryAt()?.let { entryAt ->
                                takenAt < entryAt
                            } ?: false
                        } ?: false

                        when {
                            // ??????????????????
                            (viewModel.selectedCount.value ?: 0) >= ErikuraConst.maxOutputSummaries -> {
                                // ???????????????????????????????????????????????????????????????
                                isChecked = false
                                button.isChecked = false
                                MessageUtils.displayAlert(
                                    activity,
                                    listOf("???????????????${ErikuraConst.maxOutputSummaries}???????????????????????????????????????")
                                )
                            }
                            // ?????????????????????????????????
                            height > width -> {
                                isChecked = false
                                button.isChecked = false
                                MessageUtils.displayAlert(activity, listOf("???????????????????????????????????????"))
                            }
                            // ???????????????????????????: ???????????? < ???????????????? (?????????????????????????????????????????? false)
                            isOld -> {
                                isChecked = false
                                button.isChecked = false

                                JobUtil.displayOldPictureWarning(activity, takenAt) {
                                    button.isChecked = true
                                    onClickListener?.apply {
                                        onClick(item, true)
                                    }
                                    onModifyDataListener?.apply {
                                        onClick(item, true)
                                    }
                                }
                            }
                        }
                    }

                    onClickListener?.apply {
                        onClick(item, isChecked)
                    }
                }
            }
            item.loadImage(activity, cellView.imageView, viewHolder.width, viewHolder.height)
            binding.viewModel!!.loadData(job, item, viewModel)
        } catch (e: Exception) {
            Tracking.logEvent(
                event = "image_picker_bind_viewholder", params = bundleOf(
                    Pair("position", position)
                )
            )
            Thread.sleep(500)
            // ????????????????????????
            throw e
        }
    }

    private fun displayNoticeOldTakenPictureDialog(entry_at: Date, taken_at: Date) {

    }


    override fun onViewRecycled(holder: ImagePickerViewHolder) {
        super.onViewRecycled(holder)

        val view = holder.binding.root
        val cellView: ImagePickerCellView = view.findViewById(R.id.report_image_picker_cell)
        Glide.with(activity).clear(cellView.imageView)
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