package jp.co.recruit.erikura.business.models

import android.content.Context
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.network.ErikuraConfigValue

object ErikuraConfig {
    const val REWARD_RANGE_KEY = "reward_range"
    const val WORKING_TIME_RANGE_KEY = "working_time_range"

    var rewardRange: List<Int> = listOf(1,5,10,50,100,500,1000,1500,2000,2500,3000,3500,4000,4500,5000)
    var workingTimeRange: List<Int> = listOf(1,5,10,15,30,45,60,75,90,105,120,180,240,300,360,420,480)

    fun load(context: Context) {
        Api(context).erikuraConfig { result ->
            result[REWARD_RANGE_KEY]?.let { v ->
                rewardRange = (v as ErikuraConfigValue.DoubleList).values.map{ it.toInt() }
            }
            result[WORKING_TIME_RANGE_KEY]?.let { v ->
                workingTimeRange = (v as ErikuraConfigValue.DoubleList).values.map{ it.toInt() }
            }
        }
    }
}