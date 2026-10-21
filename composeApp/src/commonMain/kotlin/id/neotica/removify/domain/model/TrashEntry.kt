package id.neotica.removify.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TrashEntry(
    val id: String,
    val originalPath: String,
    val trashedPath: String,
    val name: String,
    val type: MediaType,
    val sizeBytes: Long,
    val trashedAtEpochMs: Long,
    val sessionId: String,
)
