package id.neotica.removify.data

import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.TrashEntry
import id.neotica.removify.domain.model.TrashMoveResult
import id.neotica.removify.domain.repository.TrashRepository
import id.neotica.removify.platform.emptyTrashFiles as emptyTrashFilesPlatform
import id.neotica.removify.platform.listTrashEntries as listTrashEntriesPlatform
import id.neotica.removify.platform.moveToTrash as moveToTrashFiles
import id.neotica.removify.platform.restoreTrashEntries as restoreTrashEntriesPlatform

class TrashRepositoryImpl : TrashRepository {

    override suspend fun moveToTrash(
        items: List<MediaItem>,
        sessionId: String,
    ): Result<TrashMoveResult> = moveToTrashFiles(items, sessionId)

    override suspend fun listTrash(): Result<List<TrashEntry>> = listTrashEntriesPlatform()

    override suspend fun restore(entries: List<TrashEntry>): Result<Int> =
        restoreTrashEntriesPlatform(entries)

    override suspend fun emptyTrash(): Result<Long> = emptyTrashFilesPlatform()
}
