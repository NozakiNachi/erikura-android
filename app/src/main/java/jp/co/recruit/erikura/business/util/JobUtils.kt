package jp.co.recruit.erikura.business.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.core.content.ContextCompat.startActivity
import com.google.android.gms.maps.model.LatLng
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.realm.Sort
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.R
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.MediaItem
import jp.co.recruit.erikura.business.models.Report
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.network.MediaItemSerializer
import jp.co.recruit.erikura.data.storage.Asset
import jp.co.recruit.erikura.data.storage.AssetsManager
import jp.co.recruit.erikura.data.storage.PhotoTokenManager
import jp.co.recruit.erikura.data.storage.ReportDraft
import jp.co.recruit.erikura.presenters.activities.job.JobDetailsActivity
import jp.co.recruit.erikura.presenters.activities.report.ReportConfirmActivity
import jp.co.recruit.erikura.presenters.activities.report.ReportImagePickerActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Comparator
import kotlin.collections.HashMap

/**
 * 案件の操作を行うためのユーティリティ
 */
object JobUtils {
    object DateFormats {
        val simple: SimpleDateFormat = SimpleDateFormat("yyyy/MM/dd H:mm")
        val simple_MMddHmm: SimpleDateFormat = SimpleDateFormat("MM/dd H:mm")
    }

    /**
     * 緯度経度が同一の案件をまとめたマップを構築します
     */
    fun summarizeJobsByLocation(jobs: List<Job>): Map<LatLng, List<Job>> {
        val jobsByLocation = HashMap<LatLng, MutableList<Job>>()
        jobs.forEach { job ->
            val latLng = job.latLng
            if (!jobsByLocation.containsKey(latLng)) {
                jobsByLocation.put(latLng, mutableListOf(job))
            }
            else {
                val list = jobsByLocation[latLng] ?: mutableListOf()
                list.add(job)
            }
        }
        // ソートは同一案件の選択モーダル表示時に行います
        return jobsByLocation
    }

    fun sortJobs(jobs: List<Job>): List<Job> {
        val comparators = arrayOf(
            compareJobState,
            compareWanted, compareBoosted,
            compareWorkingFinishAtForActive, compareWorkingStartAtForFuture, compareWorkingFinishAtForPast,
            compareFee, compareCreatedAt, compareId
//            compareWorkingFinishAt, compareWorkingStartAt, compareId
        )
        return jobs.sortedWith(comparators.reduce { c1, c2 -> c1.then(c2) })
    }

    private fun jobStatusPriority(job: Job): Int {
        return when {
            job.isEntried && job.isOwner && !job.isReported -> 0
            job.isActive -> 1
            job.isPreEntry -> 2
            job.isFuture -> 3
            job.isEntried && job.isOwner -> 4
            else -> 5
        }
    }

    private val compareJobState: Comparator<Job> = Comparator { j1, j2 ->
        jobStatusPriority(j1) - jobStatusPriority(j2)
    }

    private val compareWorkingFinishAtForActive: Comparator<Job> = Comparator { j1, j2 ->
        if (!j1.isActive || !j2.isActive) { 0 }
        else {
            val aFinishedAt = j1.workingFinishAt ?: Date()
            val bFinishedAt = j2.workingFinishAt ?: Date()
            when {
                aFinishedAt < bFinishedAt -> -1
                aFinishedAt > bFinishedAt -> 1
                else -> 0
            }
        }
    }

    private val compareWorkingStartAtForFuture: Comparator<Job> = Comparator { j1, j2 ->
        if (!j1.isFuture || !j2.isFuture) { 0 }
        else {
            val aStartAt = j1.workingStartAt ?: Date()
            val bStartAt = j2.workingStartAt ?: Date()
            when {
                aStartAt < bStartAt -> -1
                aStartAt > bStartAt -> 1
                else -> 0
            }
        }
    }

