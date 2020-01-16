package jp.co.recruit.erikura.presenters.activities.job

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.marginTop
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobStatus
import jp.co.recruit.erikura.databinding.DialogJobSelectBinding
import jp.co.recruit.erikura.databinding.FragmentCarouselItemBinding
import jp.co.recruit.erikura.databinding.FragmentJobListItemBinding
import java.text.SimpleDateFormat
import java.util.*

class JobSelectDialogFragment(val jobs: List<Job>): DialogFragment(), JobSelectDialogHandler {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(activity!!)?.also { dialog ->

            dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)

            val binding = DataBindingUtil.inflate<DialogJobSelectBinding>(
                LayoutInflater.from(activity),
                R.layout.dialog_job_select,
                null,
                false
            )
            binding.lifecycleOwner = activity
            binding.handler = this

            dialog.setContentView(binding.root)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val metrics = resources.displayMetrics

//            dialog.window?.setLayout(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                metrics.heightPixels - (260 * metrics.density).toInt())

            dialog.setCanceledOnTouchOutside(true)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dialog?.window?.let { window ->

            val lp = window.attributes
            lp.gravity = Gravity.BOTTOM
            window.attributes = lp

            val adapter = JobListAdapter(this@JobSelectDialogFragment.activity!!, jobs, null)
//            adapter.onClickListner = object: ErikuraCarouselAdaptor.OnClickListener {
//                override fun onClick(job: Job) {
//                    // FIXME: 実装
//                }
//            }

            val recyclerView: RecyclerView = window.findViewById(R.id.job_select_dialog_recycler_view)
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = adapter
            recyclerView.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            recyclerView.addItemDecoration(JobListItemDecorator())
        }
    }

    override fun onClickClose(view: View) {
        dialog?.dismiss()
    }
}

interface JobSelectDialogHandler {
    fun onClickClose(view: View)
}

class JobListAdapter(private val activity: FragmentActivity, private val jobs: List<Job>, val currentPosition: LatLng?) : RecyclerView.Adapter<JobListHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobListHolder {
        val binding = DataBindingUtil.inflate<FragmentJobListItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.fragment_job_list_item,
            parent,
            false
        )

        return JobListHolder(binding)
    }

    override fun getItemCount(): Int {
        return jobs.count()
    }

    override fun onBindViewHolder(holder: JobListHolder, position: Int) {
        holder.binding.viewModel = JobListItemViewModel(activity, jobs[position], currentPosition)
        holder.binding.lifecycleOwner = activity
    }
}

class JobListHolder(val binding: FragmentJobListItemBinding) : RecyclerView.ViewHolder(binding.root)

class JobListItemViewModel(activity: Activity, val job: Job, val currentPosition: LatLng?): ViewModel() {
    val assetsManager = ErikuraApplication.instance.erikuraComponent.assetsManager()
    val resources = activity.resources
    val dateFormat = SimpleDateFormat("YYYY/MM/dd HH:mm")

    val reward: String get() = String.format("%,d円", job.fee)
    val workingTime: String get() = String.format("%d分", job.workingTime)
    val workingFinishAt: String get() = String.format("〜%s", dateFormat.format(job.workingFinishAt))

    val image: MutableLiveData<Bitmap> = MutableLiveData()
    val textColor: MutableLiveData<Int> = MutableLiveData()
    val timeLimit: MutableLiveData<SpannableStringBuilder> = MutableLiveData()
    val distance: MutableLiveData<SpannableStringBuilder> = MutableLiveData()

    init {
        if (job.isPastOrInactive) {
            textColor.value = ContextCompat.getColor(activity, R.color.warmGrey)
            timeLimit.value = SpannableStringBuilder().apply {
                append("受付終了")
            }
        }
        else if (job.isFuture) {
            textColor.value = ContextCompat.getColor(activity, R.color.waterBlue)
            val now = Date()
            val diff = job.workingStartAt.time - now.time
            val diffHours = diff / (60 * 60 * 1000)
            val diffDays = diffHours / 24
            val diffRestHours = diffHours % 24

            val sb = SpannableStringBuilder()
            sb.append("募集開始まで")
            if (diffDays > 0) {
                val start = sb.length
                sb.append(diffDays.toString())
                sb.setSpan(RelativeSizeSpan(16.0f / 12.0f), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.append("日")
            }
            if (diffDays > 0 && diffRestHours > 0) {
                sb.append("と")
            }
            if (diffRestHours > 0) {
                val start = sb.length
                sb.append(diffRestHours.toString())
                sb.setSpan(RelativeSizeSpan(16.0f / 12.0f), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.append("時間")
            }
            timeLimit.value = sb
        }
        else {
            when(job.status) {
                JobStatus.Working -> {
                    textColor.value = ContextCompat.getColor(activity, R.color.vibrantGreen)
                    timeLimit.value = SpannableStringBuilder().apply {
                        append("作業実施中")
                    }
                }
                JobStatus.Finished -> {
                    textColor.value = ContextCompat.getColor(activity, R.color.vibrantGreen)
                    timeLimit.value = SpannableStringBuilder().apply {
                        append("実施済み(未報告)")
                    }
                }
                JobStatus.Reported -> {
                    textColor.value = ContextCompat.getColor(activity, R.color.warmGrey)
                    timeLimit.value = SpannableStringBuilder().apply {
                        append("作業報告済み")
                    }
                }
                else -> {
                    textColor.value = ContextCompat.getColor(activity, R.color.coral)
                    val now = Date()
                    val diff = job.workingFinishAt.time - now.time
                    val diffHours = diff / (60 * 60 * 1000)
                    val diffDays = diffHours / 24
                    val diffRestHours = diffHours % 24

                    val sb = SpannableStringBuilder()
                    sb.append("作業終了まで")
                    if (diffDays > 0) {
                        val start = sb.length
                        sb.append(diffDays.toString())
                        sb.setSpan(
                            RelativeSizeSpan(16.0f / 12.0f),
                            start,
                            sb.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        sb.append("日")
                    }
                    if (diffDays > 0 && diffRestHours > 0) {
                        sb.append("と")
                    }
                    if (diffRestHours > 0) {
                        val start = sb.length
                        sb.append(diffRestHours.toString())
                        sb.setSpan(
                            RelativeSizeSpan(16.0f / 12.0f),
                            start,
                            sb.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        sb.append("時間")
                    }
                    timeLimit.value = sb
                }
            }
        }

        currentPosition?.let {
            val dist = SphericalUtil.computeDistanceBetween(job.latLng, it)
            val sb = SpannableStringBuilder()
            sb.append("検索地点より")

            val start = sb.length
            sb.append(String.format("%,dm", dist.toInt()))
            sb.setSpan(
                RelativeSizeSpan(16.0f / 12.0f),
                start,
                sb.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            distance.value = sb
        }

        job.thumbnailUrl?.let { url ->
            assetsManager.fetchImage(activity, url) { bitmap ->
                image.value = bitmap
            }
        }
    }
}


class JobListItemDecorator: RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.top = view.resources.getDimensionPixelSize(R.dimen.job_list_item_margin)
        outRect.bottom = view.resources.getDimensionPixelSize(R.dimen.job_list_item_margin)
    }
}
