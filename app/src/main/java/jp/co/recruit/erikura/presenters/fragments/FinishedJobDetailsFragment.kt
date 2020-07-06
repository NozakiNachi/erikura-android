package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentFinishedJobDetailsBinding
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.report.ReportImagePickerActivity
import jp.co.recruit.erikura.presenters.view_models.BaseJobDetailViewModel
import java.util.*

class FinishedJobDetailsFragment : BaseJobDetailFragment, FinishedJobDetailsFragmentEventHandlers {
    companion object {
        fun newInstance(job: Job?, user: User?, cautionsCount: Int?): FinishedJobDetailsFragment {
            val args = Bundle()
            fillArguments(args, job, user, cautionsCount)

            return FinishedJobDetailsFragment().also {
                it.arguments = args
            }
        }
    }

    private val viewModel by lazy {
        ViewModelProvider(this).get(FinishedJobDetailsFragmentViewModel::class.java)
    }
    private var jobInfoView: JobInfoViewFragment? = null
    private var manualImage: ManualImageFragment? = null
    private var manualButton: ManualButtonFragment? = null
    private var thumbnailImage: ThumbnailImageFragment? = null
    private var jobDetailsView: JobDetailsViewFragment? = null
    private var mapView: MapViewFragment? = null
    private var propertyNotesButtonButton: PropertyNotesButtonButtonFragment? = null

    constructor(): super()

    override fun refresh(job: Job?, user: User?) {
        super.refresh(job, user)

        if (isAdded) {
            jobInfoView?.refresh(job, user)
            manualImage?.refresh(job, user)
            manualButton?.refresh(job, user)
            thumbnailImage?.refresh(job, user)
            jobDetailsView?.refresh(job, user)
            mapView?.refresh(job, user)

            activity?.let {
                viewModel.setup(it, job, user)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding = FragmentFinishedJobDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        viewModel.setup(activity!!, job, user)
        binding.viewModel = viewModel
        binding.handlers = this
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val transaction = childFragmentManager.beginTransaction()
        jobInfoView = JobInfoViewFragment.newInstance(job, user)
        manualImage = ManualImageFragment.newInstance(job, user)
        manualButton = ManualButtonFragment.newInstance(job, user)
        thumbnailImage = ThumbnailImageFragment.newInstance(job, user)
        jobDetailsView = JobDetailsViewFragment.newInstance(job, user)
        mapView = MapViewFragment.newInstance(job, user)
        propertyNotesButtonButton = PropertyNotesButtonButtonFragment.newInstance(job, user, cautionsCount)
        transaction.add(R.id.finishedJobDetails_jobInfoViewFragment, jobInfoView!!, "jobInfoView")
        transaction.add(R.id.finishedJobDetails_manualImageFragment, manualImage!!, "manualImage")
        transaction.add(R.id.finishedJobDetails_manualButtonFragment, manualButton!!, "manualButton")
        transaction.add(R.id.finishedJobDetails_thumbnailImageFragment, thumbnailImage!!, "thumbnailImage")
        transaction.add(R.id.finishedJobDetails_jobDetailsViewFragment, jobDetailsView!!, "jobDetailsView")
        transaction.add(R.id.finishedJobDetails_mapViewFragment, mapView!!, "mapView")
        transaction.add(R.id.jobDetails_propertyNotesButtonFragment, propertyNotesButtonButton!!, "propertyNotesButton")
        transaction.commitAllowingStateLoss()
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_job_finished", params= bundleOf())
        Tracking.viewJobDetails(name= "/entries/finished/${job?.id ?: 0}", title= "作業完了画面", jobId= job?.id ?: 0)
    }

    override fun onClickFavorite(view: View) {
        job?.place?.id?.let { placeId ->
            // 現在のボタン状態を取得します
            val favorited = viewModel.favorited.value ?: false

            val favoriteButton: ToggleButton = this.view?.findViewById(R.id.favorite_button)!!
            // タップが聞かないのように無効化をします
            favoriteButton.isEnabled = false
            val api = Api(activity!!)
            val errorHandler: (List<String>?) -> Unit = { messages ->
                api.displayErrorAlert(messages)
                favoriteButton.isEnabled = true
            }
            if (favorited) {
                // ボタンがお気に入り状態なので登録処理
                api.placeFavorite(placeId, onError = errorHandler) {
                    viewModel.favorited.value = true
                    favoriteButton.isEnabled = true
                }
            }
            else {
                // お気に入り削除処理
                api.placeFavoriteDelete(placeId, onError = errorHandler) {
                    viewModel.favorited.value = false
                    favoriteButton.isEnabled = true
                }
            }
        }
    }

    override fun onClickCancelWorking(view: View) {
        job?.let { job ->
            if (job.entry?.limitAt?: Date() > Date()) {
                Api(activity!!).abortJob(job) {
                    val intent = Intent(activity, JobDetailsActivity::class.java)
                    intent.putExtra("job", job)
                    intent.putExtra("onClickCancelWorking", true)
                    startActivity(intent)
                }
            }else {
                val errorMessages = mutableListOf(ErikuraApplication.instance.getString(R.string.jobDetails_overLimit))
                Api(activity!!).displayErrorAlert(errorMessages)
            }
        }
    }

    override fun onClickReport(view: View) {
        job?.let { job ->
            if (job.entry?.limitAt?: Date() > Date()) {
                val intent = Intent(activity, ReportImagePickerActivity::class.java)
                intent.putExtra("job", job)
                startActivity(intent)
            }else {
                val errorMessages =
                    mutableListOf(ErikuraApplication.instance.getString(R.string.jobDetails_overLimit))
                Api(activity!!).displayErrorAlert(errorMessages)
            }
        }
    }

}

class FinishedJobDetailsFragmentViewModel: BaseJobDetailViewModel() {
    val bitmapDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()
    val favorited: MutableLiveData<Boolean> = MutableLiveData(false)
    val reportButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

    fun setup(activity: Activity, job: Job?, user: User?) {
        this.job.value = job
        this.user.value = user

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
                reportButtonVisibility.value = View.INVISIBLE
            }

            // お気に入り状態の取得
            if (Api.isLogin) {
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
