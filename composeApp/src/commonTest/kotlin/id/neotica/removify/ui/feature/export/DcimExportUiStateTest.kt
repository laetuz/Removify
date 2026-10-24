package id.neotica.removify.ui.feature.export

import id.neotica.removify.domain.model.DcimItem
import id.neotica.removify.domain.model.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals

class DcimExportUiStateTest {

    private fun item(id: Long, type: MediaType, size: Long) = DcimItem(
        id = id,
        contentUri = "content://media/$id",
        name = "file-$id",
        relativePath = "DCIM/Camera",
        mimeType = if (type == MediaType.IMAGE) "image/jpeg" else "video/mp4",
        sizeBytes = size,
        dateTakenEpochMs = 0,
        type = type,
    )

    private val items = listOf(
        item(1, MediaType.IMAGE, 100),
        item(2, MediaType.IMAGE, 200),
        item(3, MediaType.VIDEO, 1_000),
    )

    @Test
    fun selectsEverythingByDefault() {
        val state = DcimExportUiState(items = items)
        assertEquals(3, state.selectedCount)
        assertEquals(1_300L, state.selectedBytes)
    }

    @Test
    fun canSelectImagesOnly() {
        val state = DcimExportUiState(items = items, includeVideos = false)
        assertEquals(listOf(1L, 2L), state.selectedItems.map { it.id })
        assertEquals(300L, state.selectedBytes)
    }

    @Test
    fun canSelectVideosOnly() {
        val state = DcimExportUiState(items = items, includeImages = false)
        assertEquals(listOf(3L), state.selectedItems.map { it.id })
        assertEquals(1_000L, state.selectedBytes)
    }

    @Test
    fun noSelectionWhenBothDisabled() {
        val state = DcimExportUiState(items = items, includeImages = false, includeVideos = false)
        assertEquals(0, state.selectedCount)
        assertEquals(0L, state.selectedBytes)
    }
}
