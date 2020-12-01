package jp.co.recruit.erikura.presenters.activities.report

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.ReportExample
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityReportExamplesBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.fragments.ReportExamplesFragment

class ReportExamplesActivity : BaseActivity() {

    var job: Job = Job()
    var reportExamples: List<ReportExample>? = null
    var reportExampleCount: Int? = 0
    val api = Api(this)
    var adapter: FragmentPagerAdapter? = null
    lateinit var viewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityReportExamplesBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_report_examples)
        binding.lifecycleOwner = this

        intent.getParcelableExtra<Job>("job")?.let {
            job = it
        }

        intent.getParcelableArrayListExtra<ReportExample>("reportExamples")?.let {
            reportExamples = it.toList()
        }
        if (intent.data?.path != null && reportExamples == null) {
            //FDLの場合
            val jobId: Int = handleIntent(intent)
            api.reloadJobById(jobId) {
                job = it
                job.jobKind?.id?.let { jobKindId ->
                    //APIでお手本報告を取得する
                    api.goodExamples(job.placeId, jobKindId, true) { listReportExamples ->
                        reportExamples = listReportExamples
                        //トラッキングの送出、お手本報告画面の表示
                        Tracking.logEvent(event = "view_good_examples", params = bundleOf())
                        Tracking.viewGoodExamples(
                            name = "/places/good_examples",
                            title = "お手本報告画面表示",
                            jobId = job.id,
                            jobKindId = jobKindId,
                            placeId = job.placeId
                        )
                        adapter = object : FragmentPagerAdapter(
                            supportFragmentManager,
                            FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
                        ) {
                            override fun getItem(position: Int): Fragment {
                                return ReportExamplesFragment.newInstance(
                                    reportExamples?.get(position),
                                    job,
                                    position,
                                    count
                                )
                            }

                            override fun getCount(): Int {
                                return reportExamples?.count() ?: 0
                            }

                        }
                        viewPager = findViewById(R.id.report_examples_view_pager)
                        viewPager.adapter = adapter
                        //お手本報告画面はまず１ページ目を表示する
                        viewPager.setCurrentItem(0, true)
                    }
                }
                // FDLで遷移した場合、API実行後に空にセットにする
                ErikuraApplication.instance.pushUri = null
            }
        } else {
            adapter = object : FragmentPagerAdapter(
                supportFragmentManager,
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
            ) {
                override fun getItem(position: Int): Fragment {
                    return ReportExamplesFragment.newInstance(
                        reportExamples?.get(position),
                        job,
                        position,
                        count
                    )
                }

                override fun getCount(): Int {
                    return reportExamples?.count() ?: 0
                }

            }
            viewPager = findViewById(R.id.report_examples_view_pager)
            viewPager.adapter = adapter
            //お手本報告画面はまず１ページ目を表示する
            viewPager.setCurrentItem(0, true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (this::viewPager.isInitialized) {
            // viewPagerの初期化が完了している場合
            viewPager.setCurrentItem(viewPager.currentItem, true)
        }
    }

    fun onClickPrev() {
        //１ページ前に戻る
        viewPager.currentItem -= 1
    }

    fun onClickNext() {
        //１ページ前に進む
        viewPager.currentItem += 1
    }

    private fun handleIntent(intent: Intent): Int {
        val appLinkData: Uri? = intent.data
        return appLinkData!!.lastPathSegment!!.toInt()
    }
}