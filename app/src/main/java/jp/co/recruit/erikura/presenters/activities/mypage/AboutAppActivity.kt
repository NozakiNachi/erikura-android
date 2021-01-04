package jp.co.recruit.erikura.presenters.activities.mypage

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.ErikuraConfig
import jp.co.recruit.erikura.business.models.RequiredClientVersion
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.ActivityAboutAppBinding
import jp.co.recruit.erikura.databinding.FragmentAboutAppCellBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
import jp.co.recruit.erikura.presenters.util.setOnSafeClickListener
import kotlinx.android.synthetic.main.activity_about_app.*

class AboutAppActivity : BaseActivity(), AboutAppEventHandlers {
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
            startActivity(intent)
        },
        MenuItem(1, "プライバシーポリシー"
        ) {
            val privacyPolicyURLString = BuildConfig.SERVER_BASE_URL + "/pdf/privacy_policy.pdf"
            val intent = Intent(this, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(privacyPolicyURLString)
            }
            startActivity(intent)
        },
        MenuItem(2, "推奨環境") {
            val recommendedEnvironmentURLString = ErikuraConfig.recommendedEnvironmentURLString
            Uri.parse(recommendedEnvironmentURLString)?.let { uri ->
                try {
                    Intent(Intent.ACTION_VIEW, uri).let { intent ->
                        intent.setPackage("com.android.chrome")
                        startActivity(intent)
                    }
                }
                catch (e: ActivityNotFoundException) {
                    Intent(Intent.ACTION_VIEW, uri).let { intent ->
                        startActivity(intent)
                    }
                }
            }
        },
        MenuItem(3, "ライセンス") {
            // FIXME: トラッキングタグの記録はできない
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
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_about", params= bundleOf())
        Tracking.view(name= "/mypage/about", title= "このアプリについて画面")
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

            holder.binding.root.setOnSafeClickListener {
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
    val versionName: String get() = ErikuraApplication.versionName
    val version: String get() {
        return String.format("バージョン %s", versionName)
    }
}

class AboutAppMenuItemViewModel(val item: AboutAppActivity.MenuItem) : ViewModel() {
}