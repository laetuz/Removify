package id.neotica.removify.domain.model

data class TrashMoveResult(
    val movedCount: Int,
    val failedCount: Int,
    val bytes: Long,
)
