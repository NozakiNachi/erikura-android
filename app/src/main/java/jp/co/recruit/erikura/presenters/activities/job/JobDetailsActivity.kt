package jp.co.recruit.erikura.presenters.activities.job

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobKind
import jp.co.recruit.erikura.business.models.JobStatus
import jp.co.recruit.erikura.business.models.Place
import jp.co.recruit.erikura.databinding.ActivityJobDetailsBinding
import jp.co.recruit.erikura.presenters.fragments.NomalJobDetailsFragment
import java.util.*


class JobDetailsActivity : AppCompatActivity() {
    private val viewModel: JobDetailsViewModel by lazy {
        ViewModelProvider(this).get(JobDetailsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_details)

        val binding: ActivityJobDetailsBinding = DataBindingUtil.setContentView(this, R.layout.activity_job_details)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        Log.v("DEBUG", viewModel.job.value.toString())

        var fragment = Fragment()


        val jobSample = Job(id=4941, placeId=1348, place= Place(id=1348, workingBuilding="建物名", workingPlace="応募したことない場所の住所", workingPlaceShort="応募したことない場所の短縮住所", latitude=33.5890793547, longitude=130.3530555915, thumbnailUrl="http://10.9.51.15:3000/rails/active_storage/blobs/eyJfcmFpbHMiOnsibWVzc2FnZSI6IkJBaHBBbHNDIiwiZXhwIjpudWxsLCJwdXIiOiJibG9iX2lkIn19--a6176b64244d8836228747bfd288199589cb16b8/building11.jpg", hasEntries=false, jobs= mapOf()), title="【募集中】応募したことがない場所のタスク", workingStartAt= Date(), workingFinishAt=Date(), fee=1500, workingTime=150, workingPlace="応募したことない場所の短縮住所", summary="指定された箇所の撮影及び清掃, tools=スマホ、巡回章、ごみ袋、ほうき、ちりとり", entryQuestion="", latitude=33.5890793547, longitude=130.3530555915, thumbnailUrl="http://10.9.51.15:3000/rails/active_storage/blobs/eyJfcmFpbHMiOnsibWVzc2FnZSI6IkJBaHBBbHNDIiwiZXhwIjpudWxsLCJwdXIiOiJibG9iX2lkIn19--a6176b64244d8836228747bfd288199589cb16b8/building11.jpg", manualUrl="http://10.9.51.15:3000/rails/active_storage/blobs/eyJfcmFpbHMiOnsibWVzc2FnZSI6IkJBaHBBbE1DIiwiZXhwIjpudWxsLCJwdXIiOiJibG9iX2lkIn19--57c8d496dd76bc4fbdf483844d6015486a37274d/sample2.pdf", modelReportUrl=null, wanted=false, boost=false, distance=554, jobKind= JobKind(id=1, name="テスト用作業種別", iconUrl="/icons/camera.png", refine=true, summaryTitles= listOf()), entryId=null, entry=null, reportId=null, report=null, reEntryPermitted=true, summaryTitles=listOf(), tools = "A, B, C")
        fragment = NomalJobDetailsFragment(this, job = jobSample)


        // jobのステータスで挿しこむフラグメントを変更します
        if(viewModel.job.value != null){
            when(viewModel.job.value!!.status) {
                JobStatus.Normal -> {
                    fragment = NomalJobDetailsFragment(this, job = viewModel.job.value)
                }
                JobStatus.Applied -> {
                    // FIXME: 応募済み画面
                    fragment = NomalJobDetailsFragment(this, job = viewModel.job.value)
                }
                JobStatus.Working -> {
                    // FIXME: 作業中画面
                    fragment = NomalJobDetailsFragment(this, job = viewModel.job.value)
                }
                JobStatus.Finished -> {
                    // FIXME: 作業完了画面
                    fragment = NomalJobDetailsFragment(this, job = viewModel.job.value)
                }
                JobStatus.Reported -> {
                    // FIXME: 作業報告済み画面
                    fragment = NomalJobDetailsFragment(this, job = viewModel.job.value)
                }
                else -> {
                    fragment = NomalJobDetailsFragment(this, job = viewModel.job.value)
                }
            }

        }

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.job_details, fragment)
        transaction.commit()

    }
}

class JobDetailsViewModel: ViewModel() {
    val job: MutableLiveData<Job> = MutableLiveData()
}
