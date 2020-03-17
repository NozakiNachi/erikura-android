package jp.co.recruit.erikura.presenters.activities

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.app.AlertDialog
import android.app.Service
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.IBinder
import android.system.Os.remove
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.internal.util.HalfSerializer.onComplete
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.network.Api.Companion.userSession
import jp.co.recruit.erikura.databinding.*
import kotlinx.android.synthetic.main.activity_configuration.*
import jp.co.recruit.erikura.presenters.activities.registration.RegisterEmailActivity
import jp.co.recruit.erikura.presenters.fragments.ErikuraMarkerView
import kotlinx.android.synthetic.main.activity_mypage.*
import java.util.AbstractList
import java.util.regex.Pattern


class ConfigurationActivity : AppCompatActivity(), ConfigurationEventHandlers {
    data class MenuItem(val id: Int, val label: String, val iconDrawableId: Int, var requireLogin: Boolean, var display_judge_id: Int?, val onSelect: () -> Unit)

    var user: User = User()
    var menuSize: Int =0
    var fromChangeUserInformationFragment: Boolean = false
    var fromChangeAccountFragment: Boolean = false
    var fromRegisterAccountFragment: Boolean = false

    private val viewModel: ConfigurationViewModel by lazy {
        ViewModelProvider(this).get(ConfigurationViewModel::class.java)
    }

    // FIXME: 正しいリンク先の作成
    var menuItems: ArrayList<MenuItem> = arrayListOf(
        MenuItem(0, "会員情報変更", R.drawable.icon_man_15, true, 0) {
            val intent = Intent(this, RecertificationActivity::class.java)
            intent.putExtra("onClickChangeUserInformation", true)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(1, "口座情報登録・変更", R.drawable.icon_card_15, true, 1) {
            val intent = Intent(this, RecertificationActivity::class.java)
            intent.putExtra("onClickAccountSetting", true)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(2, "通知設定", R.drawable.icon_slide_15, true, 2) {
            val intent = Intent(this, NotificationSettingActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(3, "このアプリについて", R.drawable.icon_smartphone_15, false, 3) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(4, "よくある質問", R.drawable.icon_hatena_15, false, 4) {
            val intent = Intent(this, ConfigurationActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(5, "問い合わせ", R.drawable.icon_mail_15, false, 5) {
            val intent = Intent(this, RegisterEmailActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MenuItem(6, "ログアウト", R.drawable.icon_exit_15, true, 6) {
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



// FIXME: 非ログイン時の挙動実装途中
        // 設定画面のメニューをrecycler_viewで表示
        if (userSession == null) {
//            if (menuItems[0].requireLogin == true) {
//                menuItems.remove(menuItems[0])
//            }
//            if (menuItems[1].requireLogin == true) {
//                menuItems.remove(menuItems[1])
//            }
//            if (menuItems[2].requireLogin == true) {
//                menuItems.remove(menuItems[2])
//            }
//            if (menuItems[3].requireLogin == true) {
//                menuItems.remove(menuItems[3])
//            }
//            if (menuItems[4].requireLogin == true) {
//                menuItems.remove(menuItems[4])
//            }
//            if (menuItems[5].requireLogin == true) {
//                menuItems.remove(menuItems[5])
//            }
//            if (menuItems[6].requireLogin == true) {
//                menuItems.remove(menuItems[6])
//            }
//            if (menuItems[7].requireLogin == true) {
//                menuItems.remove(menuItems[7])
//            }
            for (i in menuItems) {
                if (i.requireLogin == true) {
                    menuSize++
                }
            }
        }

        val adapter = ConfigurationAdapter(menuItems, menuSize)
        adapter.setOnItemClickListener(object : ConfigurationAdapter.OnItemClickListener {
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

    class ConfigurationAdapter(private val menuItems: List<MenuItem>, private val menuSize:Int) : RecyclerView.Adapter<ConfigurationAdapter.ViewHolder>()
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

            if(userSession == null && menuItems[position].requireLogin == true){
                holder.binding.configurationCell.setVisibility(View.GONE)
            }

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
        }else if(fromRegisterAccountFragment){
            val binding: DialogRegisterAccountSuccessBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.dialog_register_account_success, null, false)
            binding.lifecycleOwner = this

            AlertDialog.Builder(this)
                .setView(binding.root)
                .show()
        }else if(fromChangeAccountFragment){
            val binding: DialogChangeAccountSuccessBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.dialog_change_account_success, null, false)
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