package jp.co.recruit.erikura.presenters.activities.mypage

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.RequiredClientVersion
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.*
import kotlinx.android.synthetic.main.activity_about_app.*
import kotlin.collections.ArrayList
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import jp.co.recruit.erikura.presenters.activities.WebViewActivity

class AboutAppActivity : AppCompatActivity(),
    AboutAppEventHandlers {
    data class MenuItem(val id: Int, val label: String, val onSelect: () -> Unit)

    var user: User = User()
    var virsion: RequiredClientVersion? = null

    private val viewModel: AboutAppViewModel by lazy {
        ViewModelProvider(this).get(AboutAppViewModel::class.java)
    }

    var menuItems: ArrayList<MenuItem> = arrayListOf(
        MenuItem(0, "利用規約") {
            val termsOfServiceURLString = BuildConfig.SERVER_BASE_URL + "/pdf/terms_of_service.pdf"
            val intent = Intent(this, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(termsOfServiceURLString)
            }
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(1, "プライバシーポリシー"
        ) {
            val privacyPolicyURLString = BuildConfig.SERVER_BASE_URL + "/pdf/privacy_policy.pdf"
            val intent = Intent(this, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(privacyPolicyURLString)
            }
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(2, "推奨環境") {
            // FIXME: 正しいURLか確認
            val privacyPolicyURLString =
                "https://faq.erikura.net/hc/ja/articles/360020286793-%E3%82%B5%E3%82%A4%E3%83%88%E3%81%AE%E6%8E%A8%E5%A5%A8%E7%92%B0%E5%A2%83%E3%82%92%E6%95%99%E3%81%88%E3%81%A6%E3%81%8F%E3%81%A0%E3%81%95%E3%81%84"
            val intent = Intent(this, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(privacyPolicyURLString)
            }
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(3, "ライセンス") {
            val intent = Intent(this, OssLicensesMenuActivity::class.java)
            startActivity(intent)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        val binding: ActivityAboutAppBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_about_app)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        val adapter = AboutAppAdapter(menuItems)
        adapter.setOnItemClickListener(object :
            AboutAppAdapter.OnItemClickListener {
            override fun onItemClickListener(item: MenuItem) {
                item.onSelect()
            }
        })

        about_app_recycler_view.adapter = adapter
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        about_app_recycler_view.addItemDecoration(itemDecoration)

        // ユーザーのOSバージョンを取得
        Api(this).clientVersion(){
            // FIXME: RequiredCilentVirsionを実装後挙動確認(xmlも書き換え)
            virsion = it
            viewModel.current.value = virsion!!.current
            viewModel.minimum.value = virsion!!.minimum
        }
    }

    class AboutAppAdapter(private val menuItems: List<MenuItem>) :
        RecyclerView.Adapter<AboutAppAdapter.ViewHolder>() {
        class ViewHolder(val binding: FragmentAboutAppCellBinding) :
            RecyclerView.ViewHolder(binding.root)

        var listener: OnItemClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = DataBindingUtil.inflate<FragmentAboutAppCellBinding>(
                LayoutInflater.from(parent.context),
                R.layout.fragment_about_app_cell,
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val MenuListItem = menuItems.get(position)
            val viewModel =
                AboutAppMenuItemViewModel(MenuListItem)
            holder.binding.viewModel = viewModel

            holder.binding.root.setOnClickListener {
                listener?.onItemClickListener(menuItems[position])
            }
        }
        interface OnItemClickListener {
            fun onItemClickListener(item: MenuItem)
        }

        fun setOnItemClickListener(listener: OnItemClickListener) {
            this.listener = listener
        }

        override fun getItemCount() = menuItems.size
    }
}

interface AboutAppEventHandlers {
}

class AboutAppViewModel: ViewModel() {
    val current: MutableLiveData<String> = MutableLiveData()
    val minimum: MutableLiveData<String> = MutableLiveData()
}

class AboutAppMenuItemViewModel(val item: AboutAppActivity.MenuItem) : ViewModel() {
}