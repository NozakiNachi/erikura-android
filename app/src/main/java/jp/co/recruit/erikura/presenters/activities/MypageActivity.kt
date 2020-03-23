package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
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
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Information
import jp.co.recruit.erikura.business.models.OwnJobQuery
import jp.co.recruit.erikura.business.util.DateUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityMypageBinding
import jp.co.recruit.erikura.databinding.FragmentInformationCellBinding
import jp.co.recruit.erikura.databinding.FragmentMypageCellBinding
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import kotlinx.android.synthetic.main.activity_mypage.*
import jp.co.recruit.erikura.presenters.activities.mypage.PaymentInformationActivity
import jp.co.recruit.erikura.presenters.fragments.NormalJobDetailsFragment
import java.util.*

class MypageActivity : AppCompatActivity(), MypageEventHandlers {
    private lateinit var informationListView: RecyclerView
    private lateinit var informationListAdapter: InformationAdapter

    data class MypageItem(val id: Int, val label: String, val iconDrawableId: Int, val requireLogin: Boolean, val onSelect: () -> Unit)

    private val viewModel: MypageViewModel by lazy {
        ViewModelProvider(this).get(MypageViewModel::class.java)
    }

    var mypageItems: List<MypageItem> = listOf(
        MypageItem(0, "お支払情報", R.drawable.ic_account, true) {
            Intent(this, PaymentInformationActivity::class.java).let {
                startActivity(it, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
        },
        MypageItem(1, "お気に入り", R.drawable.icon_star_18, true) {
            // FIXME: 正しいリンク先の作成
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        },
        MypageItem(2, "仕事へのコメント・いいね", R.drawable.icon_comment_18, true) {
            Intent(this, OwnJobsActivity::class.java).let { intent ->
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("fromMypageJobCommentGoodButton", true)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
        },
        MypageItem(3, "設定", R.drawable.ic_preferences, true) {
            val intent = Intent(this, ConfigurationActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    )

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

        // 設定画面のメニューをrecycler_viewで表示
        val adapter = MypageAdapter(mypageItems)
        adapter.setOnItemClickListener(object: MypageAdapter.OnItemClickListener{
            override fun onItemClickListener(item: MypageItem) {
                item.onSelect()
            }
        })
        mypage_recycler_view.adapter = adapter
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        mypage_recycler_view.addItemDecoration(itemDecoration)
    }

    class MypageAdapter(private val mypageItems: List<MypageItem>) : RecyclerView.Adapter<MypageAdapter.ViewHolder>()
    {
        class ViewHolder(val binding: FragmentMypageCellBinding) : RecyclerView.ViewHolder(binding.root)
        var listener: OnItemClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = DataBindingUtil.inflate<FragmentMypageCellBinding>(
                LayoutInflater.from(parent.context),
                R.layout.fragment_mypage_cell,
                parent,
                false
            )
            return ViewHolder(binding)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val MypageListItem = mypageItems.get(position)
            val viewModel = MypageMenuItemViewModel(MypageListItem)
            holder.binding.viewModel = viewModel

            holder.binding.root.setOnClickListener {
                listener?.onItemClickListener(mypageItems[position])
            }
        }
        interface OnItemClickListener{
            fun onItemClickListener(item: MypageItem)
        }

        fun setOnItemClickListener(listener: OnItemClickListener){
            this.listener = listener
        }

        override fun getItemCount() = mypageItems.size
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
        result.addSource(monthlyReward) { result.value = String.format("%,d", monthlyReward.value ?: 0) }
    }
    val formattedMonthlyCompletedJobs = MediatorLiveData<String>().also { result ->
        result.addSource(monthlyCompletedJobs) { result.value = String.format("%,d", monthlyCompletedJobs.value ?: 0) }
    }
    val formattedMonthlyGoodsCount = MediatorLiveData<String>().also { result ->
        result.addSource(monthlyGoodCount) { result.value = String.format("%,d", monthlyGoodCount.value ?: 0)}
    }

    init {
        monthlyReward.value = 0
        monthlyCompletedJobs.value = 0
        monthlyGoodCount.value = 0
    }
}

interface MypageEventHandlers {
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

class MypageMenuItemViewModel(val item: MypageActivity.MypageItem) : ViewModel() {
    val icon: Drawable get() = ErikuraApplication.applicationContext.resources.getDrawable(item.iconDrawableId, null)
}