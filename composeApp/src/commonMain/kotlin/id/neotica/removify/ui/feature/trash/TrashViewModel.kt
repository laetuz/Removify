package id.neotica.removify.ui.feature.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.neotica.removify.domain.model.TrashEntry
import id.neotica.removify.domain.repository.TrashRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrashViewModel(
    private val trashRepository: TrashRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TrashUiState())
    val state: StateFlow<TrashUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            trashRepository.listTrash()
                .onSuccess { entries ->
                    _state.update { it.copy(entries = entries, isLoading = false) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load Trash",
                        )
                    }
                }
        }
    }

    fun restore(entry: TrashEntry) {
        restoreEntries(listOf(entry))
    }

    fun restoreAll() {
        restoreEntries(_state.value.entries)
    }

    fun requestEmpty() {
        if (_state.value.entries.isEmpty()) return
        _state.update { it.copy(showEmptyDialog = true) }
    }

    fun dismissEmpty() {
        _state.update { it.copy(showEmptyDialog = false) }
    }

    fun confirmEmpty() {
        _state.update { it.copy(showEmptyDialog = false) }
        viewModelScope.launch {
            trashRepository.emptyTrash()
                .onSuccess { freedBytes ->
                    _state.update { it.copy(lastFreedBytes = freedBytes) }
                    refresh()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(errorMessage = error.message ?: "Failed to empty Trash")
                    }
                }
        }
    }

    private fun restoreEntries(entries: List<TrashEntry>) {
        if (entries.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(errorMessage = null) }
            trashRepository.restore(entries)
                .onSuccess { refresh() }
                .onFailure { error ->
                    _state.update {
                        it.copy(errorMessage = error.message ?: "Failed to restore items")
                    }
                }
        }
    }
}
