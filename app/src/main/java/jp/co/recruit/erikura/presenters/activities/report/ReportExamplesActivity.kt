package jp.co.recruit.erikura.presenters.activities.report

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.ReportExample
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityReportExamplesBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity

class ReportExamplesActivity : BaseActivity(), ReportExamplesEventHandlers {
    private val viewModel: ReportExamplesViewModel by lazy {
        ViewModelProvider(this).get(ReportExamplesViewModel::class.java)
    }

    var job: Job = Job()
    var reportExamples: List<ReportExample>? = null
    lateinit var viewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityReportExamplesBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_report_examples)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.handlers = this

        job = intent.getParcelableExtra<Job>("job")
        job.jobKind?.id?.let { jobKindId ->
            //APIでお手本報告を取得する
            Api(this).goodExamples(job.placeId, jobKindId, true) {
                reportExamples = it

                val adapter = object: FragmentPagerAdapter(supportFragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
                    override fun getCount(): Int {
                        TODO("Not yet implemented")
                    }

                    override fun getItem(position: Int): Fragment {
                        TODO("Not yet implemented")
                    }

                    override fun getPageTitle(position: Int): CharSequence? {
                        return super.getPageTitle(position)
                    }
                }
                viewPager = findViewById(R.id.report_examples_view_pager)
                viewPager.adapter = adapter
            }
        }
    }
}

class ReportExamplesViewModel : ViewModel() {

}

interface ReportExamplesEventHandlers {

}
