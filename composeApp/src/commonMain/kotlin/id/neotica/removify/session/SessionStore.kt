package id.neotica.removify.session

import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.MediaSource
import id.neotica.removify.domain.model.SessionSummary
import id.neotica.removify.domain.model.SwipeDecision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DraftSession(
    val source: MediaSource? = null,
    val items: List<MediaItem> = emptyList(),
    val decisions: Map<String, SwipeDecision> = emptyMap(),
    val summary: SessionSummary? = null,
)

class SessionStore {
    private val _draft = MutableStateFlow(DraftSession())
    val draft: StateFlow<DraftSession> = _draft.asStateFlow()

    fun setSource(source: MediaSource, items: List<MediaItem>) {
        _draft.value = DraftSession(source = source, items = items)
    }

    fun setResult(decisions: Map<String, SwipeDecision>, summary: SessionSummary) {
        _draft.update { it.copy(decisions = decisions, summary = summary) }
    }

    fun clear() {
        _draft.value = DraftSession()
    }
}
