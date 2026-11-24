package id.neotica.removify.data.local

import id.neotica.removify.domain.model.DateFixCandidate
import id.neotica.removify.domain.model.DateFixFailure
import id.neotica.removify.domain.model.DateFixPreview
import id.neotica.removify.domain.model.DateFixProgress
import id.neotica.removify.domain.model.DateFixReport
import id.neotica.removify.domain.model.MediaSource
import id.neotica.removify.domain.model.MediaType
import id.neotica.removify.domain.repository.MediaDateFixRepository
import id.neotica.removify.platform.localEpochMs
import id.neotica.removify.utils.FilenameDateParser
import id.neotica.removify.utils.MediaClassification
import id.neotica.removify.utils.matchesFilenameDate
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JvmMediaDateFixRepository : MediaDateFixRepository {

    override suspend fun preview(source: MediaSource): Result<DateFixPreview> = withContext(Dispatchers.IO) {
        try {
            val folder = when (source) {
                is MediaSource.Folder -> File(source.path)
                else -> error("Date fixing supports folders on desktop")
            }
            require(folder.isDirectory) { "Folder not found: ${folder.absolutePath}" }

            val candidates = mutableListOf<DateFixCandidate>()
            val skipped = mutableListOf<String>()

            folder.walkTopDown()
                .onEnter { directory -> !directory.isHidden }
                .filter { file -> file.isFile && !file.isHidden }
                .forEach { file ->
                    val parsed = FilenameDateParser.parse(file.name)
                    if (parsed == null) {
                        skipped += file.name
                        return@forEach
                    }

                    val target = localEpochMs(parsed.year, parsed.month, parsed.day, parsed.hour, parsed.minute)
                    if (target == null) {
                        skipped += file.name
                        return@forEach
                    }

                    val type = MediaClassification.classify(file.name, null)
                    if (type == null) {
                        skipped += file.name
                        return@forEach
                    }

                    val current = file.lastModified().takeIf { it > 0 }
                    candidates += DateFixCandidate(
                        id = file.absolutePath,
                        name = file.name,
                        type = type,
                        targetEpochMs = target,
                        currentEpochMs = current,
                        alreadyMatches = current != null &&
                            matchesFilenameDate(current, target, parsed.hasTime),
                    )
                }

            Result.success(
                DateFixPreview(
                    candidates = candidates.sortedBy { it.id },
                    skippedCount = skipped.size,
                    skippedExamples = skipped.take(5),
                ),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    override suspend fun apply(
        candidates: List<DateFixCandidate>,
        onProgress: (DateFixProgress) -> Unit,
        shouldCancel: () -> Boolean,
    ): Result<DateFixReport> = withContext(Dispatchers.IO) {
        try {
            var updated = 0
            var failed = 0
            var cancelled = false
            val failures = mutableListOf<DateFixFailure>()

            for ((index, candidate) in candidates.withIndex()) {
                if (shouldCancel()) {
                    cancelled = true
                    break
                }

                onProgress(DateFixProgress(index, candidates.size, updated, failed, candidate.name))

                try {
                    val file = File(candidate.id)
                    if (!file.isFile) error("File not found")
                    Files.setLastModifiedTime(file.toPath(), FileTime.fromMillis(candidate.targetEpochMs))
                    setCreationTimeBestEffort(file, candidate.targetEpochMs)
                    updated++
                } catch (error: Throwable) {
                    failed++
                    failures += DateFixFailure(candidate.name, error.message ?: "Failed")
                }
            }

            onProgress(DateFixProgress(candidates.size, candidates.size, updated, failed, null))

            Result.success(
                DateFixReport(
                    updatedCount = updated,
                    failedCount = failed,
                    skippedCount = 0,
                    cancelled = cancelled,
                    failures = failures,
                ),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private fun setCreationTimeBestEffort(file: File, epochMs: Long) {
        val osName = System.getProperty("os.name").lowercase()

        if (osName.contains("mac")) {
            val setFile = File("/usr/bin/SetFile")
            if (!setFile.canExecute()) return

            val formatted = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }.format(Date(epochMs))

            runCatching {
                ProcessBuilder(setFile.absolutePath, "-d", formatted, file.absolutePath)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor()
            }
            return
        }

        if (osName.contains("win")) {
            runCatching {
                val view = Files.getFileAttributeView(file.toPath(), BasicFileAttributeView::class.java)
                view?.setTimes(null, null, FileTime.fromMillis(epochMs))
            }
        }
    }
}
