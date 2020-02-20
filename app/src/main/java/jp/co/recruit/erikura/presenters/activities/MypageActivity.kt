package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Information
import jp.co.recruit.erikura.business.models.OwnJobQuery
import jp.co.recruit.erikura.business.util.DateUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityMypageBinding
import jp.co.recruit.erikura.databinding.FragmentInformationCellBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.mypage.PaymentInformationActivity
import java.util.*

class MypageActivity : AppCompatActivity(), MypageEventHandlers {
    private lateinit var informationListView: RecyclerView
    private lateinit var informationListAdapter: InformationAdapter

    private val viewModel: MypageViewModel by lazy {
        ViewModelProvider(this).get(MypageViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)

        val binding: ActivityMypageBinding = DataBindingUtil.setContentView(this, R.layout.activity_mypage)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        // 下部のタブの選択肢を仕事を探すに変更
        val nav: BottomNavigationView = findViewById(R.id.mypage_view_navigation)
        nav.selectedItemId = R.id.tab_menu_mypage

        informationListAdapter = InformationAdapter(this)
        informationListView = findViewById(R.id.mypage_information_list)
        informationListView.adapter = informationListAdapter
        informationListView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    override fun onStart() {
        super.onStart()

        val api = Api(this)
        if (Api.isLogin) {
            val query = OwnJobQuery(status = OwnJobQuery.Status.REPORTED,
                reportedFrom = DateUtils.beginningOfMonth(Date()),
                reportedTo = DateUtils.endOfMonth(Date()))
            api.ownJob(query) { jobs ->
                var reward = 0
                var count = 0
                var good = 0

                jobs.forEach { job ->
                    reward += job.fee ?: 0
                    count += 1
                    good += job.report?.operatorLikeCount ?: 0
                }
                viewModel.monthlyReward.value = reward
                viewModel.monthlyCompletedJobs.value = count
                viewModel.monthlyGoodCount.value = good
            }
        }

        api.informations {
            informationListAdapter.informations = it
            informationListAdapter.notifyDataSetChanged()
        }
    }

    override fun onClickPaymentinformationLink(view: View) {
        Intent(this, PaymentInformationActivity::class.java).let {
            startActivity(it, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    override fun onClickJobEvaluation(view: View) {
        // リンク先の作成
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onClickConfiguration(view: View) {
        // リンク先の作成
        val intent = Intent(this, ConfigurationActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.v("MENU ITEM SELECTED: ", item.toString())
        when(item.itemId) {
            R.id.tab_menu_search_jobs -> {
                Intent(this, MapViewActivity::class.java).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                }
            }
            R.id.tab_menu_applied_jobs -> {
                Intent(this, OwnJobsActivity::class.java).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                }
            }
            R.id.tab_menu_mypage -> {
                // 何も行いません
            }
        }
        return true
    }

}

class MypageViewModel: ViewModel() {
    val monthlyReward: MutableLiveData<Int> = MutableLiveData()
    val monthlyCompletedJobs: MutableLiveData<Int> = MutableLiveData()
    val monthlyGoodCount: MutableLiveData<Int> = MutableLiveData()

    val formattedMonthlyRewards = MediatorLiveData<String>().also { result ->
        result.addSource(monthlyReward) { result.value = String.format("%,d円", monthlyReward.value ?: 0) }
    }
    val formattedMonthlyCompletedJobs = MediatorLiveData<String>().also { result ->
        result.addSource(monthlyCompletedJobs) { result.value = String.format("%,d件", monthlyCompletedJobs.value ?: 0) }
    }
    val formattedMonthlyGoodsCount = MediatorLiveData<String>().also { result ->
        result.addSource(monthlyGoodCount) { result.value = String.format("%,d件", monthlyGoodCount.value ?: 0)}
    }

    init {
        monthlyReward.value = 0
        monthlyCompletedJobs.value = 0
        monthlyGoodCount.value = 0
    }
}

interface MypageEventHandlers {
    // 今月の○○表示(非ログインチェック)
    //fun onClickUnreachLink(view: View)
    // お支払情報ページへのリンク
    fun onClickPaymentinformationLink(view: View)
    // 仕事へのコメント・いいねへのリンク
    fun onClickJobEvaluation(view: View)
    // 設定画面へのリンク
    fun onClickConfiguration(view: View)
    // お知らせ取得API
    //fun onClickUnreachLink(view: View)

    fun onNavigationItemSelected(item: MenuItem): Boolean
}

class InformationAdapter(val activity: FragmentActivity) : RecyclerView.Adapter<InformationCellHolder>() {
    var informations: List<Information> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InformationCellHolder {
        val binding: FragmentInformationCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context), R.layout.fragment_information_cell, parent, false
        )
        return InformationCellHolder(binding)
    }

    override fun getItemCount(): Int {
        return informations.count()
    }

    override fun onBindViewHolder(holder: InformationCellHolder, position: Int) {
        val information = informations[position]!!
        val viewModel = InformationCellViewModel(information)

        holder.binding.lifecycleOwner = activity
        holder.binding.viewModel = viewModel

        // WebView にコンテンツを設定します
        val webView = holder.binding.informationCellWebview
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
        val encodedHtml = Base64.encodeToString(information.content.toByteArray(), Base64.NO_PADDING)
        webView.loadData(encodedHtml, "text/html", "base64")
        // FIXME: WebView, Layout の高さ調整
    }
}

class InformationCellHolder(val binding: FragmentInformationCellBinding): RecyclerView.ViewHolder(binding.root)

class InformationCellViewModel(val information: Information): ViewModel() {
    val lastUpdated: String get() {
        val now = Date()
        val diff = now.time - information.createdAt.time
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