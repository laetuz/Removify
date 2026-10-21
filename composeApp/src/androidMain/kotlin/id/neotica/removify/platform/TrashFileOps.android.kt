package id.neotica.removify.platform

import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.TrashEntry
import id.neotica.removify.domain.model.TrashMoveResult

actual suspend fun moveToTrash(items: List<MediaItem>, sessionId: String): Result<TrashMoveResult> =
    Result.success(TrashMoveResult(movedCount = 0, failedCount = 0, bytes = 0))

actual suspend fun listTrashEntries(): Result<List<TrashEntry>> = Result.success(emptyList())

actual suspend fun restoreTrashEntries(entries: List<TrashEntry>): Result<Int> = Result.success(0)

actual suspend fun emptyTrashFiles(): Result<Long> = Result.success(0L)
