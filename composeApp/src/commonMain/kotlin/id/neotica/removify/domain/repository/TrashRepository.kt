package id.neotica.removify.domain.repository

import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.TrashEntry
import id.neotica.removify.domain.model.TrashMoveResult

interface TrashRepository {
    suspend fun moveToTrash(items: List<MediaItem>, sessionId: String): Result<TrashMoveResult>
    suspend fun listTrash(): Result<List<TrashEntry>>
    suspend fun restore(entries: List<TrashEntry>): Result<Int>
    suspend fun emptyTrash(): Result<Long>
}
