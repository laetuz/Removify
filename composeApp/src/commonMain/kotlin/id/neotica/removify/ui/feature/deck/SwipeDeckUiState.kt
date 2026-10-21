package id.neotica.removify.ui.feature.deck

import id.neotica.removify.domain.model.MediaItem

data class SwipeDeckUiState(
    val currentItem: MediaItem? = null,
    val nextItems: List<MediaItem> = emptyList(),
    val totalCount: Int = 0,
    val decidedCount: Int = 0,
    val keptCount: Int = 0,
    val trashedCount: Int = 0,
    val skippedCount: Int = 0,
    val trashBytes: Long = 0,
    val isDeckFinished: Boolean = false,
    val isCompleted: Boolean = false,
    val showFinishDialog: Boolean = false,
    val isCommitting: Boolean = false,
    val errorMessage: String? = null,
)
