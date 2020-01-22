package jp.co.recruit.erikura.presenters.view_models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.business.models.JobKind
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.business.models.PeriodType
import jp.co.recruit.erikura.business.models.SortType

open class BaseJobQueryViewModel: ViewModel() {
    val keyword: MutableLiveData<String> =
        MutableLiveData()
    val minimumReward: MutableLiveData<Int> =
        MutableLiveData()
    val maximumReward: MutableLiveData<Int> =
        MutableLiveData()
    val minimumWorkingTime: MutableLiveData<Int> =
        MutableLiveData()
    val maximumWorkingTime: MutableLiveData<Int> =
        MutableLiveData()
    val jobKind: MutableLiveData<JobKind> =
        MutableLiveData()
    val sortType: MutableLiveData<SortType> =
        MutableLiveData()
    val periodType: MutableLiveData<PeriodType> =
        MutableLiveData()

    open fun query(latLng: LatLng): JobQuery {
        return JobQuery(
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            keyword = normalizedKeyword,
            minimumWorkingTime = minimumWorkingTime.value,
            maximumWorkingTime = maximumWorkingTime.value,
            minimumReward = minimumReward.value,
            maximumReward = maximumReward.value,
            jobKind = jobKind.value,
            sortBy = sortType.value
                ?: SortType.DISTANCE_ASC,
            period = periodType.value
                ?: PeriodType.ALL
        )
    }

    val normalizedKeyword: String? get() {
        return if (keyword.value == null || keyword.value == JobQuery.CURRENT_LOCATION) {
            null
        }
        else {
            keyword.value
        }
    }
}