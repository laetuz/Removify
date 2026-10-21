package id.neotica.removify.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MediaType { IMAGE, VIDEO }

@Serializable
data class MediaItem(
    val id: String,
    val name: String,
    val type: MediaType,
    val sizeBytes: Long,
    val dateTakenEpochMs: Long,
    val durationMs: Long? = null,
    val bucket: String? = null,
)
