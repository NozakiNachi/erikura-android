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
    fun setupTimeLabel(textView: TextView, context: Context, job: Job?) {
        // 受付終了：応募済みの場合、now > working_finish_at の場合, gray, 12pt
        // 作業実施中: working 状態の場合, green, 12pt
        // 実施済み(未報告): finished の場合, green, 12pt
        // 作業報告済み: reported の場合, gray, 12pt
        // 募集開始までn日とn時間: 開始前(now < working_start_at)、
        // 作業終了までn日とn時間
        if (job != null) {
            if (job.isPastOrInactive) {
                textView.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.waterBlue
                    )
                )
                textView.text = "受付終了"     // FIXME: リソース化
            }
            else if (job.isFuture) {
                textView.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.waterBlue
                    )
                )
                val now = Date()
                val workingStartAt = job.workingStartAt ?: now
                val diff = workingStartAt.time - now.time
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
                textView.text = sb
            }
            when(job.status) {
                JobStatus.Working -> {
                    textView.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.vibrantGreen
                        )
                    )
                    textView.text = "作業実施中"
                }
                JobStatus.Finished -> {
                    textView.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.vibrantGreen
                        )
                    )
                    textView.text = "実施済み(未報告)"
                }
                JobStatus.Reported -> {
                    textView.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.warmGrey
                        )
                    )
                    textView.text = "作業報告済み"
                }
                else -> {
                    textView.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.coral
                        )
                    )
                    val now = Date()
                    val workingFinishAt = job.workingFinishAt ?: now
                    val diff = workingFinishAt.time - now.time
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
                    textView.text = sb
                }
            }
        }else {
            textView.text = ""
        }
    }
}