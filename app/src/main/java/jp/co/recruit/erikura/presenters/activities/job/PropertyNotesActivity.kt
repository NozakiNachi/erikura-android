package jp.co.recruit.erikura.presenters.activities.job

import android.app.Activity
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.ErikuraApplication.Companion.applicationContext
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Caution
import jp.co.recruit.erikura.business.models.CautionFile
import jp.co.recruit.erikura.business.models.ErikuraConfig
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityPropertyNotesBinding
import jp.co.recruit.erikura.databinding.FragmentPropertyNotesItemBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.WebViewActivity

class PropertyNotesActivity : BaseActivity(), PropertyNotesEventHandlers {
    private val viewModel: PropertyNotesViewModel by lazy {
        ViewModelProvider(this).get(PropertyNotesViewModel::class.java)
    }
    private var cautions: List<Caution> = listOf()
    private var placeId: Int? = null

    private lateinit var propertyNotesAdapter: PropertyNotesAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        placeId = intent.getIntExtra("place_id", 0)
        // 物件の注意事項を取得
        placeId?.let { place_id ->
            Api(this).placeCautions(place_id) {
                //ボタンのラベルを生成しセット
                cautions = it
                propertyNotesAdapter.cautions = it
                propertyNotesAdapter.notifyDataSetChanged()
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
        placeId?.let {
            Api(this).place(it) { place ->
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
        propertyNotesAdapter = PropertyNotesAdapter(this, cautions, (this.resources.getDimension(R.dimen.Property_notes_item_margin)).toInt())
        // アダプターをRecyclerViewにセット
        recyclerView.adapter = propertyNotesAdapter
        // アイテム間の幅をセットします
        recyclerView.addItemDecoration(PropertyNotesItemDecorator())
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
        val linearLayout :LinearLayout = holder.itemView.findViewById(R.id.property_notes_image_pdf)
        val layout = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layout.setMargins(margin, margin, margin, margin)
        if (files.isNotEmpty()) {
            for (i in 0 until files.size) {
                if (files[i].url.endsWith(".pdf")){
                    val imageView = ImageView(activity)
                    val assetsManager = ErikuraApplication.assetsManager
                    assetsManager.fetchImage(activity, files[i].thumbnail_url){
                        imageView.setImageBitmap(it)
                    }
                    imageView.setOnClickListener {
                        val itemUrl: String = files[i].url
                        val intent = Intent(activity, WebViewActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            data = Uri.parse(itemUrl)
                        }
                        activity.startActivity(intent,
                            ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
                    }
                    linearLayout.addView(imageView, layout)
                } else {
                    val imageView = ImageView(activity)
                    val assetsManager = ErikuraApplication.assetsManager
                    assetsManager.fetchImage(activity, files[i].url){
                        imageView.setImageBitmap(it)
                    }
                    linearLayout.addView(imageView, layout)
                    imageView.setOnClickListener{
                        val itemUrl: String = files[i].url
                        val intent = Intent(activity, WebViewActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            data = Uri.parse(itemUrl)
                        }
                        activity.startActivity(intent,
                            ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
                    }
                }
            }

//            val propertyNotesItemFileView: ListView =
//                holder.binding.root.findViewById(R.id.property_notes_file_list)
//            val propertyNotesItemFileAdapter = PropertyNotesItemFileAdapter(
//                activity, LayoutInflater.from(applicationContext),
//                files as ArrayList<CautionFile>
//            )
//            propertyNotesItemFileView.adapter = propertyNotesItemFileAdapter
//            setListViewHeightBasedOnChildren(propertyNotesItemFileView, position)
//            propertyNotesItemFileAdapter.notifyDataSetChanged()
//            propertyNotesItemFileView.setOnItemClickListener { parent, view, position, id ->
//                // listViewのクリックされた行のテキストを取得
//                val itemUrl: String = files[position].url
//                val intent = Intent(activity, WebViewActivity::class.java).apply {
//                    action = Intent.ACTION_VIEW
//                    data = Uri.parse(itemUrl)
//                }
//                activity.startActivity(intent)
//            }
        }
    }

//    private fun setListViewHeightBasedOnChildren(listView: ListView, position: Int) {
//
//        //ListAdapterを取得
//        val listAdapter = listView.getAdapter()
//        val displayMetrics = ErikuraApplication.applicationContext.resources.displayMetrics
//        if (listAdapter == null) {
//            return
//        }
//
//        var totalHeight = 0
//
//        //個々のアイテムの高さを測り、加算していく
//        for (i in 0 until listAdapter.getCount()) {
//            val listItem = listAdapter.getView(i, null, listView)
//            listItem.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
//            //各子要素の高さを加算
//            if (this.cautions[position].files[i].url.endsWith(".pdf")) {
//                totalHeight += (listItem.measuredHeight * 0.01 * displayMetrics.density).toInt()
//            } else {
//                totalHeight += (listItem.measuredHeight * 1.2 * displayMetrics.density).toInt()
//            }
//        }
//
//        //LayoutParamsを取得
//        val params = listView.getLayoutParams()
//
//        //(区切り線の高さ * 要素数の数)だけ足す
//        params.height = totalHeight + (listView.getDividerHeight() *
//                (listAdapter.getCount() * displayMetrics.density).toInt())
//        //LayoutParamsにheightをセット
//        listView.setLayoutParams(params)
//    }
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

//class PropertyNotesItemFileAdapter(
//    val activity: FragmentActivity,
//    var inflater: LayoutInflater,
//    val files: ArrayList<CautionFile>
//) : BaseAdapter() {
//    internal data class ViewHolder(val image: ImageView)
//
//    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
//        var view = convertView
//        val viewHolder: ViewHolder
//
//        if (files.get(position).url.isNullOrBlank()) {
//            //何もセットしない
////            viewHolder.image.setImageDrawable(
////                ErikuraApplication.instance.applicationContext.resources.getDrawable(
////                    R.drawable.ic_noimage,
////                    null
////                )
////            )
//        } else {
//            view =
//                inflater.inflate(R.layout.fragment_property_notes_item_file, parent, false)
//            // 画像かpdfで分岐
//            if (files.get(position).url.endsWith(".pdf")) {
//                if (convertView == null) {
//                    view =
//                        inflater.inflate(R.layout.fragment_property_notes_item_file, parent, false)
//                }
//                var textView: TextView = view!!.findViewById(R.id.property_notes_pdf_button)
//                textView.setText(files.get(position).file_name)
//            } else {
//                if (convertView == null) {
//                    view =
//                        inflater.inflate(R.layout.fragment_property_notes_item_file, parent, false)
//                }
//
//                var thumbnailImageView: ImageView =
//                    view!!.findViewById(R.id.property_notes_thumbnailImage_image)
//                //viewのidとurlとactivityを元にサムネイル画像をセットする
//                val assetsManager = ErikuraApplication.assetsManager
//                assetsManager.fetchImage(activity, files.get(position).url, thumbnailImageView)
//            }
//        }
//        return view!!
//    }
//
//    override fun getCount(): Int {
//        return files.count()
//    }
//
//    override fun getItem(position: Int): Int {
//        return position
//    }
//
//    override fun getItemId(position: Int): Long {
//        return position.toLong()
//    }
//}