package id.neotica.removify.ui.feature.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.neotica.removify.data.SessionSnapshotRepository
import id.neotica.removify.domain.model.SessionSnapshot
import id.neotica.removify.domain.model.SwipeDecision
import id.neotica.removify.domain.repository.TrashRepository
import id.neotica.removify.domain.service.DeckEngine
import id.neotica.removify.platform.nowEpochMs
import id.neotica.removify.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class SwipeDeckViewModel(
    private val trashRepository: TrashRepository,
    private val sessionStore: SessionStore,
    private val snapshotRepository: SessionSnapshotRepository,
) : ViewModel() {

    private val draft = sessionStore.draft.value

    private val engine = DeckEngine(
        items = draft.items,
        initialDecisions = draft.decisions,
        initialSkipCounts = draft.skipCounts,
        initialQueue = if (draft.resumed) draft.queueIds else null,
    )

    private val snapshotScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val snapshotMutex = Mutex()
    private var pendingSnapshotSave: Job? = null

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
        persistSnapshot()
    }

    fun undo() {
        if (_state.value.isCommitting || _state.value.isCompleted) return
        engine.undo()
        syncState()
        persistSnapshot()
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
                    clearSnapshot()
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
        clearSnapshot()
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

    private fun persistSnapshot() {
        val current = sessionStore.draft.value
        val source = current.source ?: return
        val data = engine.snapshot()

        val snapshot = SessionSnapshot(
            source = source,
            displayName = current.displayName,
            startedAtEpochMs = current.startedAtEpochMs,
            updatedAtEpochMs = nowEpochMs(),
            decisions = data.decisions,
            skipCounts = data.skipCounts,
            queue = data.queue,
        )

        pendingSnapshotSave?.cancel()
        pendingSnapshotSave = snapshotScope.launch {
            delay(300)
            snapshotMutex.withLock { snapshotRepository.save(snapshot) }
        }
    }

    private fun clearSnapshot() {
        pendingSnapshotSave?.cancel()
        pendingSnapshotSave = null
        snapshotScope.launch {
            snapshotMutex.withLock { snapshotRepository.clear() }
        }
    }
}
