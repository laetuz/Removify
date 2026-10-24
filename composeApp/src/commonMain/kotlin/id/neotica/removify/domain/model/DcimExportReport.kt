package id.neotica.removify.domain.model

data class DcimExportFailure(
    val name: String,
    val reason: String,
)

data class DcimCopyResult(
    val copied: List<DcimItem>,
    val copiedBytes: Long,
    val failures: List<DcimExportFailure>,
    val cancelled: Boolean,
)

data class DcimExportReport(
    val totalCount: Int,
    val copiedCount: Int,
    val copiedBytes: Long,
    val failedCount: Int,
    val deletedCount: Int,
    val cancelled: Boolean,
    val deleteApproved: Boolean,
    val failures: List<DcimExportFailure>,
)
