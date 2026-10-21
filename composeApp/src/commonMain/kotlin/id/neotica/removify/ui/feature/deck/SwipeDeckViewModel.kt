package id.neotica.removify.ui.feature.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.neotica.removify.domain.model.SwipeDecision
import id.neotica.removify.domain.repository.TrashRepository
import id.neotica.removify.domain.service.DeckEngine
import id.neotica.removify.platform.nowEpochMs
import id.neotica.removify.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class SwipeDeckViewModel(
    private val trashRepository: TrashRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val engine = DeckEngine(sessionStore.draft.value.items)

    private val _state = MutableStateFlow(SwipeDeckUiState())
    val state: StateFlow<SwipeDeckUiState> = _state.asStateFlow()

    init {
        syncState()
    }

    fun decide(decision: SwipeDecision) {
        if (_state.value.isCommitting || _state.value.isCompleted) return
        when (decision) {
            SwipeDecision.SKIP -> engine.skip()
            else -> engine.decide(decision)
        }
        syncState()
    }

    fun undo() {
        if (_state.value.isCommitting || _state.value.isCompleted) return
        engine.undo()
        syncState()
    }

    fun requestFinish() {
        if (_state.value.isCommitting || _state.value.isCompleted) return
        if (engine.trashItems().isEmpty()) {
            completeWithoutTrash()
        } else {
            _state.update { it.copy(showFinishDialog = true, errorMessage = null) }
        }
    }

    fun dismissFinishDialog() {
        _state.update { it.copy(showFinishDialog = false) }
    }

    fun confirmFinish() {
        if (_state.value.isCommitting) return
        _state.update {
            it.copy(
                showFinishDialog = false,
                isCommitting = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            val trashed = engine.trashItems()
            val sessionId = "s-${nowEpochMs()}-${Random.nextInt(1000, 10000)}"

            trashRepository.moveToTrash(trashed, sessionId)
                .onSuccess {
                    sessionStore.setResult(engine.decisionsMap, engine.summary())
                    _state.update { it.copy(isCommitting = false, isCompleted = true) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isCommitting = false,
                            errorMessage = error.message ?: "Failed to move items to Trash",
                        )
                    }
                }
        }
    }

    private fun completeWithoutTrash() {
        sessionStore.setResult(engine.decisionsMap, engine.summary())
        _state.update { it.copy(isCompleted = true) }
    }

    private fun syncState() {
        val summary = engine.summary()
        _state.update {
            it.copy(
                currentItem = engine.current,
                nextItems = engine.nextUp,
                totalCount = engine.total,
                decidedCount = engine.decidedCount,
                keptCount = summary.keptCount,
                trashedCount = summary.trashCount,
                skippedCount = engine.skippedCount,
                trashBytes = summary.trashBytes,
                isDeckFinished = engine.isFinished,
            )
        }
    }
}
