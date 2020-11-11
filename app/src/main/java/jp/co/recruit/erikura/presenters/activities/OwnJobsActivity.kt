package jp.co.recruit.erikura.presenters.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.OwnJobQuery
import jp.co.recruit.erikura.business.util.DateUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityOwnJobsBinding
import jp.co.recruit.erikura.databinding.FragmentTabAppliedJobsBinding
import jp.co.recruit.erikura.databinding.FragmentTabReportedJobsBinding
import jp.co.recruit.erikura.databinding.FragmentTabWorkingJobsBinding
import jp.co.recruit.erikura.presenters.activities.job.CanceledDialogFragment
import jp.co.recruit.erikura.presenters.activities.report.ReportCompletedDialogFragment
import jp.co.recruit.erikura.presenters.fragments.AppliedJobsFragment
import jp.co.recruit.erikura.presenters.fragments.FinishedJobsFragment
import jp.co.recruit.erikura.presenters.fragments.ReportedJobsFragment
import jp.co.recruit.erikura.presenters.fragments.WorkingTimeCircleFragment
import kotlinx.android.synthetic.main.activity_own_jobs.*
import java.util.*

class OwnJobsActivity : BaseTabbedActivity(R.id.tab_menu_applied_jobs), OwnJobsHandlers {
    companion object {
        var savedTabPosition: Int? = null
        val EXTRA_FROM_REPORT_COMPLETED_KEY = "fromReportCompleted"
        val EXTRA_FROM_MYPAGE_JOB_COMMENT_GOOD_BUTTON = "fromMypageJobCommentGoodButton"
        val EXTRA_FROM_CANCEL_JOB = "fromCancelJob"
        val EXTRA_FROM_WORKING_FINISHED = "fromWorkingFinished"

        val PAGE_APPLIED_JOBS = 0
        val PAGE_FINISHED_JOBS = 1
        val PAGE_REPORTED_JOBS = 2
    }

    private val viewModel: OwnJobsViewModel by lazy {
        ViewModelProvider(this).get(OwnJobsViewModel::class.java)
    }
    var fromReportCompleted = false
    var fromMypageJobCommentGoodButton = false
    var fromReportedFDL = false
    lateinit var viewPager: ViewPager

    // 差し戻しマークが表示されているか?
    val hasRejected: Boolean get() = viewModel.hasRejected.value ?: false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityOwnJobsBinding = DataBindingUtil.setContentView(this, R.layout.activity_own_jobs)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        val adapter = object: FragmentPagerAdapter(supportFragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                return when(position) {
                    PAGE_APPLIED_JOBS -> AppliedJobsFragment()
                    PAGE_FINISHED_JOBS -> FinishedJobsFragment()
                    PAGE_REPORTED_JOBS -> ReportedJobsFragment()
                    else -> throw IllegalArgumentException("Invalid position: " + position.toString())
                }
            }

            override fun getPageTitle(position: Int): CharSequence? {
                return when(position) {
                    PAGE_APPLIED_JOBS -> "未実施"
                    PAGE_FINISHED_JOBS -> "実施済み\n(未報告)"
                    PAGE_REPORTED_JOBS -> "報告済み"
                    else -> throw IllegalArgumentException("Invalid position: " + position.toString())
                }
            }

