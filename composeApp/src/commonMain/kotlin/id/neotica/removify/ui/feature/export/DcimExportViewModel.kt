package id.neotica.removify.ui.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.neotica.removify.domain.model.DcimCopyResult
import id.neotica.removify.domain.model.DcimExportReport
import id.neotica.removify.domain.repository.DcimExportRepository
import id.neotica.removify.platform.clearDcimDestination
import id.neotica.removify.platform.dcimDestinationDisplayName
import id.neotica.removify.platform.dcimDestinationError
import id.neotica.removify.platform.hasMediaPermission
import id.neotica.removify.platform.saveDcimDestination
import id.neotica.removify.platform.savedDcimDestinationName
import id.neotica.removify.platform.savedDcimDestinationUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DcimExportViewModel(
    private val repository: DcimExportRepository,
) : ViewModel() {

    private val savedDestinationUri = savedDcimDestinationUri()
    private val savedDestinationError = savedDestinationUri?.let { dcimDestinationError(it) }

    private val _state = MutableStateFlow(
        DcimExportUiState(
            hasPermission = hasMediaPermission(),
            destinationUri = savedDestinationUri.takeIf { savedDestinationError == null },
            destinationName = savedDcimDestinationName().takeIf { savedDestinationError == null },
        ),
    )
    val state: StateFlow<DcimExportUiState> = _state.asStateFlow()

    private val cancelRequested = MutableStateFlow(false)
    private var lastCopyResult: DcimCopyResult? = null

    init {
        if (savedDestinationError != null) clearDcimDestination()
        if (_state.value.hasPermission) refresh()
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(hasPermission = granted) }
        if (granted) refresh()
    }

    fun refresh() {
        if (!hasMediaPermission()) {
            _state.update { it.copy(hasPermission = false, items = emptyList(), totalBytes = 0) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            repository.listDcim()
                .onSuccess { items ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            hasPermission = true,
                            items = items,
                            totalBytes = items.sumOf { item -> item.sizeBytes },
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to read DCIM",
                        )
                    }
                }
        }
    }

    fun onDestinationPicked(treeUri: String) {
        val error = dcimDestinationError(treeUri)
        if (error != null) {
            _state.update { it.copy(errorMessage = error) }
            return
        }

        val name = dcimDestinationDisplayName(treeUri) ?: treeUri
        saveDcimDestination(treeUri, name)
        _state.update {
            it.copy(
                destinationUri = treeUri,
                destinationName = name,
                errorMessage = null,
            )
        }
    }

    fun toggleImages() {
        _state.update {
            it.copy(
                includeImages = !it.includeImages,
                report = null,
                errorMessage = null,
            )
        }
    }

    fun toggleVideos() {
        _state.update {
            it.copy(
                includeVideos = !it.includeVideos,
                report = null,
                errorMessage = null,
            )
        }
    }

    fun requestMove() {
        val current = _state.value
        if (current.isExporting) return

        if (current.selectedItems.isEmpty()) {
            _state.update { it.copy(errorMessage = "Select photos or videos to move") }
            return
        }
        if (current.destinationUri == null) {
            _state.update { it.copy(errorMessage = "Choose a destination folder first") }
            return
        }

        _state.update {
            it.copy(showConfirmDialog = true, errorMessage = null, report = null)
        }
    }

    fun dismissConfirm() {
        _state.update { it.copy(showConfirmDialog = false) }
    }

    fun confirmMove() {
        val current = _state.value
        val destination = current.destinationUri ?: return
        val selected = current.selectedItems
        if (current.isExporting || selected.isEmpty()) return

        cancelRequested.value = false
        _state.update {
            it.copy(
                showConfirmDialog = false,
                isExporting = true,
                progress = null,
                report = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            repository.copyToTree(
                items = selected,
                treeUri = destination,
                shouldCancel = { cancelRequested.value },
                onProgress = { progress ->
                    _state.update { it.copy(progress = progress) }
                },
            ).onSuccess { result ->
                lastCopyResult = result
                if (result.copied.isEmpty() || result.cancelled) {
                    publishReport(result, deleteApproved = false)
                } else {
                    _state.update {
                        it.copy(
                            isExporting = false,
                            awaitingDeleteApproval = true,
                            pendingDeleteUris = result.copied.map { item -> item.contentUri },
                        )
                    }
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = error.message ?: "Export failed",
                    )
                }
            }
        }
    }

    fun cancelExport() {
        cancelRequested.value = true
    }

    fun onDeleteRequestResult(approved: Boolean) {
        val result = lastCopyResult ?: return
        if (approved) {
            publishReport(result, deleteApproved = true)
            refresh()
        } else {
            publishReport(result, deleteApproved = false)
        }
    }

    fun dismissReport() {
        _state.update { it.copy(report = null) }
    }

    private fun publishReport(result: DcimCopyResult, deleteApproved: Boolean) {
        val report = DcimExportReport(
            totalCount = result.copied.size + result.failures.size,
            copiedCount = result.copied.size,
            copiedBytes = result.copiedBytes,
            failedCount = result.failures.size,
            deletedCount = if (deleteApproved) result.copied.size else 0,
            cancelled = result.cancelled,
            deleteApproved = deleteApproved,
            failures = result.failures,
        )

        _state.update {
            it.copy(
                isExporting = false,
                awaitingDeleteApproval = false,
                pendingDeleteUris = emptyList(),
                progress = null,
                report = report,
            )
        }
        lastCopyResult = null
    }
}
