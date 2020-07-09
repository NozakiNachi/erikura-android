package jp.co.recruit.erikura.presenters.activities.job

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

class PropertyNotesActivity : BaseActivity(), PropertyNotesEventHandlers {
    private val viewModel: PropertyNotesViewModel by lazy {
        ViewModelProvider(this).get(PropertyNotesViewModel::class.java)
    }
    private var cautions: List<Caution> = listOf()

    private lateinit var propertyNotesAdapter: PropertyNotesAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var placeId: Int = intent.getIntExtra("place_id", 0)
        // 物件の注意事項を取得
        placeId?.let {
            Api(this).placeCautions(it) {
                //ボタンのラベルを生成しセット
                cautions = it
                propertyNotesAdapter.cautions = it
                propertyNotesAdapter.notifyDataSetChanged()
            }
            Api(this).place(placeId) { place ->
                if (place.hasEntries) {
                    // 現ユーザーが応募済の物件の場合　フル住所を表示
                    viewModel.address.value = place.workingPlace
                } else {
                    // 現ユーザーが未応募の物件の場合　短縮住所を表示
                    viewModel.address.value = place.workingPlaceShort
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
        propertyNotesAdapter = PropertyNotesAdapter(this, cautions)
        // アダプターをRecyclerViewにセット
        recyclerView.adapter = propertyNotesAdapter
        // アイテム間の幅をセットします
        recyclerView.addItemDecoration(PropertyNotesItemDecorator())
    }


}

class PropertyNotesViewModel : ViewModel() {
    val bitmap: MutableLiveData<Bitmap> = MutableLiveData()
    var address: MutableLiveData<String> = MutableLiveData()

    fun setup(activity: Activity, root: View, file: CautionFile?) {
        if (file != null){
            // ダウンロード
            val imageView: ImageView = root.findViewById(R.id.property_notes_thumbnailImage_image)
            val thumbnailUrl = if (!file.url.isNullOrBlank()) {file.url} else {null}
            if (thumbnailUrl.isNullOrBlank()) {
                imageView.setImageDrawable(ErikuraApplication.instance.applicationContext.resources.getDrawable(R.drawable.ic_noimage, null))
            }else {
                val assetsManager = ErikuraApplication.assetsManager
                assetsManager.fetchImage(activity, thumbnailUrl, imageView)
            }
        }
    }
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
    var cautions: List<Caution>
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
        //1行分のデータを受け取り１行分のデータをセットする データ表示
        val questionTextView: TextView = holder.binding.root.findViewById(R.id.question)
        val answerTextView: TextView = holder.binding.root.findViewById(R.id.answer)
        val caution = cautions[position]
        questionTextView.setText("Q.".plus(caution.question))
        answerTextView.setText("A.".plus(caution.answer))
        var files: List<CautionFile> = caution.files

        // FIXME pdf 画像用のrecyclerViewを呼び出す
        if (files.isNotEmpty()) {
            val propertyNotesItemFileView: ListView =
                holder.binding.root.findViewById(R.id.property_notes_file_list)
            val propertyNotesItemFileAdapter = PropertyNotesItemFileAdapter(
                activity, LayoutInflater.from(applicationContext),
                files as ArrayList<CautionFile>
            )
            propertyNotesItemFileView.adapter = propertyNotesItemFileAdapter
            propertyNotesItemFileAdapter.notifyDataSetChanged()
            propertyNotesItemFileView.setOnItemClickListener {parent, view, position, id ->
                // listViewのクリックされた行のテキストを取得
                val itemUrl : String = files[position].url
                // 画像かpdfで分岐
                //　下記のコメントは削除
                //またアプリ側の画像拡大表示の実装ですが、他のモーダルと同様に周りをタップすることで閉じるという動作でよろしいでしょうか。
                //こちらですが、PDFがあることも考えると、
                // モーダルではなくマニュアルのように下から出てくるダイアログがいいと思います。閉じる時はバツボタン。
                if (itemUrl.endsWith(".pdf")) {
                    //マニュアル表示を元にpdfを表示
//                    pdfの場合リンクを表示しクリックでマニュアルボタンと同じように表示させる
                }
                else {
                    //画像の場合
                    val item = (view.findViewById<TextView>(android.R.id.text1)).text
                    // トーストで表示する
                    Toast.makeText(applicationContext, item, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

class PropertyNotesItemFileAdapter(
    val activity: FragmentActivity,
    var inflater: LayoutInflater,
    val files: ArrayList<CautionFile>
) : BaseAdapter() {
    internal data class ViewHolder(val image: ImageView)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        val viewHolder: ViewHolder

        if (files.get(position).url.isNullOrBlank()) {
            //何もセットしない
//            viewHolder.image.setImageDrawable(
//                ErikuraApplication.instance.applicationContext.resources.getDrawable(
//                    R.drawable.ic_noimage,
//                    null
//                )
//            )
        } else {
            // 画像かpdfで分岐
            if (files.get(position).url.endsWith(".pdf")) {
                if (convertView == null) {
                    view =
                        inflater.inflate(R.layout.fragment_property_notes_item_file, parent, false)
                }
                //pdfはリンクを表示し押下で、別画面に表示される
                var textView: TextView = view!!.findViewById(R.id.property_notes_pdf_button)
                textView.setText(files.get(position).url)
            } else {
                if (convertView == null) {
                    view =
                        inflater.inflate(R.layout.fragment_property_notes_item_file, parent, false)
                }
                Log.v("DEBUG", "files.get(position)があるか${files.get(position)}")
                Log.v("DEBUG", "files.get(position).urlがあるか${files.get(position).url}")


               var thumbnailImageView: ImageView  = view!!.findViewById(R.id.property_notes_thumbnailImage_image)
                //viewのidとurlとactivityを元にサムネイル画像をセットする
                val assetsManager = ErikuraApplication.assetsManager
                assetsManager.fetchImage(activity, files.get(position).url, thumbnailImageView)
            }
        }
        return view!!
    }
    //画像の場合サムネで表示しクリックで拡大

    override fun getCount(): Int {
        return files.count()
    }

    override fun getItem(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}