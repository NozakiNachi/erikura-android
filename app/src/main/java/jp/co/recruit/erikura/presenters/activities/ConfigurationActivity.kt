package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Information
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.presenters.activities.job.*
import jp.co.recruit.erikura.presenters.activities.job.JobListAdapter
import jp.co.recruit.erikura.presenters.activities.job.JobListHolder
import jp.co.recruit.erikura.presenters.util.MessageUtils
import jp.co.recruit.erikura.presenters.view_models.JobListItemViewModel
import kotlinx.android.synthetic.main.activity_configuration.*
import java.util.*
import kotlin.collections.ArrayList
import android.view.MenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import jp.co.recruit.erikura.business.models.OwnJobQuery
import jp.co.recruit.erikura.business.util.DateUtils
import jp.co.recruit.erikura.databinding.*
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import java.util.*


class ConfigurationActivity : AppCompatActivity(), ConfigurationEventHandlers {

    var user: User = User()

    private lateinit var configurationListView: RecyclerView
    private lateinit var configurationListAdapter: InformationAdapter

    private val viewModel: ConfigurationViewModel by lazy {
        ViewModelProvider(this).get(ConfigurationViewModel::class.java)
    }

    var configurationTextList: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        val binding: ActivityConfigurationBinding = DataBindingUtil.setContentView(this, R.layout.activity_configuration)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this


//        // 設定画面のメニューをrecycler_viewで表示
//        addConfiguration()
//        configuration_recycler_view.layoutManager = LinearLayoutManager(this)
//        configuration_recycler_view.adapter = ConfigurationAdapter(configurationTextList)
//    }
//    fun addConfiguration() {
//        configurationTextList.add("configuration1")
//        configurationTextList.add("configuration2")
//        configurationTextList.add("configuration3")
//    }
        configurationListAdapter = InformationAdapter(this)
        configurationListView = findViewById(R.id.configuration_recycler_view)
        configurationListView.adapter = configurationListAdapter
        configurationListView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

//    class ConfigurationAdapter(private val myDataset: ArrayList<String>) : RecyclerView.Adapter<ConfigurationAdapter.ViewHolder>()
//    {
//        class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//            val textView = LayoutInflater.from(parent.context)
//                .inflate(R.layout.fragment_configuration_cell, parent, false) as TextView
//            return ViewHolder(textView)
//        }
//        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//            holder.textView.text = myDataset[position]
//        }
//        override fun getItemCount() = myDataset.size
//    }


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
        AlertDialog.Builder(this) // FragmentではActivityを取得して生成
            .setView(R.layout.dialog_logout)
            .setPositiveButton("OK",  { dialog, which ->
                // ログアウト処理
                Api(this).logout() {
                    // スタート画面に戻る
                    val intent = Intent(this, StartActivity::class.java)
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                }
            })
            .show()
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

class ConfigurationViewModel: ViewModel() {}

class ConfigurationAdapter(val activity: FragmentActivity) : RecyclerView.Adapter<ConfigurationCellHolder>() {
    var configuration: List<Information> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfigurationCellHolder {
        val binding: FragmentConfigurationCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context), R.layout.fragment_configuration_cell, parent, false
        )
        return ConfigurationCellHolder(binding)
    }

    override fun getItemCount(): Int {
        return configuration.count()
    }

    override fun onBindViewHolder(holder: ConfigurationCellHolder, position: Int) {
        val configuration = configuration[position]!!
        val viewModel = ConfigurationCellViewModel(configuration)

        holder.binding.lifecycleOwner = activity
        holder.binding.viewModel = viewModel

        // WebView にコンテンツを設定します
        val webView = holder.binding.configurationCellWebview
        webView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                val dp = activity.resources.displayMetrics
                webView.layoutParams.let { lp ->
                    lp.height = (webView.contentHeight * dp.scaledDensity).toInt()
                    webView.layoutParams = lp
                }
            }
        }
        val encodedHtml = Base64.encodeToString(configuration.content.toByteArray(), Base64.NO_PADDING)
        webView.loadData(encodedHtml, "text/html", "base64")
        // FIXME: WebView, Layout の高さ調整
    }
}

class ConfigurationCellHolder(val binding: FragmentConfigurationCellBinding): RecyclerView.ViewHolder(binding.root)

class ConfigurationCellViewModel(val configuration: Information): ViewModel() {
    val lastUpdated: String get() {
        val now = Date()
        val diff = now.time - configuration.createdAt.time
        val diffMinutes = diff / (60 * 1000)
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24

        return if (diffDays > 0) {
            String.format("%,d日前", diffDays)
        } else if (diffHours > 0) {
            String.format("%,d時間前", diffHours)
        } else {
            "1時間以内"
        }
    }
}