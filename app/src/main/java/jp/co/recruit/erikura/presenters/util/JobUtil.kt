import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore
import android.text.*
import android.text.style.*
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.Tracking
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.JobStatus
import jp.co.recruit.erikura.business.models.ReportExample
import jp.co.recruit.erikura.business.util.JobUtils
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.storage.Asset
import jp.co.recruit.erikura.data.storage.ReportDraft
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.report.*
import okhttp3.internal.closeQuietly
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object JobUtil {
    enum class TimeLabelType {
        SEARCH,
        OWNED,
    }

    fun setupTimeLabel(context: Context, job: Job?, type: TimeLabelType = TimeLabelType.SEARCH, fromJobList: Boolean = false): Pair<SpannableStringBuilder, Int> {
        // 受付終了：応募済みの場合、now > working_finish_at の場合, gray, 12pt
        // 作業実施中: working 状態の場合, green, 12pt
        // 実施済み(未報告): finished の場合, green, 12pt
        // 作業報告済み: reported の場合, gray, 12pt
        // 募集開始までn日とn時間: 開始前(now < working_start_at)、
        // 作業終了報告までn日とn時間
        var text: SpannableStringBuilder = SpannableStringBuilder().apply { append("") }
        var color: Int = ContextCompat.getColor(context, R.color.warmGrey)
        if (job != null) {
            if (job.isPast) {
                color = ContextCompat.getColor(context, R.color.warmGrey)
                text = if (job.isOwner && job.status == JobStatus.Reported) {
                    SpannableStringBuilder().apply {
                        append("作業報告済み")
                    }
                }else {
                    SpannableStringBuilder().apply {
                        append("受付終了")
                    }
                }
            }
            else if (type == TimeLabelType.SEARCH && job.isPastOrInactive) {
                color = ContextCompat.getColor(context, R.color.warmGrey)
                text = if (job.isOwner && job.status == JobStatus.Reported) {
                    SpannableStringBuilder().apply {
                        append("作業報告済み")
                    }
                }else {
                    SpannableStringBuilder().apply {
                        append("受付終了")
                    }
                }
            }
            else if (job.isFuture && !(job.isPastOrInactive)) {
                color = ContextCompat.getColor(context, R.color.waterBlue)
                val now = Date()
                if (job.isPreEntry) {
                    //先行応募の場合
                    text = SpannableStringBuilder().apply {
                        val workingStartAt = job.workingStartAt ?: now
                        val finishAt = preEntryWorkingLimitAt(workingStartAt)
                        val sdf = SimpleDateFormat("MM/dd")
                        if (!(fromJobList)) {
                            ResourcesCompat.getFont(context, R.font.fa_regular_400)?.let { fasFont ->
                                appendStringWithFont(this, "\uf058 ", "fas", fasFont)
                            }
                            append("先行応募可　")
                        }
                        append("作業日：")
                        appendStringAsLarge(this, "${sdf.format(workingStartAt)} 〜 ${sdf.format(finishAt)}" ?: "")
                    }
                } else {
                    if (job.isBeforePreEntry) {
                        // 先行応募予定の場合
                        text = SpannableStringBuilder().apply {
                            ResourcesCompat.getFont(context, R.font.fa_solid_900)?.let { fasFont ->
                                appendStringWithFont(this, "\uf058 ", "fas", fasFont)
                            }
                            val preEntryStartAt = job.preEntryStartAt ?: now
                            val (days, hours, minutes, seconds) = timeDiff(
                                from = now,
                                to = preEntryStartAt
                            )

                            append("先行応募開始まで")
                            if (days > 0) {
                                appendStringAsLarge(this, days.toString())
                                appendStringAsNormal(this, "日")
                            }
                            if (days > 0 && hours > 0) {
                                appendStringAsNormal(this, "と")
                            }
                            if (hours > 0) {
                                appendStringAsLarge(this, hours.toString())
                                appendStringAsNormal(this, "時間")
                            } else {
                                appendStringAsLarge(this, minutes.toString())
                                appendStringAsNormal(this, "分")
                            }
                        }
                    } else {
                        // 募集予定の場合
                        val workingStartAt = job.workingStartAt ?: now
                        val (days, hours, minutes, seconds) = timeDiff(from= now, to= workingStartAt)

                        val sb = SpannableStringBuilder()
                        sb.append("募集開始まで")
                        if (days > 0) {
                            appendStringAsLarge(sb, days.toString())
                            appendStringAsNormal(sb, "日")
                        }
                        if (days > 0 && hours > 0) {
                            appendStringAsNormal(sb, "と")
                        }
                        if (hours > 0) {
                            appendStringAsLarge(sb, hours.toString())
                            appendStringAsNormal(sb, "時間")
                        }
                        else {
                            appendStringAsLarge(sb, minutes.toString())
                            appendStringAsNormal(sb, "分")
                        }
                        text = sb
                    }
                }
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
                        if (type == TimeLabelType.OWNED && job.isPreEntriedWithinnPreEntryPeriod) {
                            color = ContextCompat.getColor(context, R.color.waterBlue)
                            text = SpannableStringBuilder().apply {
                                ResourcesCompat.getFont(context, R.font.fa_solid_900)?.let { fasFont ->
                                    appendStringWithFont(this, "\uf058 ", "fas", fasFont)
                                }
                                val sdfDate = SimpleDateFormat("MM/dd")
                                val sdfWeekDay = "(%s) "
                                append("先行応募済 作業日: ")
                                appendStringAsLarge(this, job?.workingStartAt?.let { sdfDate.format(it) } ?: "")
                                appendStringAsLarge(this, formatWeekDay(job?.workingStartAt?: Date())?.let { String.format(sdfWeekDay, it) } ?: "")
                                append(" 〜 ")
                                val finishAt = preEntryWorkingLimitAt(job?.workingStartAt?: Date())
                                appendStringAsLarge(this, finishAt.let { sdfDate.format(it) } ?: "")
                                appendStringAsLarge(this, formatWeekDay(finishAt)?.let { String.format(sdfWeekDay, it) } ?: "")
                            }
                        }
                        else {
                            color = ContextCompat.getColor(context, R.color.coral)
                            val now = Date()
                            val workingFinishAt = if (job.status == JobStatus.Applied) { job.entry?.limitAt ?: now } else { job.workingFinishAt ?: now }
                            val (days, hours, minutes, seconds) = timeDiff(from= now, to= workingFinishAt)

                            val sb = SpannableStringBuilder()
                            sb.append("作業終了報告まで")
                            if (days > 0) {
                                appendStringAsLarge(sb, days.toString())
                                appendStringAsNormal(sb, "日")
                            }
                            if (days > 0 && hours > 0) {
                                appendStringAsNormal(sb, "と")
                            }
                            if (hours > 0) {
                                appendStringAsLarge(sb, hours.toString())
                                appendStringAsNormal(sb, "時間")
                            }
                            else {
                                appendStringAsLarge(sb, minutes.toString())
                                appendStringAsNormal(sb, "分")
                            }
                            text = sb
                        }
                    }
                }
            }
        }

        return text to color
    }

    fun openCreateReport(activity: FragmentActivity, job: Job) {
        if (activity.isFinishing == true || activity.isDestroyed == true) {
            // Activity が存在しない状態なので、何もせずに終了します
            return
        }

        if (job.entry?.limitAt?: Date() > Date()) {
            val draft = JobUtils.loadReportDraft(job)
            if (draft != null) {
                val dialog = AlertDialog.Builder(activity)
                    .setView(R.layout.dialog_report_draft)
                    .setCancelable(true)
                    .create()
                dialog.show()
                // 再開ボタン
                (dialog.findViewById(R.id.report_draft_restart) as? Button)?.apply {
                    setOnClickListener {
                        dialog.dismiss()
                        job.report = draft.restoreReport()
                        ErikuraApplication.instance.currentJob = job

                        when(draft.step) {
                            ReportDraft.ReportStep.PictureSelectForm -> {
                                val intent= Intent(activity, ReportFormActivity::class.java)
                                intent.putExtra("job", job)
                                intent.putExtra("pictureIndex", 0)
                                activity.startActivity(intent)
                            }
                            ReportDraft.ReportStep.SummaryForm -> {
                                val intent= Intent(activity, ReportFormActivity::class.java)
                                intent.putExtra("job", job)
                                intent.putExtra("pictureIndex", draft.lastSummaryIndex)
                                activity.startActivity(intent)
                            }
                            ReportDraft.ReportStep.WorkingTimeForm -> {
                                val intent= Intent(activity, ReportWorkingTimeActivity::class.java)
                                intent.putExtra("job", job)
                                activity.startActivity(intent)
                            }
                            ReportDraft.ReportStep.OtherForm -> {
                                val intent= Intent(activity, ReportOtherFormActivity::class.java)
                                intent.putExtra("job", job)
                                activity.startActivity(intent)
                            }
                            ReportDraft.ReportStep.EvaluationForm -> {
                                val intent= Intent(activity, ReportEvaluationActivity::class.java)
                                intent.putExtra("job", job)
                                activity.startActivity(intent)
                            }
                            ReportDraft.ReportStep.Confirm -> {
                                val intent= Intent(activity, ReportConfirmActivity::class.java)
                                intent.putExtra("job", job)
                                activity.startActivity(intent)
                            }
                        }
                    }
                }
                // 最初からボタン
                (dialog.findViewById(R.id.report_draft_reset) as? Button)?.apply {
                    setOnClickListener {
                        dialog.dismiss()
                        JobUtils.removeReportDraft(job)
                        val intent = Intent(activity, ReportImagePickerActivity::class.java)
                        intent.putExtra("job", job)
                        activity.startActivity(intent)
                    }
                }
            }
            else {
                val intent = Intent(activity, ReportImagePickerActivity::class.java)
                intent.putExtra("job", job)
                activity.startActivity(intent)
            }
        } else {
            val errorMessages =
                mutableListOf(ErikuraApplication.instance.getString(R.string.jobDetails_overLimit))
            Api(activity).displayErrorAlert(errorMessages)
        }
    }

    fun openManual(activity: FragmentActivity, job: Job) {
        // ページ参照のトラッキングの送出
        Tracking.logEvent(event= "view_job_manual", params= bundleOf())
        Tracking.viewJobDetails(name= "/jobs/manual", title= "マニュアル表示", jobId= job.id)

        val manualUrl = job.manualUrl
        val assetsManager = ErikuraApplication.assetsManager
        assetsManager.fetchAsset(activity!!, manualUrl!!, Asset.AssetType.Pdf) { asset ->
            // PDFディレクトリにコピーします
            val filesDir = activity.filesDir
            val pdfDir = File(filesDir, "pdfs")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            val pdfFile = File(pdfDir, "manual.pdf")
            val out = FileOutputStream(pdfFile)
            val input = FileInputStream(File(asset.path))
            IOUtils.copy(input, out)
            out.closeQuietly()
            input.closeQuietly()

            try {
                val uri = FileProvider.getUriForFile(
                    activity!!,
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    pdfFile
                )
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(
                    uri,
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf")
                )
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                activity.startActivity(intent)
            }
            catch (e: ActivityNotFoundException) {
                Api(activity).displayErrorAlert(listOf("PDFビューワーが見つかりません。\nPDFビューワーアプリをインストールしてください。"))
            }
        }
    }

    fun openReportExample(activity: FragmentActivity, job: Job) {
        val api = Api(activity)
        var reportExamples: List<ReportExample>? = null
        job.jobKind?.id?.let { jobKindId ->
            //APIでお手本報告を取得する
            api.goodExamples(job.placeId, jobKindId, true) { listReportExamples ->
                reportExamples = listReportExamples
                //トラッキングの送出、お手本報告画面の表示
                Tracking.logEvent(event = "view_good_examples", params = bundleOf())
                Tracking.viewGoodExamples(
                    name = "/places/good_examples",
                    title = "お手本報告画面表示",
                    jobId = job.id,
                    jobKindId = jobKindId,
                    placeId = job.placeId
                )
                val intent = Intent(activity, ReportExamplesActivity::class.java)
                intent.putExtra("job", job)
                intent.putParcelableArrayListExtra( "reportExamples", ArrayList(reportExamples ?: listOf()))
                activity.startActivity(intent)
            }
        }
    }

    data class DiffPart(
        val days: Int,
        val hours: Int,
        val minutes: Int,
        val seconds: Int
    )
    /**
     * 時間の差分をとり、日数、時間、分、秒の値を返します
     */
    private fun timeDiff(from: Date, to: Date): DiffPart {
        val diffInMillis = to.time - from.time      // 時刻差分(ミリ秒単位)
        val diffInSeconds = diffInMillis / 1000     // 時刻差分(秒単位)
        val diffInMinutes = diffInSeconds / 60      // 時刻差分(分単位)
        val diffInHours = diffInMinutes / 60        // 時刻差分(時単位)
        val diffInDays = diffInHours / 24           // 時刻差分(日単位)
        val secDiff = diffInSeconds % 60            // 差分(秒部分)
        val minDiff = diffInMinutes % 60            // 差分(分部分)
        val hourDiff = diffInHours % 24             // 差分(時部分)
        val dayDiff = diffInDays                    // 差分(日部分)

        return DiffPart(
            days = dayDiff.toInt(), hours = hourDiff.toInt(),
            minutes = minDiff.toInt(), seconds = secDiff.toInt())
    }

    fun appendStringAsNormal(sb: SpannableStringBuilder, str: String) {
        sb.append(str)
    }

    fun appendStringAsLarge(sb: SpannableStringBuilder, str: String) {
        val start = sb.length
        sb.append(str)
        sb.setSpan(RelativeSizeSpan(16.0f / 12.0f), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun appendStringWithFont(sb: SpannableStringBuilder, str: String, family: String, tf: Typeface) {
        val start = sb.length
        sb.append(str)
        sb.setSpan(CustomTypefaceSpan(family, tf), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun appendLinkSpan(spannableString: SpannableStringBuilder, string: String, style: Int, handler: (view: View) -> Unit) {
        val start = spannableString.length
        spannableString.append(string)
        val end = spannableString.length
        val linkTextAppearanceSpan = TextAppearanceSpan(ErikuraApplication.instance.applicationContext, style)
        spannableString.setSpan(linkTextAppearanceSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(object: ClickableSpan() {
            override fun onClick(widget: View) {
                handler(widget)
            }
        }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun preEntryWorkingLimitAt(workingStartAt: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = workingStartAt
        calendar.add(Calendar.DATE, 1)
        return calendar.time
    }

    fun formatWeekDay(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return ErikuraApplication.WEEK_DAYS[calendar.get(Calendar.DAY_OF_WEEK) - 1]
    }

    fun getWorkingDay(date: Date): String {
        val workingDay = "作業日(%s)"
        val sdfDate = SimpleDateFormat("MM/dd")
        val sdfWeekDay = "(%s) "
        val sdfTime = SimpleDateFormat("HH:mm")
        val weekday= formatWeekDay(date)
        val dateFormat = sdfDate.format(date) + String.format(sdfWeekDay, weekday) + sdfTime.format(date)
        return String.format(workingDay, dateFormat)
    }

    fun getFormattedDate(date: Date): String {
        val sdfDate = SimpleDateFormat("yyyy/MM/dd")
        val sdfWeekDay = "(%s) "
        val sdfTime = SimpleDateFormat("HH:mm")
        val weekday= JobUtil.formatWeekDay(date)
        return sdfDate.format(date) + String.format(sdfWeekDay, weekday) + sdfTime.format(date)
    }

    fun getFormattedDateWithoutYear(date: Date): String {
        val sdfDate = SimpleDateFormat("MM/dd")
        val sdfWeekDay = "(%s) "
        val sdfTime = SimpleDateFormat("HH:mm")
        val weekday= JobUtil.formatWeekDay(date)
        return sdfDate.format(date) + String.format(sdfWeekDay, weekday) + sdfTime.format(date)
    }

    fun getFormattedDateJp (date: Date): String {
        val sdfDate = SimpleDateFormat("yyyy年MM月dd日")
        return sdfDate.format(date)
    }

    fun displaySuspendReportConfirmation(
        activity: Activity,
        step: ReportDraft.ReportStep,
        job: Job,
        summaryIndex: Int?
    ) {
        // ダイアログを表示し下書きを保存するか確認する
        val dialog = AlertDialog.Builder(activity)
            .setView(R.layout.dialog_suspend_report_confirmation)
            .setCancelable(false)
            .create()
        dialog.show()
        val saveButton: Button = dialog.findViewById(R.id.suspend_report_save_report_draft_button)
        saveButton.setOnClickListener(View.OnClickListener {
            //入力内容を保存してタスク詳細画面へ遷移する
            dialog.dismiss()
            // 作業報告を保存します
            JobUtils.saveReportDraft(job, step = step, summaryIndex = summaryIndex)
            val intent = Intent(activity, JobDetailsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("job", job)
            activity.startActivity(intent)
        })
        val discardButton: Button =
            dialog.findViewById(R.id.suspend_report_discard_report_draft_button)
        discardButton.setOnClickListener(View.OnClickListener {
            //下書きを破棄して、画像選択画面へ遷移する
            dialog.dismiss()
            job.report = null
            JobUtils.removeReportDraft(job)
            val intent = Intent(activity, ReportImagePickerActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("job", job)
            activity.startActivity(intent)
        })
        val closeButton: ImageButton = dialog.findViewById(R.id.close_button)
        closeButton.setOnClickListener(View.OnClickListener {
            dialog.dismiss()
        })
    }

    fun displayOldPictureWarning(context: Context, takenAt: Date?, acceptHandler: (() -> Unit)) {
        val takenAtString = takenAt?.let { getFormattedDateJp(it) }

        val dialog = AlertDialog.Builder(context)
            .setView(R.layout.dialog_notice_old_taken_picture)
            .setCancelable(false)
            .create()
        dialog.show()
        val warningCaption: TextView? = dialog.findViewById(R.id.dialog_warning_caption)
        warningCaption?.text = ErikuraApplication.instance.getString(R.string.notice_old_taken_picture_caption, takenAtString)
        val selectButton: Button = dialog.findViewById(R.id.select_button)
        selectButton.setOnClickListener(View.OnClickListener {
            acceptHandler()
            dialog.dismiss()
        })
        val cancelButton: Button = dialog.findViewById(R.id.cancel_button)
        cancelButton.setOnClickListener(View.OnClickListener {
            dialog.dismiss()
        })
    }
}

class CustomTypefaceSpan(family: String, val customTypeface: Typeface) : TypefaceSpan(family) {
    override fun updateDrawState(ds: TextPaint) {
        applyCustomTypeface(ds, customTypeface)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeface(paint, customTypeface)
    }

    private fun applyCustomTypeface(paint: Paint, tf: Typeface) {
        var oldStyle: Int = 0
        val old = paint.typeface
        if (old == null) {
            oldStyle = 0
        }
        else {
            oldStyle = old.style
        }

        val fake = oldStyle and tf.style.inv()
        if ((fake and Typeface.BOLD) != 0) {
            paint.isFakeBoldText = true
        }
        if ((fake and Typeface.ITALIC) != 0) {
            paint.textSkewX = -0.25f
        }
        paint.setTypeface(tf)
    }
}
