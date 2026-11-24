package id.neotica.removify.session

import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.MediaSource
import id.neotica.removify.domain.model.SessionSummary
import id.neotica.removify.domain.model.SwipeDecision
import id.neotica.removify.platform.nowEpochMs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DraftSession(
    val source: MediaSource? = null,
    val displayName: String = "",
    val items: List<MediaItem> = emptyList(),
    val decisions: Map<String, SwipeDecision> = emptyMap(),
    val skipCounts: Map<String, Int> = emptyMap(),
    val queueIds: List<String> = emptyList(),
    val resumed: Boolean = false,
    val startedAtEpochMs: Long = 0,
    val summary: SessionSummary? = null,
)

class SessionStore {
    private val _draft = MutableStateFlow(DraftSession())
    val draft: StateFlow<DraftSession> = _draft.asStateFlow()

    fun setSource(source: MediaSource, displayName: String, items: List<MediaItem>) {
        _draft.value = DraftSession(
            source = source,
            displayName = displayName,
            items = items,
            startedAtEpochMs = nowEpochMs(),
        )
    }

    fun restoreSource(
        source: MediaSource,
        displayName: String,
        items: List<MediaItem>,
        decisions: Map<String, SwipeDecision>,
        skipCounts: Map<String, Int>,
        queueIds: List<String>,
        startedAtEpochMs: Long,
    ) {
        _draft.value = DraftSession(
            source = source,
            displayName = displayName,
            items = items,
            decisions = decisions,
            skipCounts = skipCounts,
            queueIds = queueIds,
            resumed = true,
            startedAtEpochMs = startedAtEpochMs,
        )
    }

    fun setResult(decisions: Map<String, SwipeDecision>, summary: SessionSummary) {
        _draft.update { it.copy(decisions = decisions, summary = summary) }
    }

    fun clear() {
        _draft.value = DraftSession()
    }
}
