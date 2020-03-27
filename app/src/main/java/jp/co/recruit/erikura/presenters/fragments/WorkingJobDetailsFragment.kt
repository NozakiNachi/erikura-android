package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentWorkingJobDetailsBinding
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.job.StopDialogFragment
import java.util.*


class WorkingJobDetailsFragment(
    private val activity: AppCompatActivity,
    val job: Job?,
    val user: User
) : Fragment(), WorkingJobDetailsFragmentEventHandlers {
    private val viewModel: WorkingJobDetailsFragmentViewModel by lazy {
        ViewModelProvider(this).get(WorkingJobDetailsFragmentViewModel::class.java)
    }

    private var timer: Timer = Timer()
    private var timerHandler: Handler = Handler()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding = FragmentWorkingJobDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        viewModel.setup(activity, job, user)
        binding.viewModel = viewModel
        binding.handlers = this
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val transaction = childFragmentManager.beginTransaction()
        val jobInfoView = JobInfoViewFragment(job)
        val manualImage = ManualImageFragment(job)
        val manualButton = ManualButtonFragment(job)
        val thumbnailImage = ThumbnailImageFragment(job)
        val jobDetailsView = JobDetailsViewFragment(job)
        val mapView = MapViewFragment(activity, job)
        transaction.add(R.id.workingJobDetails_jobInfoViewFragment, jobInfoView, "jobInfoView")
        transaction.add(R.id.workingJobDetails_manualImageFragment, manualImage, "manualImage")
        transaction.add(R.id.workingJobDetails_manualButtonFragment, manualButton, "manualButton")
        transaction.add(
            R.id.workingJobDetails_thumbnailImageFragment,
            thumbnailImage,
            "thumbnailImage"
        )
        transaction.add(
            R.id.workingJobDetails_jobDetailsViewFragment,
            jobDetailsView,
            "jobDetailsView"
        )
        transaction.add(R.id.workingJobDetails_mapViewFragment, mapView, "mapView")
        transaction.commit()

        timer.schedule(object : TimerTask() {
            override fun run() {
                timerHandler.post(Runnable {
                    updateTimer()
                })
            }
        }, 1000, 1000) // 実行したい間隔(ミリ秒)
    }

    override fun onClickFavorite(view: View) {
        if (viewModel.favorited.value ?: false) {
            // お気に入り登録処理
            Api(activity!!).placeFavorite(job?.place?.id ?: 0) {
                viewModel.favorited.value = true
            }
        } else {
            // お気に入り削除処理
            Api(activity!!).placeFavoriteDelete(job?.place?.id ?: 0) {
                viewModel.favorited.value = false
            }
        }
    }

    override fun onClickCancelWorking(view: View) {
        timer.cancel()
        job?.let {
            if (job.entry?.limitAt?: Date() > Date()) {
                Api(activity).abortJob(job) {
                    val intent = Intent(activity, JobDetailsActivity::class.java)
                    intent.putExtra("job", job)
                    intent.putExtra("onClickCancelWorking", true)
                    startActivity(intent)
                    activity.finish()
                }
            }else {
                val errorMessages = mutableListOf(ErikuraApplication.instance.getString(R.string.jobDetails_overLimit))
                Api(activity).displayErrorAlert(errorMessages)
            }
        }
    }

    override fun onClickStop(view: View) {
        val dialog = StopDialogFragment(job)
        dialog.show(childFragmentManager, "Stop")

        timer.cancel()

    }

    // 1秒ごとに呼び出される処理
    private fun updateTimer() {
        job?.let {
            var now = Date()
            var startTime = job.entry?.startedAt ?: job.entry?.createdAt ?: now
            var time = now.time - startTime.time
            viewModel.timeCount.value =
                String.format("%d分%02d秒", time / (60 * 1000), (time % (60 * 1000)) / 1000)
        }
    }
}

class WorkingJobDetailsFragmentViewModel : ViewModel() {
    val bitmapDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()
    val timeCount: MutableLiveData<String> = MutableLiveData()
    val favorited: MutableLiveData<Boolean> = MutableLiveData(false)
    val stopButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

    fun setup(activity: Activity, job: Job?, user: User) {
        if (job != null) {
            // ダウンロード
            val thumbnailUrl = if (!job.thumbnailUrl.isNullOrBlank()) {job.thumbnailUrl}else {job.jobKind?.noImageIconUrl?.toString()}
            if (thumbnailUrl.isNullOrBlank()) {
                val drawable = ErikuraApplication.instance.applicationContext.resources.getDrawable(R.drawable.ic_noimage, null)
                val bitmapReduced = Bitmap.createScaledBitmap( drawable.toBitmap(), 15, 15, true)
                val bitmapDraw = BitmapDrawable(bitmapReduced)
                bitmapDraw.alpha = 150
                bitmapDrawable.value = bitmapDraw
            }else {
                val assetsManager = ErikuraApplication.assetsManager
                assetsManager.fetchImage(activity, thumbnailUrl) { result ->
                    activity.runOnUiThread {
                        val bitmapReduced = Bitmap.createScaledBitmap(result, 15, 15, true)
                        val bitmapDraw = BitmapDrawable(bitmapReduced)
                        bitmapDraw.alpha = 150
                        bitmapDrawable.value = bitmapDraw
                    }
                }
            }

            // 納期が過ぎている場合はボタンを非表示
            if (job.entry?.limitAt?: Date() < Date()) {
                stopButtonVisibility.value = View.INVISIBLE
            }

            // お気に入り状態の取得
            UserSession.retrieve()?.let {
                Api(activity).placeFavoriteShow(job.place?.id ?: 0) {
                    favorited.value = it
                }
            }

            timeCount.value = "0分0秒"
        }
    }
}

interface WorkingJobDetailsFragmentEventHandlers {
    fun onClickFavorite(view: View)
    fun onClickCancelWorking(view: View)
    fun onClickStop(view: View)
}
