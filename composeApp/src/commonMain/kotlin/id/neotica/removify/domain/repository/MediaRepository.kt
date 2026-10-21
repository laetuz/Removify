package id.neotica.removify.domain.repository

import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.MediaSource

interface MediaRepository {
    suspend fun loadMedia(
        source: MediaSource,
        includeImages: Boolean,
        includeVideos: Boolean,
    ): Result<List<MediaItem>>
}
