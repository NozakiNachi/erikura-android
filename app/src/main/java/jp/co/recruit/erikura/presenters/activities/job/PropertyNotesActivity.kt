package jp.co.recruit.erikura.presenters.activities.job

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.*
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Caution
import jp.co.recruit.erikura.business.models.CautionFile
import jp.co.recruit.erikura.business.models.ErikuraConfig
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.storage.Asset
import jp.co.recruit.erikura.databinding.ActivityPropertyNotesBinding
import jp.co.recruit.erikura.databinding.FragmentPropertyNotesItemBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
import okhttp3.internal.closeQuietly
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PropertyNotesActivity : BaseActivity(), PropertyNotesEventHandlers {
    private val viewModel: PropertyNotesViewModel by lazy {
        ViewModelProvider(this).get(PropertyNotesViewModel::class.java)
    }
    private var cautions: List<Caution> = listOf()
    private var jobId: Int? = null
    private var placeId: Int? = null
    val api = Api(this)
    private var jobKindId: Int? = null

    private lateinit var propertyNotesAdapter: PropertyNotesAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        jobId = intent.getIntExtra("job_id", 0)
        placeId = intent.getIntExtra("place_id", 0)
        jobKindId = intent.getIntExtra("job_kind_id", 0)

        if (intent.data?.path != null && placeId == 0) {
            Log.v("DEBUG","ここ通る！")
            Log.v("DEBUG","path=${intent.data?.path}")
            Log.v("DEBUG","placeId~${placeId}")
            // FDLの場合
            api.reloadJobById(handleIntent(intent)) { job ->
                jobId = job.id
                placeId = job.placeId
                jobKindId = job.jobKind?.id
                Log.v("DEBUG","placeId~${placeId}")
                Log.v("DEBUG","jobId~${jobId}")
                Log.v("DEBUG","jobKindId~${jobKindId}")
                Tracking.logEvent(event = "view_cautions", params = bundleOf())
                Tracking.viewCautions(
                    name = "/places/cautions",
                    title = "物件注意事項画面表示",
                    jobId = job.id,
                    placeId = job.placeId
                )
                // 物件の注意事項を取得
                if (jobId != null || placeId != null) {
                    api.placeCautions(jobId, placeId, jobKindId) {
                        //ボタンのラベルを生成しセット
                        cautions = it
                        propertyNotesAdapter.cautions = it
                        propertyNotesAdapter.notifyDataSetChanged()
                        api.place(placeId?: 0) { place ->
                            if (place.hasEntries || place.workingPlaceShort.isNullOrEmpty()) {
                                // 現ユーザーが応募済の物件の場合　フル住所を表示
                                viewModel.address.value = place.workingPlace + place.workingBuilding
                            } else {
                                // 現ユーザーが未応募の物件の場合　短縮住所を表示
                                viewModel.address.value = place.workingPlaceShort
                            }
                        }
                    }
                }
            }
        } else {
            Log.v("DEBUG","ここ通ったらダメ")
            Log.v("DEBUG","path=${intent.data?.path}")
            Log.v("DEBUG","placeId~${placeId}")
            // 物件の注意事項を取得
            placeId?.let { place_id ->
                if (jobId != null || placeId != null) {
                    api.placeCautions(jobId, placeId, jobKindId) {
                        //ボタンのラベルを生成しセット
                        cautions = it
                        propertyNotesAdapter.cautions = it
                        propertyNotesAdapter.notifyDataSetChanged()
                    }
                }
            }
        }

        val binding: ActivityPropertyNotesBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_property_notes)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        //RecyclerView の初期化を行います
        displayPropertyNotesItem()
    }

    override fun onResume() {
        super.onResume()
        if (placeId != null && placeId != 0){
            api.place(placeId!!) { place ->
                if (place.hasEntries || place.workingPlaceShort.isNullOrEmpty()) {
                    // 現ユーザーが応募済の物件の場合　フル住所を表示
                    viewModel.address.value = place.workingPlace + place.workingBuilding
                } else {
                    // 現ユーザーが未応募の物件の場合　短縮住所を表示
                    viewModel.address.value = place.workingPlaceShort
                }
            }
        }
        if ((jobId != null && jobId != 0) || (placeId != null && placeId != 0)) {
            api.placeCautions(jobId, placeId, jobKindId) {
                //ボタンのラベルを生成しセット
                cautions = it
                propertyNotesAdapter.cautions = it
                propertyNotesAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onClickShowOtherFAQ(view: View) {
        //よくある質問
        val frequentlyQuestionsURLString = ErikuraConfig.frequentlyQuestionsURLString
        Uri.parse(frequentlyQuestionsURLString)?.let { uri ->
            try {
                Intent(Intent.ACTION_VIEW, uri).let { intent ->
                    intent.setPackage("com.android.chrome")
                    startActivity(intent)
                }
            } catch (e: ActivityNotFoundException) {
                Intent(Intent.ACTION_VIEW, uri).let { intent ->
                    startActivity(intent)
                }
            }
        }
    }

    private fun displayPropertyNotesItem() {

        val recyclerView: RecyclerView = findViewById((R.id.property_notes_list))
        // リサイクラービューのサイズ固定なし　画像サイズが可変
        recyclerView.setHasFixedSize(false)
        //レイアウトマネージャの設定
        val manager = LinearLayoutManager(this)
        // 縦スクロールのリスト
        manager.orientation = RecyclerView.VERTICAL
        recyclerView.layoutManager = manager
        //アダプター取得　レイアウトとデータ関連付けさせるため
        propertyNotesAdapter = PropertyNotesAdapter(
            this,
            cautions,
            (this.resources.getDimension(R.dimen.Property_notes_item_margin)).toInt()
        )
        // アダプターをRecyclerViewにセット
        recyclerView.adapter = propertyNotesAdapter
        // アイテム間の幅をセットします
        recyclerView.addItemDecoration(PropertyNotesItemDecorator())
    }

    private fun handleIntent(intent: Intent): Int {
        val appLinkData: Uri? = intent.data
        // FDLで遷移した場合、空にセットしておく
        ErikuraApplication.instance.pushUri = null
        return appLinkData!!.lastPathSegment!!.toInt()
    }
}

class PropertyNotesViewModel : ViewModel() {
    var address: MutableLiveData<String> = MutableLiveData()
}

interface PropertyNotesEventHandlers {
    fun onClickShowOtherFAQ(view: View)
}


class PropertyNotesViewHolder(val binding: FragmentPropertyNotesItemBinding) :
    RecyclerView.ViewHolder(binding.root)

class PropertyNotesItemDecorator : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.top = view.resources.getDimensionPixelSize(R.dimen.Property_notes_item_margin)
    }
}

