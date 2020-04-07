package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.business.models.UserSession
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.databinding.FragmentAppliedJobDetailsBinding
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.util.GoogleFitApiManager
import jp.co.recruit.erikura.presenters.util.LocationManager
import java.util.*


class AppliedJobDetailsFragment(
    private val activity: AppCompatActivity,
    val job: Job?,
    val user: User
) : Fragment(), AppliedJobDetailsFragmentEventHandlers {
    private val viewModel: AppliedJobDetailsFragmentViewModel by lazy {
        ViewModelProvider(this).get(AppliedJobDetailsFragmentViewModel::class.java)
    }

    private val fitApiManager: GoogleFitApiManager = ErikuraApplication.fitApiManager
    private val locationManager: LocationManager = ErikuraApplication.locationManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        container?.removeAllViews()
        val binding = FragmentAppliedJobDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = activity
        viewModel.setup(activity, job, user)
        updateTimeLimit()
        binding.viewModel = viewModel
        binding.handlers = this
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val transaction = childFragmentManager.beginTransaction()
        val jobInfoView = JobInfoViewFragment(job)
        val manualImage = ManualImageFragment(job)
        val cancelButton = CancelButtonFragment(job)
        val manualButton = ManualButtonFragment(job)
        val thumbnailImage = ThumbnailImageFragment(job)
        val jobDetailsView = JobDetailsViewFragment(job)
        val mapView = MapViewFragment(activity, job)
        transaction.add(R.id.appliedJobDetails_jobInfoViewFragment, jobInfoView, "jobInfoView")
        transaction.add(R.id.appliedJobDetails_manualImageFragment, manualImage, "manualImage")
        transaction.add(R.id.appliedJobDetails_cancelButtonFragment, cancelButton, "cancelButton")
        transaction.add(R.id.appliedJobDetails_manualButtonFragment, manualButton, "manualButton")
        transaction.add(
            R.id.appliedJobDetails_thumbnailImageFragment,
            thumbnailImage,
            "thumbnailImage"
        )
        transaction.add(
            R.id.appliedJobDetails_jobDetailsViewFragment,
            jobDetailsView,
            "jobDetailsView"
        )
        transaction.add(R.id.appliedJobDetails_mapViewFragment, mapView, "mapView")
        transaction.commit()

        if (!ErikuraApplication.pedometerManager.checkPermission(activity)) {
            ErikuraApplication.pedometerManager.requestPermission(this)
        }
    }

    override fun onStart() {
        super.onStart()
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_job_entried", params= bundleOf())
        Tracking.viewJobDetails(name= "/entries/started/${job?.id ?: 0}", title= "作業開始前画面", jobId= job?.id ?: 0)
    }

    override fun onResume() {
        super.onResume()
        ErikuraApplication.pedometerManager.start()
    }

    override fun onPause() {
        super.onPause()
        ErikuraApplication.pedometerManager.stop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        ErikuraApplication.pedometerManager.onRequestPermissionResult(requestCode, permissions, grantResults)
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

    override fun onClickStart(view: View) {
        job?.let {
            // 作業開始のトラッキングの送出
            Tracking.logEvent(event= "push_start_job", params= bundleOf())
            Tracking.trackJobDetails(name= "push_start_job", jobId= job?.id)

            Api(activity).startJob(it, locationManager.latLng ?: locationManager.latLngOrDefault,
                steps = ErikuraApplication.pedometerManager.readStepCount(),
                distance = null, floorAsc = null, floorDesc = null
            ) {
                val intent= Intent(activity, JobDetailsActivity::class.java)
                intent.putExtra("job", job)
                intent.putExtra("onClickStart", true)
                startActivity(intent)
                activity.finish()
            }
        }
    }

    private fun updateTimeLimit() {
        val str = SpannableStringBuilder()
        val today = Date().time
        val limit = job?.entry?.limitAt?.time ?: 0
        val diff: Int = limit.toInt() - today.toInt()
        if (diff >= 0) {
            val diffHours = diff / (1000 * 60 * 60)
            val diffMinutes = (diff % (1000 * 60 * 60)) / (1000 * 60)

            if (diffHours == 0) {
                str.append("あと${diffMinutes}分以内\n")
            } else if (diffMinutes == 0) {
                str.append("あと${diffHours}時間以内\n")
            } else {
                str.append("あと${diffHours}時間${diffMinutes}分以内\n")
            }
            str.setSpan(
                ForegroundColorSpan(Color.RED),
                0,
                str.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            str.append(ErikuraApplication.instance.getString(R.string.jobDetails_goWorking))
            viewModel.timeLimit.value = str
            viewModel.msgVisibility.value = View.VISIBLE
        } else {
            str.append(ErikuraApplication.instance.getString(R.string.jobDetails_overLimit))
            str.setSpan(ForegroundColorSpan(Color.RED), 0, str.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            viewModel.timeLimit.value = str
            viewModel.msgVisibility.value = View.GONE
            viewModel.startButtonVisibility.value = View.INVISIBLE
        }
    }
}

class AppliedJobDetailsFragmentViewModel : ViewModel() {
    val bitmapDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()
    val timeLimit: MutableLiveData<SpannableStringBuilder> = MutableLiveData()
    val msgVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)
    val favorited: MutableLiveData<Boolean> = MutableLiveData(false)
    val startButtonVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

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

            // お気に入り状態の取得
            UserSession.retrieve()?.let {
                Api(activity).placeFavoriteShow(job.place?.id ?: 0) {
                    favorited.value = it
                }
            }
        }
    }
}

interface AppliedJobDetailsFragmentEventHandlers {
    fun onClickFavorite(view: View)
    fun onClickStart(view: View)
}