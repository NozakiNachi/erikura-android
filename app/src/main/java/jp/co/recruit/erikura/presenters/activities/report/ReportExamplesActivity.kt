package jp.co.recruit.erikura.presenters.activities.report

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.ReportExample
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityReportExamplesBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.fragments.ReportExamplesFragment
import java.text.SimpleDateFormat
import java.util.*

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

        val adapter = object : FragmentPagerAdapter(
            supportFragmentManager,
            FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        ) {
            override fun getItem(position: Int): Fragment {
                //指定されたお手本報告の実施箇所を元に生成したフラグメントを返す
                val created_at: String? =
                    reportExamples?.get(position)?.created_at?.let { created_at ->
                        makeSentenceCreatedAt(
                            created_at
                        )
                    }
                return ReportExamplesFragment.newInstance(
                    reportExamples?.get(position)?.output_summary_examples_attributes,
                    job,
                    created_at,
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
        viewPager.setCurrentItem(1, true)

        val api = Api(this)
        job.jobKind?.id?.let { jobKindId ->
            //APIでお手本報告を取得する
            api.goodExamples(job.placeId, jobKindId, true) { listReportExamples ->
                //報告日付の降順にソートします。
                reportExamples = listReportExamples.sortedByDescending { reportExample ->
                    reportExample.created_at
                }
                reportExampleCount = listReportExamples.count()
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewPager.setCurrentItem(viewPager.currentItem, true)
    }

    fun makeSentenceCreatedAt(created_at: Date): String {
        val df = SimpleDateFormat("yyyy/MM/dd")
        return ("このお手本の作業報告日：".plus(df.format(created_at)))
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