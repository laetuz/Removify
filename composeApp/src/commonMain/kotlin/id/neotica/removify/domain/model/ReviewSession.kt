package id.neotica.removify.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ReviewSession(
    val id: String,
    val startedAtEpochMs: Long,
    val source: MediaSource,
    val decisions: Map<String, SwipeDecision> = emptyMap(),
    val finishedAtEpochMs: Long? = null,
)
