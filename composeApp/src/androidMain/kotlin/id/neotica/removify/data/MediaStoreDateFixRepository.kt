package id.neotica.removify.data

import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import id.neotica.removify.domain.model.DateFixCandidate
import id.neotica.removify.domain.model.DateFixFailure
import id.neotica.removify.domain.model.DateFixPreview
import id.neotica.removify.domain.model.DateFixProgress
import id.neotica.removify.domain.model.DateFixReport
import id.neotica.removify.domain.model.MediaSource
import id.neotica.removify.domain.model.MediaType
import id.neotica.removify.domain.repository.MediaDateFixRepository
import id.neotica.removify.platform.AndroidAppContext
import id.neotica.removify.platform.localEpochMs
import id.neotica.removify.utils.FilenameDateParser
import id.neotica.removify.utils.matchesFilenameDate
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreDateFixRepository : MediaDateFixRepository {

    override suspend fun preview(source: MediaSource): Result<DateFixPreview> =
        withContext(Dispatchers.IO) {
            try {
                require(source == MediaSource.Gallery) {
                    "Date fixing uses the gallery on Android"
                }
                val context = AndroidAppContext.appContext
                    ?: error("Android context is not available")

                val candidates = mutableListOf<DateFixCandidate>()
                val skipped = mutableListOf<String>()

                collectCandidates(
                    context = context,
                    collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    type = MediaType.IMAGE,
                    candidates = candidates,
                    skipped = skipped,
                )
                collectCandidates(
                    context = context,
                    collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    type = MediaType.VIDEO,
                    candidates = candidates,
                    skipped = skipped,
                )

                Result.success(
                    DateFixPreview(
                        candidates = candidates.sortedBy { it.name },
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
            val context = AndroidAppContext.appContext
                ?: error("Android context is not available")
            val resolver = context.contentResolver

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
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DATE_TAKEN, candidate.targetEpochMs)
                        put(MediaStore.MediaColumns.DATE_MODIFIED, candidate.targetEpochMs / 1000)
                    }
                    val rows = resolver.update(Uri.parse(candidate.id), values, null, null)
                    if (rows > 0) {
                        updated++
                    } else {
                        failed++
                        failures += DateFixFailure(candidate.name, "MediaStore rejected the update")
                    }
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

    private fun collectCandidates(
        context: Context,
        collection: Uri,
        type: MediaType,
        candidates: MutableList<DateFixCandidate>,
        skipped: MutableList<String>,
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )

        context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val takenIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
            val modifiedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)

            if (idIndex < 0 || nameIndex < 0) return

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex) ?: continue

                val parsed = FilenameDateParser.parse(name)
                if (parsed == null) {
                    skipped += name
                    continue
                }

                val target = localEpochMs(
                    parsed.year,
                    parsed.month,
                    parsed.day,
                    parsed.hour,
                    parsed.minute,
                )
                if (target == null) {
                    skipped += name
                    continue
                }

                val taken = if (takenIndex >= 0) cursor.getLong(takenIndex) else 0L
                val modified = if (modifiedIndex >= 0) cursor.getLong(modifiedIndex) * 1000 else 0L
                val current = (if (taken > 0) taken else modified).takeIf { it > 0 }

                candidates += DateFixCandidate(
                    id = ContentUris.withAppendedId(collection, id).toString(),
                    name = name,
                    type = type,
                    targetEpochMs = target,
                    currentEpochMs = current,
                    alreadyMatches = current != null &&
                        matchesFilenameDate(current, target, parsed.hasTime),
                )
            }
        }
    }
}
