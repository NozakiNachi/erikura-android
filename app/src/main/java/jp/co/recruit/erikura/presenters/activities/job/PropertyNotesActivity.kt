package jp.co.recruit.erikura.presenters.activities.job

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.ErikuraApplication.Companion.applicationContext
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Caution
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

        var placeId: Int = intent.getIntExtra("place_id",0)
        // 物件の注意事項を取得
        placeId?.let {
            Api(this).placeCautions(it) {
                //ボタンのラベルを生成しセット
                cautions = it
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


        propertyNotesAdapter.cautions = cautions

        val binding: ActivityPropertyNotesBinding = DataBindingUtil.setContentView(this, R.layout.activity_property_notes)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this
        //実行結果をitemFragmentに埋め込みviewを作成
        val propertyNotesItemView: RecyclerView = findViewById((R.id.property_notes_list))
        propertyNotesItemView.setHasFixedSize(true)
        propertyNotesItemView.adapter = propertyNotesAdapter
        //viewをセットする
    }


    override fun onClickShowOtherFAQ(view: View) {
        //トップのFAQへ飛ぶ
    }
}

class PropertyNotesViewModel : ViewModel() {
    var address: MutableLiveData<String> = MutableLiveData()
}

interface PropertyNotesEventHandlers {
    fun onClickShowOtherFAQ(view: View)
}


class PropertyNotesViewHolder(val binding: FragmentPropertyNotesItemBinding): RecyclerView.ViewHolder(binding.root)

class PropertyNotesAdapter(
    val activity: FragmentActivity,
    var cautions: List<Caution>
    ): RecyclerView.Adapter<PropertyNotesViewHolder>() {
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
        holder.binding.lifecycleOwner = activity
        //1行分のデータを受け取り１行分のデータをセットする データ表示
        val questionTextView: TextView = holder.binding.root.findViewById(R.id.question)
        val answerTextView: TextView = holder.binding.root.findViewById(R.id.answer)
        val caution = cautions[position]
        questionTextView.setText(caution.question)
        answerTextView.setText(caution.answer)
        var files: List<String> = caution.files
        // FIXME pdf 画像用のrecyclerViewを呼び出す

        val propertyNotesItemFileView: ListView = holder.binding.root.findViewById(R.id.property_notes_file_list)
        val propertyNotesItemFileAdapter = PropertyNotesItemFileAdapter(activity, LayoutInflater.from(applicationContext), files)
        propertyNotesItemFileView.adapter = propertyNotesItemFileAdapter
        propertyNotesItemFileAdapter.notifyDataSetChanged()
    }
}

class PropertyNotesItemFileAdapter(
    val activity: FragmentActivity,
    var inflater: LayoutInflater,
    val files: List<String>) : BaseAdapter() {
    internal data class ViewHolder(val image: ImageView)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        val viewHolder: ViewHolder
        if (convertView == null) {
            view = inflater.inflate(R.layout.fragment_property_notes_item_file, parent)
            viewHolder = ViewHolder(view.findViewById(R.id.file))
            view.tag = viewHolder
        } else {
            viewHolder = view!!.tag as ViewHolder
        }
        if (files[position].isNullOrBlank()) {
            viewHolder.image.setImageDrawable(
                ErikuraApplication.instance.applicationContext.resources.getDrawable(
                    R.drawable.ic_noimage,
                    null
                )
            )
        } else {
            val assetsManager = ErikuraApplication.assetsManager
            assetsManager.fetchImage(activity, files[position], viewHolder.image)
        }
        return view!!
    }

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