package id.neotica.removify.data.local

import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.MediaType
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopTrashStoreTest {

    private val base = Files.createTempDirectory("removify-trash-test").toFile()
    private val sourceDir = File(base, "source").apply { mkdirs() }
    private val store = DesktopTrashStore(File(base, "appdata"))

    @AfterTest
    fun cleanup() {
        base.deleteRecursively()
    }

    private fun sourceFile(name: String, content: String = "content"): File =
        File(sourceDir, name).apply {
            parentFile?.mkdirs()
            writeText(content)
        }

    private fun mediaItem(file: File, type: MediaType = MediaType.IMAGE) = MediaItem(
        id = file.absolutePath,
        name = file.name,
        type = type,
        sizeBytes = file.length(),
        dateTakenEpochMs = 0,
    )

    @Test
    fun movesToTrashWritesManifestAndRestores() {
        val file = sourceFile("a.jpg", "hello")

        val result = store.moveToTrash(listOf(mediaItem(file)), "s1").getOrThrow()

        assertEquals(1, result.movedCount)
        assertEquals(0, result.failedCount)
        assertFalse(file.exists())

        val entries = store.listEntries().getOrThrow()
        assertEquals(1, entries.size)
        assertEquals(file.absolutePath, entries.single().originalPath)
        assertTrue(File(entries.single().trashedPath).isFile)

        assertEquals(1, store.restore(entries).getOrThrow())
        assertTrue(file.isFile)
        assertEquals("hello", file.readText())
        assertEquals(0, store.listEntries().getOrThrow().size)
    }

    @Test
    fun restoreUsesConflictingNameWhenOriginalExists() {
        val file = sourceFile("b.jpg", "original")
        store.moveToTrash(listOf(mediaItem(file)), "s1").getOrThrow()

        file.writeText("new content")
        val entries = store.listEntries().getOrThrow()

        assertEquals(1, store.restore(entries).getOrThrow())

        val restored = File(sourceDir, "b (restored).jpg")
        assertTrue(restored.isFile)
        assertEquals("original", restored.readText())
        assertEquals("new content", file.readText())
    }

    @Test
    fun emptyDeletesFilesAndReportsBytes() {
        val first = sourceFile("a.jpg", "12345")
        val second = sourceFile("b.png", "123")
        store.moveToTrash(listOf(mediaItem(first), mediaItem(second)), "s1").getOrThrow()

        val freed = store.empty().getOrThrow()

        assertEquals(8L, freed)
        assertEquals(0, store.listEntries().getOrThrow().size)
        assertTrue(File(base, "appdata/trash").listFiles().isNullOrEmpty())
    }

    @Test
    fun missingSourceIsCountedAsFailure() {
        val missing = File(sourceDir, "missing.jpg")

        val result = store.moveToTrash(listOf(mediaItem(missing)), "s1").getOrThrow()

        assertEquals(0, result.movedCount)
        assertEquals(1, result.failedCount)
    }
}
