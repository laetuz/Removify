package id.neotica.removify.platform

import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.MediaSource

actual suspend fun scanMedia(
    source: MediaSource,
    includeImages: Boolean,
    includeVideos: Boolean,
): Result<List<MediaItem>> = Result.success(emptyList())
