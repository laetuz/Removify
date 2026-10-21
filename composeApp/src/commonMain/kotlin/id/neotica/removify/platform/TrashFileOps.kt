package id.neotica.removify.platform

import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.TrashEntry
import id.neotica.removify.domain.model.TrashMoveResult

expect suspend fun moveToTrash(items: List<MediaItem>, sessionId: String): Result<TrashMoveResult>
expect suspend fun listTrashEntries(): Result<List<TrashEntry>>
expect suspend fun restoreTrashEntries(entries: List<TrashEntry>): Result<Int>
expect suspend fun emptyTrashFiles(): Result<Long>
