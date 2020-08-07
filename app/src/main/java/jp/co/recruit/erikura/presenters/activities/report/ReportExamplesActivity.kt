package jp.co.recruit.erikura.presenters.activities.report

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
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
    lateinit var viewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityReportExamplesBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_report_examples)
        binding.lifecycleOwner = this

        job = intent.getParcelableExtra<Job>("job")
        reportExamples = intent.getParcelableArrayListExtra<ReportExample>("reportExamples").toList() ?: listOf()

        val adapter = object : FragmentPagerAdapter(
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

    override fun onResume() {
        super.onResume()
        viewPager.setCurrentItem(viewPager.currentItem, true)
    }

    fun onClickPrev() {
        //１ページ前に戻る
        viewPager.currentItem -= 1
    }

    fun onClickNext() {
        //１ページ前に進む
        viewPager.currentItem += 1
    }
}