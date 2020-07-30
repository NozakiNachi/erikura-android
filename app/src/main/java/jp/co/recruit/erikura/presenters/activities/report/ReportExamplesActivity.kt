package jp.co.recruit.erikura.presenters.activities.report

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.ReportExample
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.ActivityReportExamplesBinding
import jp.co.recruit.erikura.presenters.activities.BaseActivity
import jp.co.recruit.erikura.presenters.fragments.ReportExamplesFragment
import kotlinx.android.synthetic.main.activity_report_examples.*
import java.text.SimpleDateFormat
import java.util.*

class ReportExamplesActivity : BaseActivity(), ReportExamplesEventHandlers {
    private val viewModel: ReportExamplesViewModel by lazy {
        ViewModelProvider(this).get(ReportExamplesViewModel::class.java)
    }

    var job: Job = Job()
    var reportExamples: List<ReportExample>? = null
    var reportExampleCount: Int? = 0
    lateinit var viewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityReportExamplesBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_report_examples)
        binding.lifecycleOwner = this
//        binding.viewModel = viewModel
//        binding.handlers = this

        job = intent.getParcelableExtra<Job>("job")
//        viewModel.jobKindName.value = job?.jobKind?.name


        val api = Api(this)
//        api.place(job.placeId) { place ->
//            if (place.hasEntries || place.workingPlaceShort.isNullOrEmpty()) {
//                // 現ユーザーが応募済の物件の場合　フル住所を表示
//                viewModel.address.value = place.workingPlace + place.workingBuilding
//            } else {
//                // 現ユーザーが未応募の物件の場合　短縮住所を表示
//                viewModel.address.value = place.workingPlaceShort
//            }
//        }

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
                return ReportExamplesFragment.newInstance(reportExamples?.get(position)?.output_summary_examples_attributes, job, created_at, position, count)
            }

            override fun getCount(): Int {
                return reportExamples?.count() ?: 0
            }

        }


        viewPager = findViewById(R.id.report_examples_view_pager)
//        /// ページ遷移のリスナーをセット
//        nextPageBtn.setOnClickListener {
//            // ページを1つ進める
//            viewPager.currentItem += 1
//        }
//        prevPageBtn.setOnClickListener {
//            // ページを1つ戻す
//            viewPager.currentItem -= 1
//        }
//        prevPageBtn.isVisible = false
//
//        /// スクロール中の変更処理
//        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
//            /// implementする
//            override fun onPageSelected(position: Int) {
//                /// btnの表示制御（端では表示しない）
//                prevPageBtn.isVisible = position != 0
//                nextPageBtn.isVisible = position != reportExampleCount ?: 0 - 1
//            }
//
//            override fun onPageScrollStateChanged(state: Int) {
//            }
//
//            override fun onPageScrolled(
//                position: Int,
//                positionOffset: Float,
//                positionOffsetPixels: Int
//            ) {
//            }
//        })
        viewPager.adapter = adapter
        viewPager.setCurrentItem(1, true)

        job.jobKind?.id?.let { jobKindId ->
            //APIでお手本報告を取得する
            api.goodExamples(job.placeId, jobKindId, true) {
                reportExamples = it
                reportExampleCount = it.count()
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewPager.setCurrentItem( viewPager.currentItem,true)
    }

    fun makeSentenceCreatedAt(created_at: Date): String {
        val df = SimpleDateFormat("yyyy/MM/dd")
        return ("このお手本の作業報告日：".plus(df.format(created_at)))
    }

    fun onClickPrev() {
        viewPager.currentItem -= 1
    }

    fun onClickNext() {
        viewPager.currentItem += 1
    }
}


class ReportExamplesViewModel : ViewModel() {
//    val address: MutableLiveData<String> = MutableLiveData()
//    val jobKindName: MutableLiveData<String> = MutableLiveData()
//    val createdAt: MutableLiveData<String> = MutableLiveData()
}

interface ReportExamplesEventHandlers {
}
