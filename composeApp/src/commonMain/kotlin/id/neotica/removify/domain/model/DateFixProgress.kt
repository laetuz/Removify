package id.neotica.removify.domain.model

data class DateFixProgress(
    val processed: Int,
    val total: Int,
    val updated: Int,
    val failed: Int,
    val currentName: String?,
)

data class DateFixFailure(
    val name: String,
    val reason: String,
)

data class DateFixReport(
    val updatedCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
    val cancelled: Boolean,
    val failures: List<DateFixFailure>,
)
