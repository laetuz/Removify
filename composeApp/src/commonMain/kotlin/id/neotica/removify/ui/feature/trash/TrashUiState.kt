package id.neotica.removify.ui.feature.trash

import id.neotica.removify.domain.model.TrashEntry

data class TrashUiState(
    val entries: List<TrashEntry> = emptyList(),
    val isLoading: Boolean = false,
    val showEmptyDialog: Boolean = false,
    val errorMessage: String? = null,
    val lastFreedBytes: Long? = null,
) {
    val totalBytes: Long get() = entries.sumOf { it.sizeBytes }
}
