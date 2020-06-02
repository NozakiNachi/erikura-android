package jp.co.recruit.erikura.presenters.activities.mypage

import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.ErikuraConfig
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.network.Api.Companion.userSession
import jp.co.recruit.erikura.databinding.ActivityConfigurationBinding
import jp.co.recruit.erikura.databinding.DialogLogoutBinding
import jp.co.recruit.erikura.databinding.FragmentConfigurationCellBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.activities.StartActivity
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
import jp.co.recruit.erikura.presenters.activities.job.ChangeAccountSettingFragment
import jp.co.recruit.erikura.presenters.activities.job.ChangeUserInformationFragment
import jp.co.recruit.erikura.presenters.activities.job.RegisterAccountSettingFragment
import jp.co.recruit.erikura.presenters.util.setOnSafeClickListener
import kotlinx.android.synthetic.main.activity_configuration.*


class ConfigurationActivity : BaseActivity(), ConfigurationEventHandlers {
    data class MenuItem(val id: Int, val label: String, val iconDrawableId: Int, var requireLogin: Boolean, val onSelect: () -> Unit)

    var user: User = User()
    var fromChangeUserInformationFragment: Boolean = false
    var fromChangeAccountFragment: Boolean = false
    var fromRegisterAccountFragment: Boolean = false

    private val viewModel: ConfigurationViewModel by lazy {
        ViewModelProvider(this).get(ConfigurationViewModel::class.java)
    }

    var menuItems: ArrayList<MenuItem> = arrayListOf(
        MenuItem(0, "会員情報変更", R.drawable.icon_man_15, true) {
            val intent = Intent(this, ChangeUserInformationActivity::class.java)
            startActivity(intent)
        },
        MenuItem(1, "口座情報登録・変更", R.drawable.icon_card_15, true) {
            val intent = Intent(this, AccountSettingActivity::class.java)
            startActivity(intent)
        },
        MenuItem(2, "通知設定", R.drawable.icon_slide_15, true) {
            val intent = Intent(this, NotificationSettingActivity::class.java)
            startActivity(intent)
        },
        MenuItem(3, "このアプリについて", R.drawable.icon_smartphone_15, false) {
            val intent = Intent(this, AboutAppActivity::class.java)
            startActivity(intent)
        },
        MenuItem(4, "よくある質問", R.drawable.icon_hatena_15, false) {
            val frequentlyQuestionsURLString = ErikuraConfig.frequentlyQuestionsURLString
            Uri.parse(frequentlyQuestionsURLString)?.let { uri ->
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
        MenuItem(5, "問い合わせ", R.drawable.icon_mail_15, false) {
            val inquiryURLString = ErikuraConfig.inquiryURLString
            Uri.parse(inquiryURLString)?.let { uri ->
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
        MenuItem(6, "ログアウト", R.drawable.icon_exit_15, true) {
            onClickLogoutLink()
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        val binding: ActivityConfigurationBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_configuration)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        // 未ログイン時は表示する項目を絞る。
        if (userSession == null) {
            for (items in 0..menuItems.size-1) {
                for (item in 0..menuItems.size-1) {
                    if (menuItems[item].requireLogin) {
                        menuItems.remove(menuItems[item])
                        break
                    }
                }
            }
        }

        val adapter = ConfigurationAdapter(menuItems)
        adapter.setOnItemClickListener(object : ConfigurationAdapter.OnItemClickListener { override fun onItemClickListener(item: MenuItem) {
                item.onSelect()
            }
        })

        configuration_recycler_view.adapter = adapter
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        configuration_recycler_view.addItemDecoration(itemDecoration)

        fromChangeUserInformationFragment = intent.getBooleanExtra("onClickChangeUserInformationFragment", false)
        fromChangeAccountFragment = intent.getBooleanExtra("onClickChangeAccountFragment", false)
        fromRegisterAccountFragment = intent.getBooleanExtra("onClickRegisterAccountFragment", false)
    }

    class ConfigurationAdapter(private val menuItems: List<MenuItem>) : RecyclerView.Adapter<ConfigurationAdapter.ViewHolder>()
    {
        class ViewHolder(val binding: FragmentConfigurationCellBinding) : RecyclerView.ViewHolder(binding.root)
        var listener: OnItemClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = DataBindingUtil.inflate<FragmentConfigurationCellBinding>(
                LayoutInflater.from(parent.context),
                R.layout.fragment_configuration_cell, parent, false)
            return ViewHolder(binding)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val MenuListItem = menuItems.get(position)
            val viewModel = ConfigurationMenuItemViewModel(MenuListItem)
            holder.binding.viewModel = viewModel

            holder.binding.root.setOnSafeClickListener {
                listener?.onItemClickListener(menuItems[position])
            }
        }
        interface OnItemClickListener{
            fun onItemClickListener(item: MenuItem)
        }

        fun setOnItemClickListener(listener: OnItemClickListener){
            this.listener = listener
        }

        override fun getItemCount() = menuItems.size
    }

    override fun onStart() {
        super.onStart()
        if(fromChangeUserInformationFragment) {
            val dialog = ChangeUserInformationFragment()
            dialog.show(supportFragmentManager, "ChangeUserInformation")
            fromChangeUserInformationFragment = false
        }else if(fromRegisterAccountFragment) {
            val dialog = RegisterAccountSettingFragment()
            dialog.show(supportFragmentManager, "RegisterAccountSetting")
            fromRegisterAccountFragment = false
        }else if(fromChangeAccountFragment) {
            val dialog = ChangeAccountSettingFragment()
            dialog.show(supportFragmentManager, "ChangeAccountSetting")
            fromChangeAccountFragment = false
        }
    }

    // ログアウトリンク
    override fun onClickLogoutLink() {
        val binding: DialogLogoutBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.dialog_logout, null, false)
        binding.lifecycleOwner = this
        binding.handlers = this

        val dialog = AlertDialog.Builder(this)
            .setView(binding.root)
            .show()
        binding.logoutButton.setOnSafeClickListener {
            dialog.dismiss()
        }
    }

    // ログアウト処理
    override fun onClickLogout(view: View) {
        Api(this).logout() { deletedSession ->
            // ログアウトのトラッキングの送出
            Tracking.logEvent(event= "logout", params= bundleOf())
            deletedSession?.let {
                it.user?.let { user ->
                    Tracking.identify(user= user, status= "logout")
                }
            }

            // ページ参照のトラッキングの送出
            Tracking.logEvent(event= "view_logout", params= bundleOf())
            Tracking.view(name= "/mypage/logout", title= "ログアウト完了画面")

            // スタート画面に戻る
            val intent = Intent(this, StartActivity::class.java)
            // 戻るボタンの無効化
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    // 再認証が必要か確認
    override fun recertification() {
        // スタート画面に戻る
        val intent = Intent(this, StartActivity::class.java)
        // 戻るボタンの無効化
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}

interface ConfigurationEventHandlers {
    // ログアウトへのリンク
    fun onClickLogoutLink()
    // ログアウト処理
    fun onClickLogout(view: View)
    // 再認証が必要かどうか確認
    fun recertification()
}

class ConfigurationViewModel: ViewModel() {
}

class ConfigurationMenuItemViewModel(val item: ConfigurationActivity.MenuItem) : ViewModel() {
    val icon: Drawable get() = ErikuraApplication.applicationContext.resources.getDrawable(item.iconDrawableId, null)
}