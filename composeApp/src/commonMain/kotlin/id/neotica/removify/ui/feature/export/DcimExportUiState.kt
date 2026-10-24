package id.neotica.removify.ui.feature.export

import id.neotica.removify.domain.model.DcimExportProgress
import id.neotica.removify.domain.model.DcimExportReport
import id.neotica.removify.domain.model.DcimItem
import id.neotica.removify.domain.model.MediaType

data class DcimExportUiState(
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
    val items: List<DcimItem> = emptyList(),
    val totalBytes: Long = 0,
    val destinationUri: String? = null,
    val destinationName: String? = null,
    val isExporting: Boolean = false,
    val progress: DcimExportProgress? = null,
    val showConfirmDialog: Boolean = false,
    val awaitingDeleteApproval: Boolean = false,
    val pendingDeleteUris: List<String> = emptyList(),
    val report: DcimExportReport? = null,
    val errorMessage: String? = null,
) {
    val photoCount: Int get() = items.count { it.type == MediaType.IMAGE }
    val videoCount: Int get() = items.count { it.type == MediaType.VIDEO }
}
