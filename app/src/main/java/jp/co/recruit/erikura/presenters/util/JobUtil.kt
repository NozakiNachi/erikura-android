import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobStatus
import java.util.*

object JobUtil {
    enum class TimeLabelType {
        SEARCH,
        OWNED,
    }


    fun setupTimeLabel(context: Context, job: Job?, type: TimeLabelType = TimeLabelType.SEARCH): Pair<SpannableStringBuilder, Int> {
        // 受付終了：応募済みの場合、now > working_finish_at の場合, gray, 12pt
        // 作業実施中: working 状態の場合, green, 12pt
        // 実施済み(未報告): finished の場合, green, 12pt
        // 作業報告済み: reported の場合, gray, 12pt
        // 募集開始までn日とn時間: 開始前(now < working_start_at)、
        // 作業終了までn日とn時間
        var text: SpannableStringBuilder = SpannableStringBuilder().apply { append("") }
        var color: Int = ContextCompat.getColor(context, R.color.warmGrey)
        if (job != null) {
            if (job.isPast) {
                color = ContextCompat.getColor(context, R.color.warmGrey)
                text = SpannableStringBuilder().apply {
                    append("受付終了")
                }
            }
            else if (type == TimeLabelType.SEARCH && job.isPastOrInactive && !job.isOwner) {
                color = ContextCompat.getColor(context, R.color.warmGrey)
                text = SpannableStringBuilder().apply {
                    append("受付終了")
                }
            }
            else if (job.isFuture) {
                color = ContextCompat.getColor(context, R.color.waterBlue)
                val now = Date()
                val workingStartAt = job.workingStartAt ?: now
                val diff = workingStartAt.time - now.time
                val diffHours = diff / (60 * 60 * 1000)
                val diffDays = diffHours / 24
                val diffRestHours = diffHours % 24
                val diffRestMinutes = (diff / (60 * 1000)) % 60

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
                if (diffDays <= 0 && diffRestHours <= 0) {
                    val start = sb.length
                    sb.append(diffRestMinutes.toString())
                    sb.setSpan(
                        RelativeSizeSpan(16.0f / 12.0f), start, sb.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    sb.append("分")
                }
                text = sb
            }
            else {
                when(job.status) {
                    JobStatus.Working -> {
                        color = ContextCompat.getColor(context, R.color.vibrantGreen)
                        text = SpannableStringBuilder().apply {
                            append("作業実施中")
                        }
                    }
                    JobStatus.Finished -> {
                        color = ContextCompat.getColor(context, R.color.vibrantGreen)
                        text = SpannableStringBuilder().apply {
                            append("実施済み(未報告)")
                        }
                    }
                    JobStatus.Reported -> {
                        color = ContextCompat.getColor(context, R.color.warmGrey)
                        text = SpannableStringBuilder().apply {
                            append("作業報告済み")
                        }
                    }
                    else -> {
                        color = ContextCompat.getColor(context, R.color.coral)
                        val now = Date()
                        val workingFinishAt = if (job.status == JobStatus.Applied) { job.entry?.limitAt ?: now } else { job.workingFinishAt ?: now }
                        val diff = workingFinishAt.time - now.time
                        val diffHours = diff / (60 * 60 * 1000)
                        val diffDays = diffHours / 24
                        val diffRestHours = diffHours % 24
                        val diffRestMinutes = (diff / (60 * 1000)) % 60

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
                        if (diffDays <= 0 && diffRestHours <= 0) {
                            val start = sb.length
                            sb.append(diffRestMinutes.toString())
                            sb.setSpan(
                                RelativeSizeSpan(16.0f / 12.0f), start, sb.length,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            sb.append("分")
                        }
                        text = sb
                    }
                }
            }
        }

        return text to color
    }
}