            override fun getCount(): Int {
                return 3
            }
        }
        viewPager = findViewById(R.id.applied_jobs_view_pager)
        viewPager.adapter = adapter

        val tabLayout: TabLayout = findViewById(R.id.owned_jobs_tab_layout)
        tabLayout.setupWithViewPager(viewPager)


        tabLayout.getTabAt(PAGE_APPLIED_JOBS)?.let {
            val binding = FragmentTabAppliedJobsBinding.inflate(layoutInflater, it.view, false)
            binding.lifecycleOwner = this
            binding.viewModel = viewModel
            it.setCustomView(binding.root)
        }
        tabLayout.getTabAt(PAGE_FINISHED_JOBS)?.let {
            val binding = FragmentTabWorkingJobsBinding.inflate(layoutInflater, it.view, false)
            binding.lifecycleOwner = this
            binding.viewModel = viewModel
            it.setCustomView(binding.root)
        }
        tabLayout.getTabAt(PAGE_REPORTED_JOBS)?.let {
            val binding = FragmentTabReportedJobsBinding.inflate(layoutInflater, it.view, false)
            binding.lifecycleOwner = this
            binding.viewModel = viewModel
            it.setCustomView(binding.root)
        }
    }

    override fun onResume() {
        super.onResume()

        savedTabPosition?.let { position ->
            owned_jobs_tab_layout.getTabAt(position)?.select()
        }

        // 応募キャンセルから遷移してきた場合
        if (intent.getBooleanExtra(EXTRA_FROM_CANCEL_JOB, false)) {
            intent.putExtra(EXTRA_FROM_CANCEL_JOB, false)

            // 応募中の仕事ページを表示します
            viewPager.setCurrentItem(PAGE_APPLIED_JOBS, true)
            // 仕事をキャンセルした文言を表示します
            val dialog = CanceledDialogFragment()
            dialog.show(supportFragmentManager, "Canceled")
        }else if(intent.getBooleanExtra(EXTRA_FROM_WORKING_FINISHED, false)) {
            // 作業完了画面から移動してきた場合
            intent.putExtra(EXTRA_FROM_WORKING_FINISHED, false)
            // 未報告の仕事ページを表示します
            viewPager.setCurrentItem(PAGE_FINISHED_JOBS, true)
        }

        fromReportCompleted = intent.getBooleanExtra(EXTRA_FROM_REPORT_COMPLETED_KEY, false)
        if (fromReportCompleted) {
            viewPager.setCurrentItem(PAGE_REPORTED_JOBS, true)
            val uploadingDialog = ReportCompletedDialogFragment()
            uploadingDialog.show(supportFragmentManager, "ReportCompleted")
            intent.putExtra(EXTRA_FROM_REPORT_COMPLETED_KEY, false)
            fromReportCompleted = false
        }
        //FDLの処理
        fromReportedFDL = handleReportedFDL(intent)
        if (fromReportedFDL) {
            viewPager.setCurrentItem(PAGE_REPORTED_JOBS, true)
            intent.putExtra(EXTRA_FROM_REPORT_COMPLETED_KEY, false)
            fromReportedFDL = false
        }

        fromMypageJobCommentGoodButton = intent.getBooleanExtra(EXTRA_FROM_MYPAGE_JOB_COMMENT_GOOD_BUTTON, false)
        if (fromMypageJobCommentGoodButton) {
            viewPager.setCurrentItem(PAGE_REPORTED_JOBS, true)
            intent.putExtra(EXTRA_FROM_MYPAGE_JOB_COMMENT_GOOD_BUTTON, false)
            fromMypageJobCommentGoodButton = false
        }

        Api(this).also { api ->
            api.ownJob(OwnJobQuery(status = OwnJobQuery.Status.STARTED)) { jobs ->
                val transaction = supportFragmentManager.beginTransaction()
                if (!jobs.isNullOrEmpty()) {
                    val sortedJobs = jobs.sortedBy{
                        it.entry?.limitAt
                    }.first()
                    val timerCircle = WorkingTimeCircleFragment.newInstance(sortedJobs)
                    transaction.replace(R.id.own_jobs_timer_circle, timerCircle, "timerCircle")
                    transaction.commitAllowingStateLoss()
                }else {
                    val fragment = supportFragmentManager.findFragmentByTag("timerCircle")
                    fragment?.let {
                        transaction.remove(fragment)
                        transaction.commitAllowingStateLoss()
                    }
                }
            }

            refreshHasRejected(api)
        }
    }

    override fun onPause() {
        super.onPause()
        savedTabPosition = owned_jobs_tab_layout.selectedTabPosition
    }

    fun refreshHasRejected(api: Api) {
        // 差戻しから48時間以上経過した案件は自動キャンセルされる
        //   => 念の為、当日から 30日前まで取得する
        val today = Date()
        val endDate = DateUtils.endOfMonth(today)
        val startDate = DateUtils.addDays(today, -30)
        api.ownJob(OwnJobQuery(status = OwnJobQuery.Status.REPORTED, reportedFrom = startDate, reportedTo = endDate)) { jobs ->
            viewModel.hasRejected.value = false
            jobs.forEach { job ->
                if (job.isRejected) {
                    viewModel.hasRejected.value = true
                }
            }
        }
    }

    private fun handleReportedFDL(intent: Intent): Boolean {
        var fromReportedFDL = false
        val appLinkData: Uri? = intent.data
        appLinkData?.let{
            if (appLinkData.path == "/app/link/report/finish/") {
                fromReportedFDL = true
            }
        }
        return fromReportedFDL
    }
}

class OwnJobsViewModel: ViewModel() {
    val hasRejected = MutableLiveData<Boolean>(false)
    val rejectedBadgeVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(hasRejected) { rejected ->
            result.value = if (rejected) { View.VISIBLE } else { View.GONE }
        }
    }
}

interface OwnJobsHandlers: TabEventHandlers {
}