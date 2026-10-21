package id.neotica.removify.platform

import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.MediaSource
import id.neotica.removify.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")
private val videoExtensions = setOf("mp4", "mov", "m4v", "mkv", "webm", "avi")

actual suspend fun scanMedia(
    source: MediaSource,
    includeImages: Boolean,
    includeVideos: Boolean,
): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
    runCatching {
        val folder = when (source) {
            MediaSource.Gallery -> error("Gallery source is not supported on desktop")
            is MediaSource.Folder -> File(source.path)
        }
        require(folder.isDirectory) { "Folder not found: ${folder.absolutePath}" }

        val rootPath = folder.absolutePath
        val trashPath = File(appDataDir(), "trash").absolutePath

        folder.walkTopDown()
            .onEnter { dir ->
                val path = dir.absolutePath
                path == rootPath || (!dir.isHidden && path != trashPath)
            }
            .filter { it.isFile && !it.isHidden }
            .mapNotNull { file ->
                val extension = file.extension.lowercase()
                val type = when {
                    extension in imageExtensions && includeImages -> MediaType.IMAGE
                    extension in videoExtensions && includeVideos -> MediaType.VIDEO
                    else -> null
                } ?: return@mapNotNull null

                MediaItem(
                    id = file.absolutePath,
                    name = file.name,
                    type = type,
                    sizeBytes = file.length(),
                    dateTakenEpochMs = file.lastModified(),
                    durationMs = null,
                    bucket = file.parent,
                )
            }
            .sortedByDescending { it.dateTakenEpochMs }
            .toList()
    }
}
