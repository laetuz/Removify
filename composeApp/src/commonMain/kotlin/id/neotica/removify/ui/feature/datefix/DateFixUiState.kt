package id.neotica.removify.ui.feature.datefix

import id.neotica.removify.domain.model.DateFixPreview
import id.neotica.removify.domain.model.DateFixProgress
import id.neotica.removify.domain.model.DateFixReport

data class DateFixUiState(
    val isGalleryTarget: Boolean = false,
    val hasPermission: Boolean = false,
    val folderPath: String? = null,
    val folderName: String? = null,
    val isLoading: Boolean = false,
    val preview: DateFixPreview? = null,
    val skipMatching: Boolean = true,
    val isApplying: Boolean = false,
    val progress: DateFixProgress? = null,
    val showConfirmDialog: Boolean = false,
    val awaitingWriteApproval: Boolean = false,
    val pendingWriteIds: List<String> = emptyList(),
    val report: DateFixReport? = null,
    val errorMessage: String? = null,
) {
    val alreadyCount: Int get() = preview?.alreadyCount ?: 0
    val skippedCount: Int get() = preview?.skippedCount ?: 0
    val updateCount: Int
        get() = when {
            preview == null -> 0
            skipMatching -> preview.toUpdateCount
            else -> preview.matchedCount
        }
}
