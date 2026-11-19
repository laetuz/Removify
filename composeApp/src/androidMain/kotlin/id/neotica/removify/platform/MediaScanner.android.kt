package id.neotica.removify.platform

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.MediaSource
import id.neotica.removify.domain.model.MediaType
import id.neotica.removify.utils.MediaClassification
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun scanMedia(
    source: MediaSource,
    includeImages: Boolean,
    includeVideos: Boolean,
): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
    try {
        val items = when (source) {
            MediaSource.Gallery -> emptyList()
            is MediaSource.Folder -> scanFileTree(File(source.path), includeImages, includeVideos)
            is MediaSource.Tree -> scanDocumentTree(source.treeUri, includeImages, includeVideos)
        }
        Result.success(items.sortedByDescending { it.dateTakenEpochMs })
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
}

private fun scanDocumentTree(
    treeUri: String,
    includeImages: Boolean,
    includeVideos: Boolean,
): List<MediaItem> {
    val context = AndroidAppContext.appContext ?: error("Android context is not available")
    val root = DocumentFile.fromTreeUri(context, Uri.parse(treeUri))
        ?: error("Folder is not accessible. Choose it again.")

    val results = mutableListOf<MediaItem>()
    val queue = ArrayDeque<DocumentFile>()
    queue.add(root)

    while (queue.isNotEmpty()) {
        val document = queue.removeFirst()

        if (document.isDirectory) {
            document.listFiles().forEach { child ->
                val name = child.name
                if (name != null && !name.startsWith(".")) {
                    queue.add(child)
                }
            }
            continue
        }

        if (!document.isFile) continue

        val name = document.name ?: continue
        val type = MediaClassification.classify(name, document.type) ?: continue
        if (type == MediaType.IMAGE && !includeImages) continue
        if (type == MediaType.VIDEO && !includeVideos) continue

        results += MediaItem(
            id = document.uri.toString(),
            name = name,
            type = type,
            sizeBytes = document.length().coerceAtLeast(0),
            dateTakenEpochMs = document.lastModified(),
            durationMs = null,
            bucket = document.parentFile?.name,
        )
    }

    return results
}

private fun scanFileTree(
    root: File,
    includeImages: Boolean,
    includeVideos: Boolean,
): List<MediaItem> {
    if (!root.exists()) error("Folder not found: ${root.absolutePath}")

    return root.walkTopDown()
        .onEnter { directory -> !directory.isHidden }
        .filter { file -> file.isFile && !file.isHidden }
        .mapNotNull { file ->
            val type = MediaClassification.classify(file.name, null) ?: return@mapNotNull null
            if (type == MediaType.IMAGE && !includeImages) return@mapNotNull null
            if (type == MediaType.VIDEO && !includeVideos) return@mapNotNull null

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
        .toList()
}
