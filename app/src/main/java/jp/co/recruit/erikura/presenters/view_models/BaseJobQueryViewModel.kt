package jp.co.recruit.erikura.presenters.view_models

import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import jp.co.recruit.erikura.ErikuraApplication
import jp.co.recruit.erikura.business.models.JobKind
import jp.co.recruit.erikura.business.models.JobQuery
import jp.co.recruit.erikura.business.models.PeriodType
import jp.co.recruit.erikura.business.models.SortType

open class BaseJobQueryViewModel: ViewModel() {
    val keyword: MutableLiveData<String> = MutableLiveData()
    val minimumReward: MutableLiveData<Int> = MutableLiveData()
    val maximumReward: MutableLiveData<Int> = MutableLiveData()
    val minimumWorkingTime: MutableLiveData<Int> = MutableLiveData()
    val maximumWorkingTime: MutableLiveData<Int> = MutableLiveData()
    val jobKind: MutableLiveData<JobKind> = MutableLiveData()
    val sortType: MutableLiveData<SortType> = MutableLiveData()
    val periodType: MutableLiveData<PeriodType> = MutableLiveData()
    val latLng: MutableLiveData<LatLng> = MutableLiveData()

    val conditions = MediatorLiveData<List<String>>().also { result ->
        result.addSource(keyword)               { result.value = generateConditions() }
        result.addSource(minimumWorkingTime)    { result.value = generateConditions() }
        result.addSource(maximumWorkingTime)    { result.value = generateConditions() }
        result.addSource(minimumReward)         { result.value = generateConditions() }
        result.addSource(maximumReward)         { result.value = generateConditions() }
        result.addSource(jobKind)               { result.value = generateConditions() }
        result.addSource(sortType)              { result.value = generateConditions() }
        result.addSource(periodType)            { result.value = generateConditions() }
    }

    open fun query(latLng: LatLng): JobQuery {
        this.latLng.value = latLng
        Log.v(ErikuraApplication.LOG_TAG, "QUERY: latLng=${this.latLng.value}")

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

    open fun clearWithoutPeriod() {
        keyword.value = null
        latLng.value = null
        minimumWorkingTime.value = null
        maximumWorkingTime.value = null
        minimumReward.value = null
        maximumReward.value = null
        jobKind.value = null
        sortType.value = null
    }

    open fun apply(query: JobQuery) {
        keyword.value = query.keyword
        latLng.value = query.latLng
        minimumWorkingTime.value = query.minimumWorkingTime
        maximumWorkingTime.value = query.maximumWorkingTime
        minimumReward.value = query.minimumReward
        maximumReward.value = query.maximumReward
        jobKind.value = query.jobKind
        sortType.value = query.sortBy
        periodType.value = query.period
    }

    val normalizedKeyword: String? get() {
        return if (keyword.value == null || keyword.value == JobQuery.CURRENT_LOCATION) {
            null
        }
        else {
            keyword.value
        }
    }

    private fun generateConditions() : List<String> {
        val conditions = ArrayList<String>()

        // ??????
        conditions.add(keyword.value ?: "???????????????")
        // ??????
        if (minimumReward.value != null || maximumReward.value != null) {
            val minReward = minimumReward.value?.let { if (it != JobQuery.MIN_REWARD) { String.format("%,d???", it) } else { "????????????" } } ?: ""
            val maxReward = maximumReward.value?.let { if (it != JobQuery.MAX_REWARD) { String.format("%,d???", it) } else { "????????????" } } ?: ""
            conditions.add("${minReward} ??? ${maxReward}")
        }
        // ????????????
        if (minimumWorkingTime.value != null || maximumWorkingTime.value != null) {
            val minWorkTime = minimumWorkingTime.value?.let { if (it != JobQuery.MIN_WORKING_TIME) { String.format("%,d???", it) } else { "????????????" } } ?: ""
            val maxWorkTime = maximumWorkingTime.value?.let { if (it != JobQuery.MAX_WORKING_TIME) { String.format("%,d???", it) } else { "????????????" } } ?: ""
            conditions.add("${minWorkTime} ??? ${maxWorkTime}")
        }
        // ??????
        jobKind.value?.also {
            conditions.add(it.name?: "")
        }
        return conditions
    }}