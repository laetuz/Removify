package id.neotica.removify.domain.model

data class DcimExportProgress(
    val processedCount: Int,
    val totalCount: Int,
    val copiedBytes: Long,
    val totalBytes: Long,
    val currentName: String?,
)
