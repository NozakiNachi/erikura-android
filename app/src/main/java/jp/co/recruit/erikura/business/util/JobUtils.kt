package jp.co.recruit.erikura.business.util

import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.data.storage.ReportDraft
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
}