    private val compareWorkingFinishAtForPast: Comparator<Job> = Comparator { j1, j2 ->
        if (!j1.isPastOrInactive || !j2.isPastOrInactive) { 0 }
        else {
            val aFinishedAt = j1.workingFinishAt ?: Date()
            val bFinishedAt = j2.workingFinishAt ?: Date()
            when {
                aFinishedAt < bFinishedAt -> -1
                aFinishedAt > bFinishedAt -> 1
                else -> 0
            }
        }
    }

    private val compareWanted: Comparator<Job> = Comparator { j1, j2 ->
        val aWanted = j1.wanted ?: false
        val bWanted = j2.wanted ?: false
        when {
            aWanted && !bWanted -> -1
            !aWanted && bWanted -> 1
            else -> 0
        }
    }

    private val compareBoosted: Comparator<Job> = Comparator { j1, j2 ->
        val aBoosted = j1.boost ?: false
        val bBoosted = j2.boost ?: false
        when {
            aBoosted && !bBoosted -> -1
            !aBoosted && bBoosted -> 1
            else -> 0
        }
    }

    private val compareWorkingFinishAt: Comparator<Job> = compareBy(Job::workingFinishAt)
    private val compareWorkingStartAt: Comparator<Job> = compareBy(Job::workingStartAt)
    private val compareFee: Comparator<Job> = compareBy(Job::fee).reversed()
    private val compareCreatedAt: Comparator<Job> = compareBy(Job::createdAt)
    private val compareId: Comparator<Job> = compareBy(Job::id)

    fun loadReportDraft(job: Job): ReportDraft? {
        return ErikuraApplication.realm.let { realm ->
            realm.where(ReportDraft::class.java).equalTo("jobId", job.id).findFirst()
        }
    }

    fun saveReportDraft(job: Job, step: ReportDraft.ReportStep, summaryIndex: Int? = null) {
        job.report?.let {
            val realm = ErikuraApplication.realm
            realm.executeTransaction {
                val draft: ReportDraft =
                    realm.where(ReportDraft::class.java).equalTo("jobId", job.id).findFirst()
                        ?: realm.createObject(ReportDraft::class.java, job.id)
                draft.lastModifiedAt = Date()
                draft.step = step
                summaryIndex?.let { draft.lastSummaryIndex = it }
                job.report?.let { report ->
                    draft.dumpReport(report)
                }
            }
        }
    }

    /** 下書きを削除します */
    fun removeReportDraft(job: Job) {
        val realm = ErikuraApplication.realm
        realm.executeTransaction {
            val draft: ReportDraft? = realm.where(ReportDraft::class.java).equalTo("jobId", job.id).findFirst()
            draft?.deleteFromRealm()
        }
    }

    fun reportActivityRelatedOnClickBack(activity: Activity, step: ReportDraft.ReportStep, job: Job, summaryIndex: Int?) {
            // ダイアログを表示し下書きを保存するか確認する
            val dialog = AlertDialog.Builder(activity)
                .setView(R.layout.dialog_save_report_draft_question)
                .setCancelable(false)
                .create()
            dialog.show()
            val button: Button = dialog.findViewById(R.id.save_report_draft_button)
            button.setOnClickListener(View.OnClickListener{
                //入力内容を保存してタスク詳細画面へ遷移する
                dialog.dismiss()
                // 作業報告を保存します
                JobUtils.saveReportDraft(job, step = step, summaryIndex = summaryIndex)
                val intent= Intent(activity, JobDetailsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("job", job)
                activity.startActivity(intent)
            })
            val cancelButton: Button = dialog.findViewById(R.id.not_save_report_draft_button)
            cancelButton.setOnClickListener(View.OnClickListener {
                //下書きを破棄して、画像選択画面へ遷移する
                dialog.dismiss()
                job?.report = null
                removeReportDraft(job)
                val intent = Intent(activity, ReportImagePickerActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("job", job)
                activity.startActivity(intent)
            })
            val closeButton: ImageButton = dialog.findViewById(R.id.close_button)
            closeButton.setOnClickListener(View.OnClickListener{
                dialog.dismiss()
            })
    }
}
