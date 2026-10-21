package id.neotica.removify.domain.model

data class SessionSummary(
    val keptCount: Int,
    val trashCount: Int,
    val skippedCount: Int,
    val trashBytes: Long,
)