class PropertyNotesAdapter(
    val activity: FragmentActivity,
    var cautions: List<Caution>,
    val margin: Int
) : RecyclerView.Adapter<PropertyNotesViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyNotesViewHolder {
        //表示するレイアウトを設定
        val binding = DataBindingUtil.inflate<FragmentPropertyNotesItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.fragment_property_notes_item,
            parent,
            false
        )
        return PropertyNotesViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return cautions.count()
    }

    override fun onBindViewHolder(holder: PropertyNotesViewHolder, position: Int) {
        // ビューホルダーに値を割り当てて、個々のリスト項目を生成
        holder.binding.lifecycleOwner = activity
        holder.binding.viewModel = PropertyNotesItemViewModel(cautions[position])
        //1行分のデータを受け取り１行分のデータをセットする データ表示
        val caution = cautions[position]
        val files: List<CautionFile> = caution.files
        //ListViewで実装しようとしたが高さが適性値を取得できないため、addViewで実装　
        // ListViewについてはコメントアウトで残してます。
        val linearLayout: LinearLayout = holder.itemView.findViewById(R.id.property_notes_image_pdf)
        linearLayout.removeAllViewsInLayout()
        val layout = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layout.setMargins(margin, margin, margin, margin)
        if (files.isNotEmpty()) {
            for (i in 0 until files.size) {
                if (files[i].file_name.endsWith(".pdf")) {
                    val imageView = ImageView(activity)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    imageView.layoutParams = lp
                    imageView.adjustViewBounds = true
                    imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    val assetsManager = ErikuraApplication.assetsManager
                    assetsManager.fetchAsset(activity, files[i].thumbnail_url) { asset ->
                        Glide.with(activity).load(File(asset.path)).into(imageView)
                    }

                    imageView.setOnClickListener {
                        val itemUrl: String = files[i].url

                        assetsManager.fetchAsset(activity, itemUrl, Asset.AssetType.Pdf) { asset ->
                            // PDFディレクトリにコピーします
                            val filesDir = activity.filesDir
                            val pdfDir = File(filesDir, "pdfs")
                            if (!pdfDir.exists()) {
                                pdfDir.mkdirs()
                            }
                            val pdfFile = File(pdfDir, files[i].file_name)
                            val out = FileOutputStream(pdfFile)
                            val input = FileInputStream(File(asset.path))
                            IOUtils.copy(input, out)
                            out.closeQuietly()
                            input.closeQuietly()

                            val uri = FileProvider.getUriForFile(
                                activity!!,
                                BuildConfig.APPLICATION_ID + ".fileprovider",
                                pdfFile
                            )
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(
                                uri,
                                MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf")
                            )
                            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            activity.startActivity(intent)
                        }
                    }
                    linearLayout.addView(imageView, layout)
                } else {
                    val imageView = ImageView(activity)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    imageView.layoutParams = lp
                    imageView.adjustViewBounds = true
                    imageView.scaleType = ImageView.ScaleType.FIT_CENTER

                    val assetsManager = ErikuraApplication.assetsManager
                    assetsManager.fetchAsset(activity, files[i].url) { asset ->
                        Glide.with(activity).load(File(asset.path)).into(imageView)
                    }
                    linearLayout.addView(imageView, layout)
                    imageView.setOnClickListener {
                        val itemUrl: String = files[i].url
                        assetsManager.fetchAsset(
                            activity,
                            itemUrl,
                            Asset.AssetType.Other
                        ) { asset ->
                            val intent = Intent(activity, WebViewActivity::class.java).apply {
                                action = Intent.ACTION_VIEW
                                data = Uri.parse("file://" + asset.path)
                            }
                            activity.startActivity(
                                intent,
                                ActivityOptions.makeSceneTransitionAnimation(activity).toBundle()
                            )
                        }
                    }
                }
            }
        }
    }
}

class PropertyNotesItemViewModel(
    var caution: Caution
) : ViewModel() {
    var question: MutableLiveData<String> = MutableLiveData()
    var answer: MutableLiveData<String> = MutableLiveData()

    init {
        question.value = "Q. ".plus(caution.question)
        answer.value = "A. ".plus(caution.answer)
    }
}
