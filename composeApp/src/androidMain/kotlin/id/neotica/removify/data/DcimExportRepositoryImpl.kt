package id.neotica.removify.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import id.neotica.removify.domain.model.DcimCopyResult
import id.neotica.removify.domain.model.DcimExportFailure
import id.neotica.removify.domain.model.DcimExportProgress
import id.neotica.removify.domain.model.DcimItem
import id.neotica.removify.domain.model.MediaType
import id.neotica.removify.domain.repository.DcimExportRepository
import id.neotica.removify.platform.AndroidAppContext
import id.neotica.removify.utils.ExportNaming
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class ExportCancelledException : Exception()

class DcimExportRepositoryImpl : DcimExportRepository {

    override suspend fun listDcim(): Result<List<DcimItem>> = withContext(Dispatchers.IO) {
        try {
            val context = requireContext()
            val items = buildList {
                addAll(queryCollection(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaType.IMAGE))
                addAll(queryCollection(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaType.VIDEO))
            }.sortedByDescending { it.dateTakenEpochMs }
            Result.success(items)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    override suspend fun copyToTree(
        items: List<DcimItem>,
        treeUri: String,
        shouldCancel: () -> Boolean,
        onProgress: (DcimExportProgress) -> Unit,
    ): Result<DcimCopyResult> = withContext(Dispatchers.IO) {
        try {
            Result.success(doCopy(items, treeUri, shouldCancel, onProgress))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private fun doCopy(
        items: List<DcimItem>,
        treeUri: String,
        shouldCancel: () -> Boolean,
        onProgress: (DcimExportProgress) -> Unit,
    ): DcimCopyResult {
        val context = requireContext()
        val resolver = context.contentResolver
        val root = DocumentFile.fromTreeUri(context, Uri.parse(treeUri))
            ?: error("Destination folder is not accessible")

        val totalBytes = items.sumOf { it.sizeBytes }
        val copied = mutableListOf<DcimItem>()
        val failures = mutableListOf<DcimExportFailure>()
        var copiedBytes = 0L
        var cancelled = false

        for ((index, item) in items.withIndex()) {
            if (shouldCancel()) {
                cancelled = true
                break
            }

            onProgress(
                DcimExportProgress(
                    processedCount = index,
                    totalCount = items.size,
                    copiedBytes = copiedBytes,
                    totalBytes = totalBytes,
                    currentName = item.name,
                ),
            )

            var destination: DocumentFile? = null
            try {
                val parent = ensureFolders(root, ExportNaming.relativeFolders(item.relativePath))
                val existingNames = parent.listFiles().mapNotNull { it.name }.toSet()
                val fileName = ExportNaming.uniqueName(
                    ExportNaming.sanitizeFileName(item.name),
                    existingNames,
                )

                destination = parent.createFile(
                    item.mimeType.ifBlank { "application/octet-stream" },
                    fileName,
                ) ?: error("Cannot create destination file")

                val sourceHash = hashCopy(
                    resolver = resolver,
                    source = Uri.parse(item.contentUri),
                    destination = destination.uri,
                    shouldCancel = shouldCancel,
                )
                val destinationHash = hashOf(resolver, destination.uri)
                val destinationSize = destination.length()

                val sizeMatches = destinationSize < 0 ||
                    item.sizeBytes <= 0 ||
                    destinationSize == item.sizeBytes
                if (sourceHash != destinationHash || !sizeMatches) {
                    error("Verification failed")
                }

                copied += item
                copiedBytes += item.sizeBytes
            } catch (cancel: ExportCancelledException) {
                destination?.delete()
                cancelled = true
                break
            } catch (error: Throwable) {
                destination?.delete()
                failures += DcimExportFailure(item.name, error.message ?: "Copy failed")
            }
        }

        onProgress(
            DcimExportProgress(
                processedCount = items.size,
                totalCount = items.size,
                copiedBytes = copiedBytes,
                totalBytes = totalBytes,
                currentName = null,
            ),
        )

        return DcimCopyResult(
            copied = copied,
            copiedBytes = copiedBytes,
            failures = failures,
            cancelled = cancelled,
        )
    }

    private fun queryCollection(
        context: Context,
        collection: Uri,
        type: MediaType,
    ): List<DcimItem> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.RELATIVE_PATH,
            "datetaken",
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("DCIM/%")
        val results = mutableListOf<DcimItem>()

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "datetaken DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val mimeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            val takenIndex = cursor.getColumnIndex("datetaken")
            val modifiedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

            if (idIndex < 0 || nameIndex < 0 || pathIndex < 0) return emptyList()

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex) ?: continue
                val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                val mime = if (mimeIndex >= 0) cursor.getString(mimeIndex).orEmpty() else ""
                val relativePath = (cursor.getString(pathIndex) ?: "DCIM").trimEnd('/')
                val taken = if (takenIndex >= 0) cursor.getLong(takenIndex) else 0L
                val modified = if (modifiedIndex >= 0) cursor.getLong(modifiedIndex) * 1000 else 0L

                results += DcimItem(
                    id = id,
                    contentUri = ContentUris.withAppendedId(collection, id).toString(),
                    name = name,
                    relativePath = relativePath,
                    mimeType = mime,
                    sizeBytes = size,
                    dateTakenEpochMs = if (taken > 0) taken else modified,
                    type = type,
                )
            }
        }

        return results
    }

    private fun ensureFolders(root: DocumentFile, folders: List<String>): DocumentFile {
        var current = root
        for (folder in folders) {
            val existing = current.findFile(folder)
            current = when {
                existing == null -> current.createDirectory(folder)
                    ?: error("Cannot create destination folder '$folder'")

                existing.isDirectory -> existing
                else -> error("A file named '$folder' already exists at the destination")
            }
        }
        return current
    }

    private fun hashCopy(
        resolver: ContentResolver,
        source: Uri,
        destination: Uri,
        shouldCancel: () -> Boolean,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = resolver.openInputStream(source) ?: error("Cannot open source file")
        val output = resolver.openOutputStream(destination) ?: error("Cannot open destination file")

        input.use { sourceStream ->
            output.use { destinationStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    if (shouldCancel()) throw ExportCancelledException()
                    val read = sourceStream.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                    destinationStream.write(buffer, 0, read)
                }
                destinationStream.flush()
            }
        }

        return digest.digest().toHex()
    }

    private fun hashOf(resolver: ContentResolver, uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = resolver.openInputStream(uri) ?: error("Cannot verify destination file")
        input.use { stream ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { byte -> ((byte.toInt() and 0xFF) + 0x100).toString(16).substring(1) }

    private fun requireContext(): Context =
        AndroidAppContext.appContext ?: error("Android context is not available")

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}
