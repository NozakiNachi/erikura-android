package jp.co.recruit.erikura.presenters.fragments

import android.app.Activity
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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User
import jp.co.recruit.erikura.databinding.FragmentAppliedJobDetailsBinding
import java.util.*



class AppliedJobDetailsFragment(private val activity: AppCompatActivity, val job: Job?, val user: User) : Fragment() {
    private val viewModel: AppliedJobDetailsFragmentViewModel by lazy {
        ViewModelProvider(this).get(AppliedJobDetailsFragmentViewModel::class.java)
    }

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
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val transaction = childFragmentManager.beginTransaction()
        val jobInfoView = JobInfoViewFragment(job)
        val cancelButton = CancelButtonFragment(job)
        val manualButton = ManualButtonFragment(job)
        val thumbnailImage = ThumbnailImageFragment(job)
        val jobDetailsView = JobDetailsViewFragment(job)
        val mapView = MapViewFragment(activity, job)
        transaction.add(R.id.appliedJobDetails_jobInfoViewFragment, jobInfoView, "jobInfoView")
        transaction.add(R.id.appliedJobDetails_cancelButtonFragment, cancelButton, "cancelButton")
        transaction.add(R.id.appliedJobDetails_manualButtonFragment, manualButton, "manualButton")
        transaction.add(R.id.appliedJobDetails_thumbnailImageFragment, thumbnailImage, "thumbnailImage")
        transaction.add(R.id.appliedJobDetails_jobDetailsViewFragment, jobDetailsView, "jobDetailsView")
        transaction.add(R.id.appliedJobDetails_mapViewFragment, mapView, "mapView")
        transaction.commit()
    }

    private fun updateTimeLimit() {
        val str = SpannableStringBuilder()
        val today = Date().time
        val limit = job?.entry?.limitAt?.time?: 0
        val diff: Int = limit.toInt() - today.toInt()
        if (diff >= 0) {
            val diffHours = diff / (1000 * 60 * 60)
            val diffMinutes = (diff % (1000 * 60 * 60)) / (1000 * 60)

            if (diffMinutes == 0) {
                str.append("あと${diffHours}時間以内\n")
            }else {
                str.append("あと${diffHours}時間${diffMinutes}分以内\n")
            }
            str.setSpan(ForegroundColorSpan(Color.RED), 0, str.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            str.append(ErikuraApplication.instance.getString(R.string.jobDetails_goWorking))
            viewModel.timeLimit.value = str
            viewModel.msgVisibility.value = View.VISIBLE
        }else {
            str.append(ErikuraApplication.instance.getString(R.string.jobDetails_overLimit))
            str.setSpan(R.color.colorAccent, 0, str.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            viewModel.timeLimit.value = str
            viewModel.msgVisibility.value = View.GONE
        }
    }
}

class AppliedJobDetailsFragmentViewModel: ViewModel() {
    val bitmapDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()
    val timeLimit: MutableLiveData<SpannableStringBuilder> = MutableLiveData()
    val msgVisibility: MutableLiveData<Int> = MutableLiveData(View.VISIBLE)

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

        }
    }
}