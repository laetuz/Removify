package id.neotica.removify.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val includeImages: Boolean = true,
    val includeVideos: Boolean = true,
    val sortNewestFirst: Boolean = true,
    val lastDesktopFolder: String? = null,
    val skippedRequeuedAtEnd: Boolean = true,
)
