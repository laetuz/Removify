package id.neotica.removify.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SessionSnapshot(
    val version: Int = 1,
    val source: MediaSource,
    val displayName: String,
    val startedAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val decisions: Map<String, SwipeDecision> = emptyMap(),
    val skipCounts: Map<String, Int> = emptyMap(),
    val queue: List<String> = emptyList(),
)
