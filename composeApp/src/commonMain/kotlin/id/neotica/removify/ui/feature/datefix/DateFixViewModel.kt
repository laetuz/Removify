package id.neotica.removify.ui.feature.datefix

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.neotica.removify.domain.model.DateFixCandidate
import id.neotica.removify.domain.model.MediaSource
import id.neotica.removify.domain.repository.MediaDateFixRepository
import id.neotica.removify.platform.displayNameForPickedFolder
import id.neotica.removify.platform.hasMediaPermission
import id.neotica.removify.platform.isDateFixWriteApprovalRequired
import id.neotica.removify.platform.isGalleryDateFixTarget
import id.neotica.removify.platform.savedSourceFolderName
import id.neotica.removify.platform.savedSourceFolderUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DateFixViewModel(
    private val repository: MediaDateFixRepository,
) : ViewModel() {

    private val savedFolderPath = if (isGalleryDateFixTarget) null else savedSourceFolderUri()

    private val _state = MutableStateFlow(
        DateFixUiState(
            isGalleryTarget = isGalleryDateFixTarget,
            hasPermission = if (isGalleryDateFixTarget) hasMediaPermission() else true,
            folderPath = savedFolderPath,
            folderName = savedFolderPath?.let {
                savedSourceFolderName() ?: displayNameForPickedFolder(it) ?: it
            },
        ),
    )
    val state: StateFlow<DateFixUiState> = _state.asStateFlow()

    private val cancelRequested = MutableStateFlow(false)
    private var pendingCandidates: List<DateFixCandidate> = emptyList()

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(hasPermission = granted) }
    }

    fun onFolderPicked(value: String) {
        val name = displayNameForPickedFolder(value) ?: value
        _state.update {
            it.copy(
                folderPath = value,
                folderName = name,
                preview = null,
                report = null,
                errorMessage = null,
            )
        }
    }

    fun preview() {
        val current = _state.value

        val source = when {
            current.isGalleryTarget -> {
                if (!current.hasPermission) {
                    _state.update { it.copy(errorMessage = "Allow access to photos and videos first") }
                    return
                }
                MediaSource.Gallery
            }

            current.folderPath != null -> MediaSource.Folder(current.folderPath)
            else -> {
                _state.update { it.copy(errorMessage = "Choose a folder first") }
                return
            }
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, report = null) }
            repository.preview(source)
                .onSuccess { preview ->
                    _state.update { it.copy(isLoading = false, preview = preview) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Preview failed",
                        )
                    }
                }
        }
    }

    fun toggleSkipMatching() {
        _state.update { it.copy(skipMatching = !it.skipMatching) }
    }

    fun requestApply() {
        val current = _state.value
        if (current.isApplying || current.preview == null) return

        if (current.updateCount == 0) {
            _state.update { it.copy(errorMessage = "Nothing to update") }
            return
        }

        _state.update { it.copy(showConfirmDialog = true, errorMessage = null) }
    }

    fun dismissConfirm() {
        _state.update { it.copy(showConfirmDialog = false) }
    }

    fun confirmApply() {
        val current = _state.value
        val preview = current.preview ?: return

        val selected = if (current.skipMatching) {
            preview.candidates.filter { !it.alreadyMatches }
        } else {
            preview.candidates
        }

        if (selected.isEmpty()) {
            _state.update {
                it.copy(showConfirmDialog = false, errorMessage = "Nothing to update")
            }
            return
        }

        _state.update { it.copy(showConfirmDialog = false) }

        if (isDateFixWriteApprovalRequired) {
            pendingCandidates = selected
            _state.update {
                it.copy(
                    awaitingWriteApproval = true,
                    pendingWriteIds = selected.map { candidate -> candidate.id },
                )
            }
        } else {
            startApply(selected)
        }
    }

    fun onWriteApprovalResult(approved: Boolean) {
        if (!approved) {
            pendingCandidates = emptyList()
            _state.update {
                it.copy(
                    awaitingWriteApproval = false,
                    pendingWriteIds = emptyList(),
                    errorMessage = "Write permission was not granted. Nothing was changed.",
                )
            }
            return
        }

        val candidates = pendingCandidates
        pendingCandidates = emptyList()
        _state.update { it.copy(awaitingWriteApproval = false, pendingWriteIds = emptyList()) }
        startApply(candidates)
    }

    fun cancelApply() {
        cancelRequested.value = true
    }

    fun dismissReport() {
        _state.update { it.copy(report = null) }
    }

    private fun startApply(candidates: List<DateFixCandidate>) {
        if (candidates.isEmpty()) return

        cancelRequested.value = false
        _state.update {
            it.copy(
                isApplying = true,
                progress = null,
                report = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            repository.apply(
                candidates = candidates,
                onProgress = { progress ->
                    _state.update { it.copy(progress = progress) }
                },
                shouldCancel = { cancelRequested.value },
            ).onSuccess { result ->
                val skipped = if (_state.value.skipMatching) {
                    _state.value.preview?.alreadyCount ?: 0
                } else {
                    0
                }
                _state.update {
                    it.copy(
                        isApplying = false,
                        progress = null,
                        report = result.copy(skippedCount = skipped),
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isApplying = false,
                        errorMessage = error.message ?: "Update failed",
                    )
                }
            }
        }
    }
}
