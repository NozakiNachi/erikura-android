package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import kotlin.collections.ArrayList
import jp.co.recruit.erikura.databinding.*
import kotlinx.android.synthetic.main.activity_configuration.*
import java.util.*


class ConfigurationActivity : AppCompatActivity(), ConfigurationEventHandlers {
    data class MenuItem(val id: Int, val label: String, val iconDrawableId: Int, val requireLogin: Boolean, val onSelect: () -> Unit)

    var user: User = User()

    private val viewModel: ConfigurationViewModel by lazy {
        ViewModelProvider(this).get(ConfigurationViewModel::class.java)
    }

    var configurationTextList: List<String> = listOf("会員情報変更","口座情報登録・変更","通知設定")
//    var menuItems: List<MenuItem> = listOf(
//        MenuItem(0, "会員情報変更", R.drawable.ic_account, true) { Log.v("TEST", "test") }
//        // 以降メニューを定義する
//    )

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        val binding: ActivityConfigurationBinding = DataBindingUtil.setContentView(this, R.layout.activity_configuration)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        // 設定画面のメニューをrecycler_viewで表示
        configuration_recycler_view.adapter = ConfigurationAdapter(configurationTextList)


    }

    class ConfigurationAdapter(private val configurationDataset: List<String>) : RecyclerView.Adapter<ConfigurationAdapter.ViewHolder>()
    {

        class ViewHolder(val binding: FragmentConfigurationCellBinding) : RecyclerView.ViewHolder(binding.root)

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
            //holder.binding.test.text = configurationDataset[position]
            val viewModel = ConfigurationMenuItemViewModel(MenuItem(0, "test", R.drawable.ic_comment_2x, false) {})
            holder.binding.viewModel = viewModel
//
//            holder.binding.addOnLick.. {
//                item.onSelectd()
//            }
        }
        override fun getItemCount() = configurationDataset.size
    }

    override fun onRegistrationLink(view: View) {
        // FIXME: リンク先の作成
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onAccountRegistration(view: View) {
        // FIXME: リンク先の作成
        val intent = Intent(this, ConfigurationActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onNotificationSettings(view: View) {
        // FIXME: リンク先の作成
        val intent = Intent(this, RegisterEmailActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onAboutTheApp(view: View) {
        // FIXME: リンク先の作成
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onFrequentQuestions(view: View) {
        // FIXME: リンク先の作成
        val intent = Intent(this, ConfigurationActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onInquiry(view: View) {
        // FIXME: リンク先の作成
        val intent = Intent(this, RegisterEmailActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onClickLogoutLink(view: View) {
        val binding: DialogLogoutBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.dialog_logout, null, false)
        binding.lifecycleOwner = this
        binding.handlers = this

        val dialog = AlertDialog.Builder(this) // FragmentではActivityを取得して生成
            .setView(binding.root)
            .show()

        binding.logoutButton.setOnClickListener {
            dialog.dismiss()
            // ログアウト処理
            Api(this).logout() {
                // スタート画面に戻る
                val intent = Intent(this, StartActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
        }
    }
    //
    override fun onClickLogout(view: View) {
        // FIXME: ログアウト処理
        Api(this).logout() {
            // スタート画面に戻る
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
        // 戻るボタンで戻れないようにする。
    }
}

interface ConfigurationEventHandlers {
    // 非ログイン対処
    //fun onClickUnreachLink(view: View)
    // 会員情報変更へのリンク
    fun onRegistrationLink(view: View)
    // 口座情報登録・変更へのリンク
    fun onAccountRegistration(view: View)
    // 通知設定へのリンク
    fun onNotificationSettings(view: View)
    // このアプリについてへのリンク
    fun onAboutTheApp(view: View)
    // よくある質問へのリンク
    fun onFrequentQuestions(view: View)
    // 問い合わせへのリンク
    fun onInquiry(view: View)
    // ログアウトへのリンク
    fun onClickLogoutLink(view: View)
    // ログアウト
    fun onClickLogout(view: View)
}

class ConfigurationViewModel: ViewModel() {

}

class ConfigurationMenuItemViewModel(val item: ConfigurationActivity.MenuItem) : ViewModel() {

}