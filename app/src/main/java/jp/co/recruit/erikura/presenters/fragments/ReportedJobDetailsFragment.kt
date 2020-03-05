package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentReportedJobDetailsBinding
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.models.UserSession


class ReportedJobDetailsFragment(private val activity: AppCompatActivity, val job: Job?, val user: User) : Fragment() {
    private val viewModel: ReportedJobDetailsFragmentViewModel by lazy {
        ViewModelProvider(this).get(ReportedJobDetailsFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding = FragmentReportedJobDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        viewModel.setup(activity, job, user)
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val transaction = childFragmentManager.beginTransaction()
        //×ボタン
        val timeLabel = TimeLabelFragment(job, user)
        val jobInfoView = JobInfoViewFragment(job)
        val thumbnailImage = ThumbnailImageFragment(job)
        val reportedJobStatus = ReportedJobStatusFragment(activity, job?.report)
        val reportedJobEditButton = ReportedJobEditButtonFragment(job)
        val reportedJobRemoveButton = ReportedJobRemoveButtonFragment(job)
        val jobDetailsView = JobDetailsViewFragment(job)
        //実施箇所表示
        //作業時間
        //マニュアル外の報告
        //マニュアル外の報告画像
        //コメント
        //運営からの評価
        //案件の評価

        //FIXME:　×ボタン
        transaction.add(R.id.jobDetails_timeLabelFragment, timeLabel, "timeLabel")
        transaction.add(R.id.jobDetails_jobInfoViewFragment, jobInfoView, "jobInfoView")
        transaction.add(R.id.jobDetails_thumbnailImageFragment, thumbnailImage, "thumbnailImage")
        transaction.add(R.id.jobDetails_reportedJobStatus, reportedJobStatus, "reportedJobStatus")
        transaction.add(R.id.reportedJobEditButton, reportedJobEditButton, "reportedJobEditButton")
        transaction.add(R.id.reportedJobRemoveButton, reportedJobRemoveButton, "reportedJobRemoveButton")
        transaction.add(R.id.jobDetails_jobDetailsViewFragment, jobDetailsView, "jobDetailsView")
        //FIXME: 実施箇所表示
        val implementedLocationList: RecyclerView = activity!!.findViewById(R.id.ImplementedLocationList)
        implementedLocationList.setHasFixedSize(true)
        // Holder(getするやつ)とアダプターを用意する
//        implementedLocationList.adapter = implementLocationAdapter
//        implementedLocationList.addItemDecoration(DividerItemDecoration(activity!!, DividerItemDecoration.VERTICAL))
//        implementedLocationList.addItemDecoration(JobListItemDecorator())
        //FIXME:　作業時間
        //transaction.add(R.id.jobDetails_jobDetailsViewFragment, jobDetailsView, "jobDetailsView")
        //FIXME: マニュアル外の報告
        //transaction.add(R.id.jobDetails_jobDetailsViewFragment, jobDetailsView, "jobDetailsView")
        //FIXME: マニュアル外の報告画像
        //transaction.add(R.id.jobDetails_thumbnailImageFragment, thumbnailImage, "thumbnailImage")
        //FIXME: コメント
        //transaction.add(R.id.jobDetails_thumbnailImageFragment, thumbnailImage, "thumbnailImage")
        //FIXME: 運営からの評価
        //transaction.add(R.id.jobDetails_thumbnailImageFragment, thumbnailImage, "thumbnailImage")
        //FIXME:　案件の評価
        //transaction.add(R.id.jobDetails_thumbnailImageFragment, thumbnailImage, "thumbnailImage")
        transaction.commit()
    }
}

//interface ReportedJobDetailsHandler {
//    fun onClickClose(view: View)
//}

class ReportedJobDetailsFragmentViewModel: ViewModel() {
    val bitmapDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()
    val warningCaption: MutableLiveData<String> = MutableLiveData()
    val warningCaptionVisibility: MutableLiveData<Int> = MutableLiveData(View.GONE)

    fun setup(activity: Activity, job: Job?, user: User) {
        if (job != null){
            // ダウンロード
            job.thumbnailUrl?.let { url ->
                val assetsManager = ErikuraApplication.assetsManager

                assetsManager.fetchImage(activity, url) { result ->
                    activity.runOnUiThread {
                        val bitmapReduced = Bitmap.createScaledBitmap(result, 15, 15, true)
                        val bitmapDraw = BitmapDrawable(bitmapReduced)
                        bitmapDraw.alpha = 150
                        bitmapDrawable.value = bitmapDraw
                    }
                }
            }

            if (job.isFuture || job.isPast) {
                warningCaption.value = ErikuraApplication.instance.getString(R.string.jobDetails_outOfEntryExpire)
                warningCaptionVisibility.value = View.VISIBLE
            }else if (job.isEntried || (UserSession.retrieve() != null && job.targetGender != null && job.targetGender != user.gender) || job.banned) {
                warningCaption.value = ErikuraApplication.instance.getString(R.string.jobDetails_entryFinished)
                warningCaptionVisibility.value = View.VISIBLE
            }else if (!job.reEntryPermitted) {
                warningCaption.value = ErikuraApplication.instance.getString(R.string.jobDetails_cantEntry)
                warningCaptionVisibility.value = View.VISIBLE
            }else if (UserSession.retrieve() != null && user.holdingJobs >= user.maxJobs) {
                warningCaption.value = ErikuraApplication.instance.getString(R.string.jobDetails_maxEntry, user.maxJobs)
                warningCaptionVisibility.value = View.VISIBLE
            }else {
                warningCaption.value = ""
                warningCaptionVisibility.value = View.GONE
            }

        }
    }
}