package jp.co.recruit.erikura.presenters.activities

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.OwnJobQuery
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityOwnJobsBinding
import jp.co.recruit.erikura.presenters.activities.report.ReportCompletedDialogFragment
import jp.co.recruit.erikura.presenters.fragments.AppliedJobsFragment
import jp.co.recruit.erikura.presenters.fragments.FinishedJobsFragment
import jp.co.recruit.erikura.presenters.fragments.ReportedJobsFragment
import jp.co.recruit.erikura.presenters.fragments.WorkingTimeCircleFragment
import kotlinx.android.synthetic.main.activity_own_jobs.*

class OwnJobsActivity : BaseTabbedActivity(R.id.tab_menu_applied_jobs), OwnJobsHandlers {
    companion object {
        var savedTabPosition: Int? = null
    }

    private val viewModel: OwnJobsViewModel by lazy {
        ViewModelProvider(this).get(OwnJobsViewModel::class.java)
    }
    var fromReportCompleted = false
    var fromMypageJobCommentGoodButton = false
    lateinit var viewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityOwnJobsBinding = DataBindingUtil.setContentView(this, R.layout.activity_own_jobs)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        val adapter = object: FragmentPagerAdapter(supportFragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                return when(position) {
                    0 -> AppliedJobsFragment()
                    1 -> FinishedJobsFragment()
                    2 -> ReportedJobsFragment()
                    else -> throw IllegalArgumentException("Invalid position: " + position.toString())
                }
            }

            override fun getPageTitle(position: Int): CharSequence? {
                return when(position) {
                    0 -> "未実施"
                    1 -> "実施済み\n(未報告)"
                    2 -> "報告済み"
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

        tabLayout.getTabAt(0)?.let {
            it.setCustomView(R.layout.fragment_tab_applied_jobs)
        }
        tabLayout.getTabAt(1)?.let {
            it.setCustomView(R.layout.fragment_tab_working_jobs)
        }
        tabLayout.getTabAt(2)?.let {
            it.setCustomView(R.layout.fragment_tab_reported_jobs)
        }
    }

    override fun onResume() {
        super.onResume()

        savedTabPosition?.let { position ->
            owned_jobs_tab_layout.getTabAt(position)?.select()
        }

        fromReportCompleted = intent.getBooleanExtra("fromReportCompleted", false)
        if (fromReportCompleted) {
            viewPager.setCurrentItem(2, true)
            val uploadingDialog = ReportCompletedDialogFragment()
            uploadingDialog.show(supportFragmentManager, "ReportCompleted")
            intent.putExtra("fromReportCompleted", false)
            fromReportCompleted = false
        }

        fromMypageJobCommentGoodButton = intent.getBooleanExtra("fromMypageJobCommentGoodButton", false)
        if (fromMypageJobCommentGoodButton) {
            viewPager.setCurrentItem(2, true)
            intent.putExtra("fromMypageJobCommentGoodButton", false)
            fromMypageJobCommentGoodButton = false
        }

        Api(this).ownJob(OwnJobQuery(status = OwnJobQuery.Status.STARTED)) { jobs ->
            val transaction = supportFragmentManager.beginTransaction()
            if (!jobs.isNullOrEmpty()) {
                val sortedJobs = jobs.sortedBy{
                    it.entry?.limitAt
                }.first()
                val timerCircle = WorkingTimeCircleFragment(sortedJobs)
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
    }

    override fun onPause() {
        super.onPause()
        savedTabPosition = owned_jobs_tab_layout.selectedTabPosition
    }
}

class OwnJobsViewModel: ViewModel() {}

interface OwnJobsHandlers: TabEventHandlers {
}