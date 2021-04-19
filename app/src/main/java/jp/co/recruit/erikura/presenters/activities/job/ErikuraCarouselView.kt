package jp.co.recruit.erikura.presenters.activities.job

import JobUtil
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.databinding.FragmentCarouselItemBinding
import jp.co.recruit.erikura.presenters.util.setOnSafeClickListener
import java.io.File

class ErikuraCarouselViewHolder(private val activity: Activity, val binding: FragmentCarouselItemBinding): RecyclerView.ViewHolder(binding.root) {
    var currentPosition: Int = -1

    var timeLimit: TextView = itemView.findViewById(
        R.id.carousel_cell_timelimit
    )
    var title: TextView = itemView.findViewById(R.id.carousel_cell_title)
    var image: ImageView = itemView.findViewById(
        R.id.carousel_cell_image
    )
    var reward: TextView = itemView.findViewById(
        R.id.carousel_cell_reward
    )
    var workingTime: TextView = itemView.findViewById(
        R.id.carousel_cell_working_time
    )
    var workingStartAt: TextView = itemView.findViewById(
        R.id.carousel_cell_working_start_at
    )
    var workingFinishAt: TextView = itemView.findViewById(
        R.id.carousel_cell_working_finish_at
    )
    var workingPlace: TextView = itemView.findViewById(
        R.id.carousel_cell_working_place
    )

    fun setup(context: Context, job: Job) {
        val position = this.currentPosition
        val (timeLimitText, timeLimitColor) = JobUtil.setupTimeLabel(context, job)
        timeLimit.text = timeLimitText
        timeLimit.setTextColor(timeLimitColor)

        title.text = job.title
        reward.text = job.fee.toString() + "円"
        workingTime.text = job.workingTime.toString() + "分"
        if (job.isPreEntry) {
            workingStartAt.text = job.workingStartAt?.let { JobUtil.getFormattedDateWithoutYear(it) } ?: ""
            //　先行応募中の場合作業開始日時の1日後
            workingFinishAt.text = job.workingStartAt?.let { " 〜 " + JobUtil.getFormattedDateWithoutYear(JobUtil.preEntryWorkingLimitAt(it)) } ?: ""
        } else {
            workingStartAt.text = job.workingStartAt?.let { JobUtil.getFormattedDate(it) } ?: ""
            workingFinishAt.text = job.workingFinishAt?.let { " 〜 " + JobUtil.getFormattedDate(it) } ?: ""
        }
        workingPlace.text = job.workingPlace

        // ダウンロード
        val thumbnailUrl = if (!job.thumbnailUrl.isNullOrBlank()) {job.thumbnailUrl}else {job.jobKind?.noImageIconUrl?.toString()}

        val imageView: ImageView = itemView.findViewById(R.id.carousel_cell_image)
        imageView.setImageDrawable(ErikuraApplication.instance.applicationContext.resources.getDrawable(R.drawable.ic_noimage, null))

        if (!thumbnailUrl.isNullOrBlank()) {
            val assetsManager = ErikuraApplication.assetsManager
            assetsManager.fetchAsset(activity, thumbnailUrl) { asset ->
                // 画像取得中に別のカルーセルに移動する可能性があるので、position が一致していることを確認する
                if (position == this.currentPosition && ErikuraApplication.instance.isEnableActivity(activity)) {
                    Glide.with(activity).load(File(asset.path)).fitCenter().into(imageView)
                }
            }
        }
    }
}

class ErikuraCarouselViewModel(val job: Job, val jobsByLocation: Map<LatLng, List<Job>>): ViewModel() {
    private val jobsCountAt: Int get() = jobsByLocation[job.latLng]?.size ?: 0
    val hasOtherJobs: Boolean get() = jobsCountAt > 1
    val jobsCountText: String get() = String.format("ほか%d件のお仕事", jobsCountAt - 1)
    val jobsCountTextVisibility: Int get() = if(hasOtherJobs) { View.VISIBLE } else { View.GONE }
    val disabled: Boolean get() = job.isFuture && !(job.isPreEntry) || job.isPastOrInactive
    val bodyBackgroundDrawable: Drawable
        get() {
            return if (hasOtherJobs) {
                if(disabled) {
                    ErikuraApplication.applicationContext.resources.getDrawable(R.drawable.background_carousel_body_multi_disabled, null)
                }
                else {
                    ErikuraApplication.applicationContext.resources.getDrawable(R.drawable.background_carousel_body_multi, null)
                }
            }
            else {
                if(disabled) {
                    ErikuraApplication.applicationContext.resources.getDrawable(R.drawable.background_carousel_body_disabled, null)

                }
                else {
                    ErikuraApplication.applicationContext.resources.getDrawable(R.drawable.background_carousel_body, null)
                }
            }
        }
}

class ErikuraCarouselAdapter(val activity: FragmentActivity, val carousel: RecyclerView, var data: List<Job>, var jobsByLocation: Map<LatLng, List<Job>>): RecyclerView.Adapter<ErikuraCarouselViewHolder>() {
    var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ErikuraCarouselViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding: FragmentCarouselItemBinding =
            FragmentCarouselItemBinding.inflate(
                layoutInflater,
                parent,
                false
            )

        return ErikuraCarouselViewHolder(
            activity,
            binding
        )
    }

    override fun onBindViewHolder(holder: ErikuraCarouselViewHolder, position: Int) {
        val job = data[position]
        val viewModel = ErikuraCarouselViewModel(job, jobsByLocation)

        holder.currentPosition = position
        holder.binding.lifecycleOwner = activity
        holder.binding.viewModel = viewModel
        holder.title.text = job.title
        holder.setup(ErikuraApplication.instance.applicationContext, job)

        holder.binding.executePendingBindings()

        val view = holder.binding.root
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        Log.v(ErikuraApplication.LOG_TAG, "Resizing: otherJobs=${viewModel.hasOtherJobs}, height=${view.measuredHeight}")

        view.setOnSafeClickListener {
            onClickListener?.apply {
                onClick(job)
            }
        }
        view.requestLayout()
        carousel.requestLayout()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    interface OnClickListener {
        fun onClick(job: Job)
    }
}

class ErikuraCarouselCellDecoration: RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.left = view.resources.getDimensionPixelSize(R.dimen.carousel_cell_spacing)
        outRect.right = view.resources.getDimensionPixelSize(R.dimen.carousel_cell_spacing)
    }
}