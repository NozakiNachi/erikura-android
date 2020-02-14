package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentFinishedJobDetailsBinding
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.report.ReportImagePickerActivity
import java.util.*


class FinishedJobDetailsFragment(
    private val activity: AppCompatActivity,
    val job: Job?,
    val user: User
) : Fragment(), FinishedJobDetailsFragmentEventHandlers {
    private val viewModel by lazy {
        ViewModelProvider(this).get(FinishedJobDetailsFragmentViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding = FragmentFinishedJobDetailsBinding.inflate(inflater, container, false)
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
        transaction.add(R.id.finishedJobDetails_jobInfoViewFragment, jobInfoView, "jobInfoView")
        transaction.add(R.id.finishedJobDetails_manualImageFragment, manualImage, "manualImage")
        transaction.add(R.id.finishedJobDetails_manualButtonFragment, manualButton, "manualButton")
        transaction.add(
            R.id.finishedJobDetails_thumbnailImageFragment,
            thumbnailImage,
            "thumbnailImage"
        )
        transaction.add(
            R.id.finishedJobDetails_jobDetailsViewFragment,
            jobDetailsView,
            "jobDetailsView"
        )
        transaction.add(R.id.finishedJobDetails_mapViewFragment, mapView, "mapView")
        transaction.commit()
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

    override fun onClickReport(view: View) {
        val intent = Intent(activity, ReportImagePickerActivity::class.java)
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
    }

}

class FinishedJobDetailsFragmentViewModel: ViewModel() {
    val bitmapDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()
    val favorited: MutableLiveData<Boolean> = MutableLiveData(false)
    val reportButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

    fun setup(activity: Activity, job: Job?, user: User) {
        if (job != null) {
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

            // 納期が過ぎている場合はボタンを非表示
            if (job.entry?.limitAt?: Date() < Date()) {
                reportButtonVisibility.value = View.INVISIBLE
            }

            // お気に入り状態の取得
            UserSession.retrieve()?.let {
                Api(activity).placeFavoriteShow(job.place?.id ?: 0) {
                    favorited.value = it
                }
            }
        }
    }
}

interface FinishedJobDetailsFragmentEventHandlers {
    fun onClickFavorite(view: View)
    fun onClickCancelWorking(view: View)
    fun onClickReport(view: View)
}
