package jp.co.recruit.erikura.presenters.activities

import android.app.ActivityOptions
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.MenuItem
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
import jp.co.recruit.erikura.presenters.activities.job.MapViewActivity
import jp.co.recruit.erikura.presenters.activities.mypage.MypageActivity
import jp.co.recruit.erikura.presenters.activities.report.ReportCompletedDialogFragment
import jp.co.recruit.erikura.presenters.fragments.AppliedJobsFragment
import jp.co.recruit.erikura.presenters.fragments.FinishedJobsFragment
import jp.co.recruit.erikura.presenters.fragments.ReportedJobsFragment
import jp.co.recruit.erikura.presenters.fragments.WorkingTimeCircleFragment
import java.lang.IllegalArgumentException

class OwnJobsActivity : AppCompatActivity(), OwnJobsHandlers {

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

        // FIXME: viewPager の設定
        // FIXME: tabItem のカスタマイズ
        // FIXME: fragment の実装

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
                    1 -> {
                        val sb = SpannableStringBuilder()
                        sb.append("実施済み\n")
                        val start = sb.length
                        sb.append("(未報告)")
                        val end = sb.length
                        sb.setSpan(RelativeSizeSpan(12.0f / 16.0f), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                        sb.toString()
                    }
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
//        viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
//            override fun onPageScrollStateChanged(state: Int) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//            }
//
//            override fun onPageScrolled(
//                position: Int,
//                positionOffset: Float,
//                positionOffsetPixels: Int
//            ) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//            }
//
//            override fun onPageSelected(position: Int) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//            }
//        })

        val tabLayout: TabLayout = findViewById(R.id.owned_jobs_tab_layout)
        tabLayout.setupWithViewPager(viewPager)


        val tab = tabLayout.getTabAt(0)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.v("MENU ITEM SELECTED: ", item.toString())
        when(item.itemId) {
            R.id.tab_menu_search_jobs -> {
                // 地図画面、またはリスト画面に遷移します
                Intent(this, MapViewActivity::class.java).let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(it, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                }
            }
            R.id.tab_menu_applied_jobs -> {
                // 何も行いません
            }
            R.id.tab_menu_mypage -> {
                Intent(this, MypageActivity::class.java).let { intent ->
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                }
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()

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
            if (!jobs.isNullOrEmpty()) {
                val transaction = supportFragmentManager.beginTransaction()
                val sortedJobs = jobs.sortedBy{
                    it.entry?.limitAt
                }.first()
                val timerCircle = WorkingTimeCircleFragment(sortedJobs)
                transaction.replace(R.id.own_jobs_timer_circle, timerCircle, "timerCircle")
                transaction.commit()
            }
        }
    }
}

class OwnJobsViewModel: ViewModel() {}

interface OwnJobsHandlers {
    fun onNavigationItemSelected(item: MenuItem): Boolean
}