package id.neotica.removify.ui.feature.source

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.neotica.removify.domain.model.MediaSource
import id.neotica.removify.domain.repository.MediaRepository
import id.neotica.removify.platform.clearSourceFolder
import id.neotica.removify.platform.displayNameForPickedFolder
import id.neotica.removify.platform.mediaSourceForPickedLocation
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

    private fun load(source: MediaSource, displayName: String, fromSaved: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    folderName = displayName,
                    isLoading = true,
                    errorMessage = null,
                )
            }

            mediaRepository.loadMedia(
                source = source,
                includeImages = true,
                includeVideos = true,
            ).onSuccess { items ->
                sessionStore.setSource(source, items)
                _state.update {
                    it.copy(
                        mediaCount = items.size,
                        isLoading = false,
                    )
                }
            }.onFailure { error ->
                if (fromSaved) clearSourceFolder()
                _state.update {
                    it.copy(
                        mediaCount = 0,
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load media",
                    )
                }
            }
        }
    }
}
