package jp.co.recruit.erikura.business.util

import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.business.models.Job

/**
 * 案件の操作を行うためのユーティリティ
 */
object JobUtils {
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
}
