package jp.co.recruit.erikura.data.storage

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import jp.co.recruit.erikura.business.models.MediaItem
import jp.co.recruit.erikura.business.models.Report
import jp.co.recruit.erikura.data.network.MediaItemSerializer
import java.util.*

/**
 * 作業報告下書き
 */
open class ReportDraft : RealmObject() {
    enum class ReportStep {
        PictureSelectForm,
        SummaryForm,
        WorkingTimeForm,
        OtherForm,
        EvaluationForm,
        Confirm,
    }

    @PrimaryKey
    open var jobId: Int = 0
    open lateinit var jsonString: String
    open lateinit var lastModifiedAt: Date
    open var lastSummaryIndex = 0
    open lateinit var stepValue: String

    var step: ReportStep
        get() = ReportStep.values().first { it.name == stepValue }
        set(value) {
            stepValue = value.name
        }

    fun restoreReport(): Report? {
        val gson = reportGson()
        return gson.fromJson(jsonString, Report::class.java)
    }

    fun dumpReport(report: Report) {
        val gson = reportGson()
        jsonString = gson.toJson(report)
    }

    private fun reportGson(): Gson {
        return GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(MediaItem::class.java, MediaItemSerializer())
            .serializeNulls()
            .create()
    }
}