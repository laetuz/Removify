package id.neotica.removify.ui.feature.source

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.neotica.removify.domain.model.MediaSource
import id.neotica.removify.domain.repository.MediaRepository
import id.neotica.removify.platform.pickFolder
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

    fun chooseFolder() {
        viewModelScope.launch {
            val path = pickFolder() ?: return@launch
            loadFolder(path)
        }
    }

    fun loadFolder(path: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    folderPath = path,
                    isLoading = true,
                    errorMessage = null,
                )
            }

            val source = MediaSource.Folder(path)
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
