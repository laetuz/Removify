package id.neotica.removify.data.local

import id.neotica.removify.domain.model.TrashEntry
import id.neotica.removify.domain.model.TrashMoveResult
import id.neotica.removify.domain.model.MediaItem
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

class DesktopTrashStore(private val baseDir: File) {

    private val trashDir = File(baseDir, "trash")
    private val manifestFile = File(baseDir, "trash.json")
    private val serializer = ListSerializer(TrashEntry.serializer())
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun moveToTrash(items: List<MediaItem>, sessionId: String): Result<TrashMoveResult> = runCatching {
        trashDir.mkdirs()
        val entries = readEntries().toMutableList()
        var moved = 0
        var failed = 0
        var bytes = 0L
        val timestamp = System.currentTimeMillis()

        for (item in items) {
            val source = File(item.id)
            if (!source.isFile) {
                failed++
                continue
            }

            try {
                val target = File(trashDir, uniqueTrashName(source.name, timestamp))
                moveFile(source, target)
                entries += TrashEntry(
                    id = UUID.randomUUID().toString(),
                    originalPath = source.absolutePath,
                    trashedPath = target.absolutePath,
                    name = source.name,
                    type = item.type,
                    sizeBytes = item.sizeBytes,
                    trashedAtEpochMs = timestamp,
                    sessionId = sessionId,
                )
                moved++
                bytes += item.sizeBytes
            } catch (_: Exception) {
                failed++
            }
        }

        writeEntries(entries)
        TrashMoveResult(movedCount = moved, failedCount = failed, bytes = bytes)
    }

    fun listEntries(): Result<List<TrashEntry>> = runCatching {
        readEntries().filter { File(it.trashedPath).isFile }
    }

    fun restore(entries: List<TrashEntry>): Result<Int> = runCatching {
        val remaining = readEntries().toMutableList()
        var restored = 0

        for (entry in entries) {
            val trashed = File(entry.trashedPath)
            if (!trashed.isFile) {
                remaining.removeAll { it.id == entry.id }
                continue
            }

            try {
                val original = File(entry.originalPath)
                original.parentFile?.mkdirs()
                val target = if (original.exists()) uniqueRestoreName(original) else original
                moveFile(trashed, target)
                remaining.removeAll { it.id == entry.id }
                restored++
            } catch (_: Exception) {
                // Leave the entry in the manifest so it can be retried.
            }
        }

        writeEntries(remaining)
        restored
    }

    fun empty(): Result<Long> = runCatching {
        var freed = 0L
        for (entry in readEntries()) {
            val file = File(entry.trashedPath)
            if (file.isFile) {
                val length = file.length()
                if (file.delete()) freed += length
            }
        }
        writeEntries(emptyList())
        freed
    }

    private fun readEntries(): List<TrashEntry> {
        if (!manifestFile.isFile) return emptyList()
        return runCatching {
            json.decodeFromString(serializer, manifestFile.readText())
        }.getOrElse { emptyList() }
    }

    private fun writeEntries(entries: List<TrashEntry>) {
        baseDir.mkdirs()
        val temp = File(baseDir, "trash.json.tmp")
        temp.writeText(json.encodeToString(serializer, entries))
        try {
            Files.move(
                temp.toPath(),
                manifestFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: Exception) {
            Files.move(temp.toPath(), manifestFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun moveFile(source: File, target: File) {
        target.parentFile?.mkdirs()
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            if (source.length() != target.length()) {
                target.delete()
                error("Size mismatch while moving ${source.name}")
            }
            if (!source.delete()) {
                target.delete()
                error("Failed to remove source ${source.name}")
            }
        }
    }

    private fun uniqueTrashName(name: String, timestamp: Long): String {
        var candidate = "${timestamp}_$name"
        var index = 1
        while (File(trashDir, candidate).exists()) {
            candidate = "${timestamp}_${index}_$name"
            index++
        }
        return candidate
    }

    private fun uniqueRestoreName(target: File): File {
        val parent = target.parentFile
        val base = target.nameWithoutExtension
        val extension = target.extension.takeIf { it.isNotEmpty() }?.let { ".$it" } ?: ""
        var candidate = File(parent, "$base (restored)$extension")
        var index = 1
        while (candidate.exists()) {
            candidate = File(parent, "$base (restored $index)$extension")
            index++
        }
        return candidate
    }
}
