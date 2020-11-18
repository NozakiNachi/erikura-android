package jp.co.recruit.erikura.business.models

import android.content.Context
import jp.co.recruit.erikura.BuildConfig
import jp.co.recruit.erikura.data.network.Api
import jp.co.recruit.erikura.data.network.ErikuraConfigValue

object ErikuraConfig {
    const val REWARD_RANGE_KEY = "reward_range"
    const val WORKING_TIME_RANGE_KEY = "working_time_range"
    const val INQUIRY_URL_KEY = "inquiry_url"
    const val FAQ_URL_KEY = "faq_url"
    const val RECOMMENDED_URL_KEY = "recommended_environment_url"

    var loaded: Boolean = false
    var rewardRange: List<Int> = listOf(1,5,10,50,100,500,1000,1500,2000,2500,3000,3500,4000,4500,5000)
    var workingTimeRange: List<Int> = listOf(1,5,10,15,30,45,60,75,90,105,120,180,240,300,360,420,480)
    var inquiryURLString: String = "https://support.erikura.net/"
    var frequentlyQuestionsURLString: String = "https://faq.erikura.net/hc/ja/sections/360003690953-FAQ"
    var recommendedEnvironmentURLString: String =
        "https://faq.erikura.net/hc/ja/articles/360020286793-%E3%82%B5%E3%82%A4%E3%83%88%E3%81%AE%E6%8E%A8%E5%A5%A8%E7%92%B0%E5%A2%83%E3%82%92%E6%95%99%E3%81%88%E3%81%A6%E3%81%8F%E3%81%A0%E3%81%95%E3%81%84"

    fun jobReportURLString(job_id: Int?, token: String?): String {
        return BuildConfig.SERVER_BASE_URL + "login_with_token?token="+ token + "&job_id=" + job_id
    }

    fun load(context: Context, onError: ((messages: List<String>?) -> Unit)? = null) {
        if (!loaded) {
            Api(context).erikuraConfig(onError = onError) { result ->
                result[REWARD_RANGE_KEY]?.let { v ->
                    rewardRange = (v as ErikuraConfigValue.DoubleList).values.map { it.toInt() }
                }
                result[WORKING_TIME_RANGE_KEY]?.let { v ->
                    workingTimeRange =
                        (v as ErikuraConfigValue.DoubleList).values.map { it.toInt() }
                }
                result[FAQ_URL_KEY]?.let { v ->
                    (v as? ErikuraConfigValue.StringValue)?.let {
                        frequentlyQuestionsURLString = it.value ?: frequentlyQuestionsURLString
                    }
                }
                result[INQUIRY_URL_KEY]?.let { v ->
                    (v as? ErikuraConfigValue.StringValue)?.let {
                        inquiryURLString = it.value ?: inquiryURLString
                    }
                }
                result[RECOMMENDED_URL_KEY]?.let { v ->
                    (v as? ErikuraConfigValue.StringValue)?.let {
                        recommendedEnvironmentURLString =
                            it.value ?: recommendedEnvironmentURLString
                    }
                }
                loaded = true
            }
        }
    }
}