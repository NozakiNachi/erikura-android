package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityConfigurationBinding
import jp.co.recruit.erikura.databinding.FragmentJobListItemBinding
import jp.co.recruit.erikura.presenters.activities.job.*
import jp.co.recruit.erikura.presenters.activities.job.JobListAdapter
import jp.co.recruit.erikura.presenters.activities.job.JobListHolder
import jp.co.recruit.erikura.presenters.util.MessageUtils
import jp.co.recruit.erikura.presenters.view_models.JobListItemViewModel
import kotlinx.android.synthetic.main.activity_configuration.*


class ConfigurationActivity : AppCompatActivity(), ConfigurationEventHandlers {

    var user: User = User()

    private val viewModel: ConfigurationViewModel by lazy {
        ViewModelProvider(this).get(ConfigurationViewModel::class.java)
    }

    var configurationTextList: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        val binding: ActivityConfigurationBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_configuration)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this


//        // 設定画面のメニューをrecycler_viewで表示
//        addConfiguration()
//        configuration_recycler_view.layoutManager = LinearLayoutManager(this)
//        configuration_recycler_view.adapter = ConfigurationAdapter(configurationTextList)
//
//    }
//
//    fun addConfiguration() {
//        configurationTextList.add("configuration1")
//        configurationTextList.add("configuration2")
//        configurationTextList.add("configuration3")
    }

//    class ConfigurationAdapter(private val myDataset: ArrayList<String>) : RecyclerView.Adapter<ConfigurationAdapter.ViewHolder>()
//    {
//        // RecyclerViewの一要素となるXML要素の型を引数に指定する
//        // この場合はactivity_configuration.xmlのTextView
//        class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//            val textView = LayoutInflater.from(parent.context)
//                .inflate(R.layout.activity_configuration_list_item, parent, false) as TextView
//            return ViewHolder(textView)
//        }
//
//        // 第１引数のViewHolderはこのファイルの上のほうで作成した`class ViewHolder`です。
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
            .show()
    }

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