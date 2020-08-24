package jp.co.recruit.erikura.business.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class ReportExample(
    var id: Int,
    var created_at: Date,
    @SerializedName("output_summary_examples_attributes")
    var output_summary_examples_attributes: List<OutputSummaryExample> = listOf()
) : Parcelable {}