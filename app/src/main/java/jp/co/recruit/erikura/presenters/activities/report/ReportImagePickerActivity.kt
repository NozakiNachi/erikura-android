package jp.co.recruit.erikura.presenters.activities.report

import android.Manifest
import android.app.ActivityOptions
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
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.MediaItem
import jp.co.recruit.erikura.business.models.OutputSummary
import jp.co.recruit.erikura.business.models.Report
import jp.co.recruit.erikura.databinding.ActivityReportImagePickerBinding
import jp.co.recruit.erikura.databinding.FragmentReportImagePickerCellBinding
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
import jp.co.recruit.erikura.presenters.fragments.ImagePickerCellView
import jp.co.recruit.erikura.presenters.util.RecyclerViewCursorAdapter

class ReportImagePickerActivity : AppCompatActivity(), ReportImagePickerEventHandler {
    private val viewModel by lazy {
        ViewModelProvider(this).get(ReportImagePickerViewModel::class.java)
    }
    private val REQUEST_PERMISSION = 2
    private val REQUEST_CODE_CHOOSE = 1
    private lateinit var adapter: ImagePickerAdapter
    var job: Job = Job()

    private fun hasStoragePermission(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return permissions.all { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun requestStoragePermission() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_image_picker)

        job = intent.getParcelableExtra<Job>("job")
        Log.v("DEBUG", job.toString())

        val binding: ActivityReportImagePickerBinding = DataBindingUtil.setContentView(this, R.layout.activity_report_image_picker)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        if(hasStoragePermission()) {
            displayImagePicker()
        }
        else {
            requestStoragePermission()
        }

    }

    private fun displayImagePicker() {
        adapter = ImagePickerAdapter(this).also {
            it.onClickListener = object: ImagePickerAdapter.OnClickListener {
                override fun onClick(item: MediaItem, isChecked: Boolean) {
                    onImageSelected(item, isChecked)
                }
            }
        }
        val recyclerView: RecyclerView = findViewById(R.id.report_image_picker_selection)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

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
            REQUEST_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    displayImagePicker()
                }
            }
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

    override fun onClickNext(view: View) {
        val outputSummaryList: MutableList<OutputSummary> = mutableListOf()
        viewModel.imageMap.forEach { (k, v) ->
            val summary = OutputSummary()
            summary.photoAsset = v
            outputSummaryList.add(summary)
        }

        if (job.report == null) {
            job.report = Report()
        }
        job.report?.let { report ->
            report.outputSummaries = outputSummaryList
            // FIXME: 画像のアップロード処理開始
        }

        val intent= Intent(this, ReportFormActivity::class.java)
        intent.putExtra("job", job)
        intent.putExtra("pictureIndex", 0)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }
}

class ReportImagePickerViewModel: ViewModel() {
    val imageMap: MutableMap<Long, MediaItem> = HashMap()
    val isNextButtonEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
}

class ImagePickerCellViewModel: ViewModel() {
    val checked = MutableLiveData<Boolean>(false)
}

class ImagePickerViewHolder(val binding: FragmentReportImagePickerCellBinding): RecyclerView.ViewHolder(binding.root)

class ImagePickerAdapter(val activity: FragmentActivity): RecyclerViewCursorAdapter<ImagePickerViewHolder>(null) {

    var onClickListener: OnClickListener? = null

    init {
        // cursor を作成して、それを this.cursor に設定する
        this.cursor = activity.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
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
    }

    interface OnClickListener {
        fun onClick(item: MediaItem, isChecked: Boolean)
    }
}

interface ReportImagePickerEventHandler {
    fun onClickManual(view: View)
    fun onClickNext(view: View)
}
