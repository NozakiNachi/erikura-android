package jp.co.recruit.erikura.business.models

import java.util.*

data class ReportExample(
    var id: Int,
    var created_at: Date,
    var output_summary_examples_attributes: List<OutputSummaryExample>
)