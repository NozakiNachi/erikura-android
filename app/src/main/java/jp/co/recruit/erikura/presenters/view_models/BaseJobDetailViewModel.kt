package jp.co.recruit.erikura.presenters.view_models

import android.view.View
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import jp.co.recruit.erikura.business.models.Job
import jp.co.recruit.erikura.business.models.User

open class BaseJobDetailViewModel : ViewModel() {
    val job = MutableLiveData<Job>()
    val user = MutableLiveData<User>()

    val boostVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(job) { job ->
            result.value = if (job.boost) { View.VISIBLE } else { View.GONE }
        }
    }
    val wantedVisibility = MediatorLiveData<Int>().also { result ->
        result.addSource(job) { job ->
            result.value = if (job.wanted) { View.VISIBLE } else { View.GONE }
        }
    }
}