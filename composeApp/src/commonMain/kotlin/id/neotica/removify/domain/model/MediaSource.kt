package id.neotica.removify.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface MediaSource {
    @Serializable
    data object Gallery : MediaSource

    @Serializable
    data class Folder(val path: String) : MediaSource
}
