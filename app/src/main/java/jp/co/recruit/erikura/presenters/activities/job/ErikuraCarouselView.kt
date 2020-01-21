package jp.co.recruit.erikura.presenters.activities.job

import TextViewUtil
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobStatus
import jp.co.recruit.erikura.databinding.FragmentCarouselItemBinding
import java.text.SimpleDateFormat
import java.util.*

class ErikuraCarouselViewHolder(private val activity: Activity, val binding: FragmentCarouselItemBinding): RecyclerView.ViewHolder(binding.root) {
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
    var workingFinishAt: TextView = itemView.findViewById(
        R.id.carousel_cell_working_finish_at
    )
    var workingPlace: TextView = itemView.findViewById(
        R.id.carousel_cell_working_place
    )

    fun setup(context: Context, job: Job) {

        TextViewUtil.setupTimeLabel(timeLimit, context, job)
        /*if (job.isPastOrInactive) {
            timeLimit.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.warmGrey
                )
            )
            timeLimit.text = "受付終了"
        }
        else if (job.isFuture) {
            timeLimit.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.waterBlue
                )
            )
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
                sb.setSpan(
                    RelativeSizeSpan(16.0f / 12.0f), start, sb.length,
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
                    RelativeSizeSpan(16.0f / 12.0f), start, sb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                sb.append("時間")
            }
            timeLimit.text = sb
        }
        when(job.status) {
            JobStatus.Working -> {
                timeLimit.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.vibrantGreen
                    )
                )
                timeLimit.text = "作業実施中"
            }
            JobStatus.Finished -> {
                timeLimit.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.vibrantGreen
                    )
                )
                timeLimit.text = "実施済み(未報告)"
            }
            JobStatus.Reported -> {
                timeLimit.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.warmGrey
                    )
                )
                timeLimit.text = "作業報告済み"
            }
            else -> {
                timeLimit.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.coral
                    )
                )
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
                        RelativeSizeSpan(16.0f / 12.0f), start, sb.length,
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
                        RelativeSizeSpan(16.0f / 12.0f), start, sb.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    sb.append("時間")
                }
                timeLimit.text = sb
            }
        }*/

        title.text = job.title
        reward.text = job.fee.toString() + "円"
        workingTime.text = job.workingTime.toString() + "分"
        val sd = SimpleDateFormat("YYYY/MM/dd HH:mm")
        workingFinishAt.text = "〜" + sd.format(job.workingFinishAt)
        workingPlace.text = job.workingPlace

        // ダウンロード
        job.thumbnailUrl?.let { url ->
            val assetsManager = ErikuraApplication.assetsManager

            assetsManager.fetchImage(activity, url) { bitmap ->
                activity.runOnUiThread {
                    image.setImageBitmap(bitmap)
                }
            }
        }
    }
}

class ErikuraCarouselAdapter(val activity: Activity, var data: List<Job>): RecyclerView.Adapter<ErikuraCarouselViewHolder>() {
    var onClickListner: OnClickListener? = null

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

        holder.title.text = job.title
        holder.setup(ErikuraApplication.instance.applicationContext, job)

        holder.binding.root.setOnClickListener {
            onClickListner?.apply {
                onClick(job)
            }
        }
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