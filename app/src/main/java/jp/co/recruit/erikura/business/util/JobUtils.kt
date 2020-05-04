package jp.co.recruit.erikura.business.util

import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.business.models.Job
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
        // FIXME: まとめた後の案件のソートについて検討すること
        return jobsByLocation
    }

    fun sortJobs(jobs: List<Job>): List<Job> {
        val comparators = arrayOf(
            compareJobState,
            compareWorkingFinishAtForActive, compareWorkingStartAtForFuture, compareWorkingFinishAtForPast,
            compareWorkingFinishAt, compareWorkingStartAt, compareId
        )
        return jobs.sortedWith(comparators.reduce { c1, c2 -> c1.then(c2) })
    }

    private fun jobStatusPriority(job: Job): Int {
        return when {
            job.isFuture -> 1
            job.isPastOrInactive -> 2
            else -> 0
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
                aFinishedAt > bFinishedAt -> -1
                aFinishedAt < bFinishedAt -> 1
                else -> 0
            }
        }
    }

    private val compareWorkingFinishAt: Comparator<Job> = compareBy(Job::workingFinishAt)
    private val compareWorkingStartAt: Comparator<Job> = compareBy(Job::workingStartAt)
    private val compareId: Comparator<Job> = compareBy(Job::id)
}
