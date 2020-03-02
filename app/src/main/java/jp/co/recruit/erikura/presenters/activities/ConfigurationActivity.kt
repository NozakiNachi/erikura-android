package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.app.AlertDialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.*
import jp.co.recruit.erikura.presenters.activities.errors.LoginRequiredActivity
import kotlinx.android.synthetic.main.activity_configuration.*
import jp.co.recruit.erikura.presenters.activities.registration.RegisterEmailActivity


class ConfigurationActivity : AppCompatActivity(), ConfigurationEventHandlers {
    data class MenuItem(val id: Int, val label: String, val iconDrawableId: Int, val requireLogin: Boolean, val onSelect: () -> Unit)

    var user: User = User()
    var fromChangeUserInformationFragment: Boolean = false

    private val viewModel: ConfigurationViewModel by lazy {
        ViewModelProvider(this).get(ConfigurationViewModel::class.java)
    }

    // FIXME: 正しいリンク先の作成
    // FIXME: アイコンのファイル名書き換え
    var menuItems: List<MenuItem> = listOf(
        MenuItem(0, "会員情報変更", R.drawable.icon_man_15, true) {
            val intent = Intent(this, ChangeUserInformationActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(1, "口座情報登録・変更", R.drawable.icon_card_15, true) {
            val intent = Intent(this, AccountSettingActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(2, "通知設定", R.drawable.icon_slide_15, true) {
            val intent = Intent(this, RegisterEmailActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(3, "このアプリについて", R.drawable.icon_smartphone_15, true) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(4, "よくある質問", R.drawable.icon_hatena_15, true) {
            val intent = Intent(this, ConfigurationActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(5, "問い合わせ", R.drawable.icon_mail_15, true) {
            val intent = Intent(this, RegisterEmailActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(6, "ログアウト", R.drawable.icon_exit_15, true) {
            onClickLogoutLink()
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)


        val binding: ActivityConfigurationBinding = DataBindingUtil.setContentView(this, R.layout.activity_configuration)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        // 設定画面のメニューをrecycler_viewで表示
        val adapter = ConfigurationAdapter(menuItems)
        adapter.setOnItemClickListener(object:ConfigurationAdapter.OnItemClickListener{
            override fun onItemClickListener(item: MenuItem) {
                item.onSelect()
            }
        })
        configuration_recycler_view.adapter = adapter

        fromChangeUserInformationFragment = intent.getBooleanExtra("onClickChangeUserInformationFragment", false)
    }

    class ConfigurationAdapter(private val menuItems: List<MenuItem>) : RecyclerView.Adapter<ConfigurationAdapter.ViewHolder>()
    {
        class ViewHolder(val binding: FragmentConfigurationCellBinding) : RecyclerView.ViewHolder(binding.root)
        var listener: OnItemClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = DataBindingUtil.inflate<FragmentConfigurationCellBinding>(
                LayoutInflater.from(parent.context),
                R.layout.fragment_configuration_cell,
                parent,
                false
            )
            return ViewHolder(binding)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val MenuListItem = menuItems.get(position)
            val viewModel = ConfigurationMenuItemViewModel(MenuListItem)
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

    //
    override fun onStart() {
        super.onStart()
        if(fromChangeUserInformationFragment) {
            val binding: DialogChangeUserInformationSuccessBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.dialog_change_user_information_success, null, false)
            binding.lifecycleOwner = this

            AlertDialog.Builder(this)
                .setView(binding.root)
                .show()
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
}

interface ConfigurationEventHandlers {
    // ログアウトへのリンク
    fun onClickLogoutLink()
    // ログアウト処理
    fun onClickLogout(view: View)
}

class ConfigurationViewModel: ViewModel() {
}

class ConfigurationMenuItemViewModel(val item: ConfigurationActivity.MenuItem) : ViewModel() {
    val icon: Drawable get() = ErikuraApplication.applicationContext.resources.getDrawable(item.iconDrawableId, null)
}