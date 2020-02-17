package jp.co.recruit.erikura.presenters.activities.report

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.database.getLongOrNull
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.databinding.FragmentReportImagePickerCellBinding
import jp.co.recruit.erikura.presenters.fragments.ImagePickerCellView
import jp.co.recruit.erikura.presenters.util.RecyclerViewCursorAdapter

class ReportImagePickerActivity : AppCompatActivity() {
    private val REQUEST_PERMISSION = 2
    private val REQUEST_CODE_CHOOSE = 1

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

        if(hasStoragePermission()) {
            displayImagePicker()
        }
        else {
            requestStoragePermission()
        }
    }

    private fun displayImagePicker() {
        val adapter = ImagePickerAdapter(this)
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
}

class ImagePickerCellViewModel: ViewModel() {
    val checked = MutableLiveData<Boolean>(false)
}

class ImagePickerViewHolder(val binding: FragmentReportImagePickerCellBinding): RecyclerView.ViewHolder(binding.root)

class ImagePickerAdapter(val activity: FragmentActivity): RecyclerViewCursorAdapter<ImagePickerViewHolder>(null) {

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
        binding.lifecycleOwner = activity
        // FIXME: viewModel を紐付ける
        // もしくは画像のロード処理を実施する

        val view = binding.root
        val item = MediaItem.from(cursor)
        val cellView: ImagePickerCellView = view.findViewById(R.id.report_image_picker_cell)
        item.loadThumbnail(activity, cellView.imageView)
    }
}

data class MediaItem(val id: Long, val mimeType: String, val size: Long, val contentUri: Uri) {
    companion object {
        fun from(cursor: Cursor): MediaItem {
            val id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
            val mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE))
            val size = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE))
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

            return MediaItem(id = id, mimeType = mimeType, size = size, contentUri = uri)
        }
    }

    fun loadThumbnail(context: Context, imageView: ImageView) {
        Glide.with(context).load(contentUri).into(imageView)
    }
}
