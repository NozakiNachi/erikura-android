package jp.co.recruit.erikura.presenters.activities.job

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Caution
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityPropertyNotesBinding
import jp.co.recruit.erikura.databinding.FragmentPropertyNotesButtonBinding
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
    //recyclerviewをviewをidで渡す
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
        //1行分のデータを受け取り１行分のデータをセットする データ表示
        val questionTextView: TextView = holder.binding.root.findViewById(R.id.question)
        val answerTextView: TextView = holder.binding.root.findViewById(R.id.answer)
        val caution = cautions[position]
        questionTextView.setText(caution.question)
        answerTextView.setText(caution.answer)
        var files: List<String> = caution.files
        // FIXME pdf 画像用のrecyclerViewを呼び出す

        holder.binding.lifecycleOwner = activity
    }
}