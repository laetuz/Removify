package id.neotica.removify.data

import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.MediaSource
import id.neotica.removify.domain.repository.MediaRepository
import id.neotica.removify.platform.scanMedia

class MediaRepositoryImpl : MediaRepository {
    override suspend fun loadMedia(
        source: MediaSource,
        includeImages: Boolean,
        includeVideos: Boolean,
    ): Result<List<MediaItem>> = scanMedia(source, includeImages, includeVideos)
}
