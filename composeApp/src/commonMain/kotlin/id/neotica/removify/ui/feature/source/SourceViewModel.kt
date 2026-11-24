package id.neotica.removify.ui.feature.source

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.neotica.removify.data.SessionSnapshotRepository
import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.MediaSource
import id.neotica.removify.domain.model.SessionSnapshot
import id.neotica.removify.domain.repository.MediaRepository
import id.neotica.removify.platform.clearSourceFolder
import id.neotica.removify.platform.displayNameForPickedFolder
import id.neotica.removify.platform.mediaSourceForPickedLocation
import id.neotica.removify.platform.nowEpochMs
import id.neotica.removify.platform.saveSourceFolder
import id.neotica.removify.platform.savedSourceFolderName
import id.neotica.removify.platform.savedSourceFolderUri
import id.neotica.removify.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SourceViewModel(
    private val mediaRepository: MediaRepository,
    private val sessionStore: SessionStore,
    private val snapshotRepository: SessionSnapshotRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SourceUiState())
    val state: StateFlow<SourceUiState> = _state.asStateFlow()

    init {
        val savedUri = savedSourceFolderUri()
        if (savedUri != null) {
            val name = savedSourceFolderName()
                ?: displayNameForPickedFolder(savedUri)
                ?: savedUri
            load(
                source = mediaSourceForPickedLocation(savedUri),
                displayName = name,
                fromSaved = true,
            )
        }
    }

    fun onFolderPicked(value: String) {
        val name = displayNameForPickedFolder(value) ?: value
        saveSourceFolder(value, name)
        load(
            source = mediaSourceForPickedLocation(value),
            displayName = name,
            fromSaved = false,
        )
    }

    fun startOver() {
        val draft = sessionStore.draft.value
        val source = draft.source ?: return
        val displayName = draft.displayName
        val items = draft.items

        viewModelScope.launch {
            snapshotRepository.clear()
            sessionStore.setSource(source, displayName, items)
            _state.update { it.copy(resumeAvailable = false, resumeDecided = 0) }
        }
    }

    private fun load(source: MediaSource, displayName: String, fromSaved: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    folderName = displayName,
                    mediaCount = 0,
                    isLoading = true,
                    errorMessage = null,
                    resumeAvailable = false,
                    resumeDecided = 0,
                )
            }

            if (!fromSaved) {
                sessionStore.clear()
                snapshotRepository.clear()
            }

            mediaRepository.loadMedia(
                source = source,
                includeImages = true,
                includeVideos = true,
            ).onSuccess { items ->
                val ids = items.mapTo(HashSet()) { it.id }
                val snapshot = if (fromSaved) snapshotRepository.load() else null
                val usable = snapshot?.takeIf {
                    it.source == source && isSnapshotUsable(it, ids)
                }

                if (usable != null) {
                    sessionStore.restoreSource(
                        source = source,
                        displayName = usable.displayName.ifBlank { displayName },
                        items = items,
                        decisions = usable.decisions,
                        skipCounts = usable.skipCounts,
                        queueIds = usable.queue,
                        startedAtEpochMs = usable.startedAtEpochMs.takeIf { it > 0 } ?: nowEpochMs(),
                    )
                    _state.update {
                        it.copy(
                            mediaCount = items.size,
                            isLoading = false,
                            resumeAvailable = true,
                            resumeDecided = usable.decisions.keys.count { id -> id in ids },
                        )
                    }
                } else {
                    if (snapshot != null) snapshotRepository.clear()
                    sessionStore.setSource(source, displayName, items)
                    _state.update {
                        it.copy(
                            mediaCount = items.size,
                            isLoading = false,
                            resumeAvailable = false,
                            resumeDecided = 0,
                        )
                    }
                }
            }.onFailure { error ->
                if (fromSaved) {
                    clearSourceFolder()
                    snapshotRepository.clear()
                }
                _state.update {
                    it.copy(
                        mediaCount = 0,
                        isLoading = false,
                        resumeAvailable = false,
                        resumeDecided = 0,
                        errorMessage = if (fromSaved) {
                            "Saved folder is no longer accessible - choose it again."
                        } else {
                            error.message ?: "Failed to load media"
                        },
                    )
                }
            }
        }
    }

    private fun isSnapshotUsable(snapshot: SessionSnapshot, ids: Set<String>): Boolean {
        if (ids.isEmpty()) return false
        return snapshot.queue.any { it in ids } ||
            snapshot.decisions.keys.any { it in ids } ||
            snapshot.skipCounts.keys.any { it in ids }
    }
}
