package id.neotica.removify.platform

import id.neotica.removify.data.local.DesktopTrashStore
import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.TrashEntry
import id.neotica.removify.domain.model.TrashMoveResult
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val store by lazy { DesktopTrashStore(File(appDataDir())) }

actual suspend fun moveToTrash(
    items: List<MediaItem>,
    sessionId: String,
): Result<TrashMoveResult> = withContext(Dispatchers.IO) {
    store.moveToTrash(items, sessionId)
}

actual suspend fun listTrashEntries(): Result<List<TrashEntry>> = withContext(Dispatchers.IO) {
    store.listEntries()
}

actual suspend fun restoreTrashEntries(entries: List<TrashEntry>): Result<Int> =
    withContext(Dispatchers.IO) {
        store.restore(entries)
    }

actual suspend fun emptyTrashFiles(): Result<Long> = withContext(Dispatchers.IO) {
    store.empty()
}
