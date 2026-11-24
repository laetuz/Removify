package id.neotica.removify.data.local

import id.neotica.removify.domain.model.MediaSource
import id.neotica.removify.platform.localEpochMs
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributeView
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmMediaDateFixRepositoryTest {

    private val root = Files.createTempDirectory("removify-datefix").toFile()
    private val repository = JvmMediaDateFixRepository()

    @AfterTest
    fun cleanup() {
        root.deleteRecursively()
    }

    private fun file(name: String, modifiedEpochMs: Long): File =
        File(root, name).apply {
            parentFile?.mkdirs()
            writeText("x")
            setLastModified(modifiedEpochMs)
        }

    private fun epoch(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        localEpochMs(year, month, day, hour, minute)!!

    @Test
    fun previewsMatchedAndSkippedFiles() = runBlocking {
        file("IMG-20251022-WA0055.jpg", epoch(2025, 1, 1, 0, 0))
        file("notes.txt", epoch(2025, 1, 1, 0, 0))
        file(".hidden.jpg", epoch(2025, 1, 1, 0, 0))

        val preview = repository.preview(MediaSource.Folder(root.absolutePath)).getOrThrow()

        assertEquals(1, preview.matchedCount)
        assertEquals(0, preview.alreadyCount)
        assertEquals(1, preview.toUpdateCount)
        assertEquals(1, preview.skippedCount)
        assertEquals(
            epoch(2025, 10, 22, 12, 0),
            preview.candidates.single().targetEpochMs,
        )
    }

    @Test
    fun detectsAlreadyMatchingDates() = runBlocking {
        file("IMG-20251022-WA0055.jpg", epoch(2025, 10, 22, 8, 30))
        file("PXL_20251119_123456789.jpg", epoch(2025, 11, 19, 12, 34))

        val preview = repository.preview(MediaSource.Folder(root.absolutePath)).getOrThrow()

        assertEquals(2, preview.matchedCount)
        assertEquals(2, preview.alreadyCount)
        assertEquals(0, preview.toUpdateCount)
    }

    @Test
    fun appliesModifiedTime() = runBlocking {
        val target = epoch(2025, 10, 22, 12, 0)
        val file = file("IMG-20251022-WA0055.jpg", epoch(2025, 1, 1, 0, 0))
        val candidate = repository.preview(MediaSource.Folder(root.absolutePath))
            .getOrThrow().candidates.single()

        val report = repository.apply(listOf(candidate), {}, { false }).getOrThrow()

        assertEquals(1, report.updatedCount)
        assertEquals(0, report.failedCount)
        assertEquals(target, file.lastModified())

        val isMac = System.getProperty("os.name").lowercase().contains("mac")
        if (isMac && File("/usr/bin/SetFile").canExecute()) {
            val attributes = Files.getFileAttributeView(
                file.toPath(),
                BasicFileAttributeView::class.java,
            )?.readAttributes()
            assertEquals(target, attributes?.creationTime()?.toMillis())
        }
    }

    @Test
    fun cancellationStopsTheBatch() = runBlocking {
        file("IMG-20251022-WA0055.jpg", epoch(2025, 1, 1, 0, 0))
        file("IMG-20251023-WA0055.jpg", epoch(2025, 1, 1, 0, 0))
        val candidates = repository.preview(MediaSource.Folder(root.absolutePath))
            .getOrThrow().candidates

        var checks = 0
        val report = repository.apply(
            candidates = candidates,
            onProgress = {},
            shouldCancel = {
                checks++
                checks > 1
            },
        ).getOrThrow()

        assertEquals(1, report.updatedCount)
        assertTrue(report.cancelled)
    }

    @Test
    fun reportsMissingFileAsFailure() = runBlocking {
        val file = file("IMG-20251022-WA0055.jpg", epoch(2025, 1, 1, 0, 0))
        val candidates = repository.preview(MediaSource.Folder(root.absolutePath))
            .getOrThrow().candidates
        file.delete()

        val report = repository.apply(candidates, {}, { false }).getOrThrow()

        assertEquals(0, report.updatedCount)
        assertEquals(1, report.failedCount)
    }
}
