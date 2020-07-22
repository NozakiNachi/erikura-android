package jp.co.recruit.erikura.presenters.activities.report

import android.os.Bundle
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
        binding.viewModel = viewModel
        binding.handlers = this

        job = intent.getParcelableExtra<Job>("job")
        viewModel.jobKindName.value = job?.jobKind?.name

        job.jobKind?.id?.let { jobKindId ->
            val api = Api(this)
            api.place(job.placeId) { place ->
                if (place.hasEntries || place.workingPlaceShort.isNullOrEmpty()) {
                    // 現ユーザーが応募済の物件の場合　フル住所を表示
                    viewModel.address.value = place.workingPlace + place.workingBuilding
                } else {
                    // 現ユーザーが未応募の物件の場合　短縮住所を表示
                    viewModel.address.value = place.workingPlaceShort
                }
            }
            //APIでお手本報告を取得する
            api.goodExamples(job.placeId, jobKindId, true) {
                reportExamples = it
                reportExampleCount = it.count()

                val adapter = object: FragmentPagerAdapter(supportFragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
                    override fun getCount(): Int {
                        return it.count()
                    }

                    override fun getItem(position: Int): Fragment {
                        //指定されたお手本報告の実施箇所を元に生成したフラグメントを返す
                        viewModel.createdAt.value = makeSentenceCreatedAt(it[position].created_at)
                        return ReportExamplesFragment(it[position])
                    }

                    override fun getPageTitle(position: Int): CharSequence? {
                        return super.getPageTitle(position)
                    }
                }
                viewPager = findViewById(R.id.report_examples_view_pager)
                viewPager.adapter = adapter

                /// ページ遷移のリスナーをセット
                nextPageBtn.setOnClickListener {
                    // ページを1つ進める
                    viewPager.currentItem += 1
                }
                prevPageBtn.setOnClickListener {
                    // ページを1つ戻す
                    viewPager.currentItem -= 1
                }
                prevPageBtn.isVisible = false

                /// スクロール中の変更処理
                viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                    /// implementする
                    override fun onPageSelected(position: Int) {
                        /// btnの表示制御（端では表示しない）
                        prevPageBtn.isVisible = position != 0
                        nextPageBtn.isVisible = position != reportExampleCount?: 0 -1
                    }
                    override fun onPageScrollStateChanged(state: Int) {
                    }

                    override fun onPageScrolled(
                        position: Int,
                        positionOffset: Float,
                        positionOffsetPixels: Int
                    ) {}
                })
            }
        }
    }

    fun makeSentenceCreatedAt(created_at: Date): String {
        val df = SimpleDateFormat("yyyy/MM/dd")
        return ("このお手本の作業報告日：".plus(df.format(created_at)))
    }
}


class ReportExamplesViewModel : ViewModel() {
    val address: MutableLiveData<String> = MutableLiveData()
    val jobKindName: MutableLiveData<String> = MutableLiveData()
    val createdAt: MutableLiveData<String> = MutableLiveData()
}

interface ReportExamplesEventHandlers {
}
