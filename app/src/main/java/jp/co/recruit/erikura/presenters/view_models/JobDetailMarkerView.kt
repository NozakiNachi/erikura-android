package jp.co.recruit.erikura.presenters.view_models

import android.view.View
import jp.co.recruit.erikura.business.models.Job

class JobDetailMarkerView(job: Job): MarkerViewModel(job) {
    override val isOwnerMarker: Boolean
        get() = false
    override val isDisabled: Boolean
        get() {return false}
    override val boostVisibility: Int
        get() {return View.GONE}
    override val wantedVisibility: Int
        get() {return View.GONE}
    override val soonVisibility: Int
        get() {return View.GONE}
    override val preEntryVisibility: Int
        get() {return View.GONE}
    override val markerUrl: String
        get() {
            val iconPath = job.jobKind?.activeIconUrl?.path ?: "EMPTY_PATH"
            return "eriukra-marker://v2/jobDetail/${job.fee}/false/false/false/${active.value}/false/current/${iconPath}/"
        }
}