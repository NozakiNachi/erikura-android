package jp.co.recruit.erikura.presenters.activities.mypage

import android.app.ActivityOptions
import android.content.Intent
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.network.Api.Companion.userSession
import jp.co.recruit.erikura.databinding.*
import jp.co.recruit.erikura.presenters.activities.registration.NotificationSettingActivity
import jp.co.recruit.erikura.presenters.activities.StartActivity
import jp.co.recruit.erikura.presenters.activities.WebViewActivity
import jp.co.recruit.erikura.presenters.activities.job.*
import kotlinx.android.synthetic.main.activity_configuration.*
import jp.co.recruit.erikura.presenters.activities.registration.RegisterEmailActivity


class ConfigurationActivity : AppCompatActivity(), ConfigurationEventHandlers {
    data class MenuItem(val id: Int, val label: String, val iconDrawableId: Int, var requireLogin: Boolean, val onSelect: () -> Unit)

    var user: User = User()
    var fromChangeUserInformationFragment: Boolean = false
    var fromChangeAccountFragment: Boolean = false
    var fromRegisterAccountFragment: Boolean = false

    private val viewModel: ConfigurationViewModel by lazy {
        ViewModelProvider(this).get(ConfigurationViewModel::class.java)
    }

    // FIXME: 正しいリンク先の作成
    var menuItems: ArrayList<MenuItem> = arrayListOf(
        MenuItem(0, "会員情報変更", R.drawable.icon_man_15, true) {
            val intent = Intent(this, ChangeUserInformationActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(1, "口座情報登録・変更", R.drawable.icon_card_15, true) {
            val intent = Intent(this, AccountSettingActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(2, "通知設定", R.drawable.icon_slide_15, true) {
            val intent = Intent(this, NotificationSettingActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(3, "このアプリについて", R.drawable.icon_smartphone_15, false) {
            val intent = Intent(this, AboutAppActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(4, "よくある質問", R.drawable.icon_hatena_15, false) {
            val frequentlyQuestionsURLString = "https://faq.erikura.net/hc/ja/sections/360003690953-FAQ"
            val intent = Intent(this, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(frequentlyQuestionsURLString)
            }
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(5, "問い合わせ", R.drawable.icon_mail_15, false) {
            val inquiryURLString = "https://support.erikura.net/"
            val intent = Intent(this, WebViewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(inquiryURLString)
            }
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
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

        val adapter =
            ConfigurationAdapter(menuItems)
        adapter.setOnItemClickListener(object :
            ConfigurationAdapter.OnItemClickListener {
            override fun onItemClickListener(item: MenuItem) {
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
            val viewModel =
                ConfigurationMenuItemViewModel(MenuListItem)
            holder.binding.viewModel = viewModel

            holder.binding.root.setOnClickListener {
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
        binding.logoutButton.setOnClickListener {
            dialog.dismiss()
        }
    }

    // ログアウト処理
    override fun onClickLogout(view: View) {
        Api(this).logout() {
            // スタート画面に戻る
            val intent = Intent(this, StartActivity::class.java)
            // 戻るボタンの無効化
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    // 再認証が必要か確認
    override fun recertification() {
        // スタート画面に戻る
        val intent = Intent(this, StartActivity::class.java)
        // 戻るボタンの無効化
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
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