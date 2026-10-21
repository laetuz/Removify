package id.neotica.removify.platform

import id.neotica.removify.domain.model.MediaSource
import id.neotica.removify.domain.model.MediaType
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaScannerTest {

    private val root = Files.createTempDirectory("removify-scan").toFile()

    @AfterTest
    fun cleanup() {
        root.deleteRecursively()
    }

    private fun createFile(relativePath: String, modifiedAt: Long): File {
        val file = File(root, relativePath)
        file.parentFile?.mkdirs()
        file.writeText("x")
        file.setLastModified(modifiedAt)
        return file
    }

    @Test
    fun scansImagesAndVideosIncludingSubfolders() = runBlocking {
        createFile("b.jpg", 2000)
        createFile("a.mp4", 1000)
        createFile("notes.txt", 3000)
        createFile(".hidden.jpg", 4000)
        createFile("sub/c.png", 5000)

        val result = scanMedia(
            source = MediaSource.Folder(root.absolutePath),
            includeImages = true,
            includeVideos = true,
        )

        val items = result.getOrThrow()
        assertEquals(3, items.size)
        assertEquals(listOf("c.png", "b.jpg", "a.mp4"), items.map { it.name })
    }

    @Test
    fun filtersByMediaType() = runBlocking {
        createFile("b.jpg", 2000)
        createFile("a.mp4", 1000)

        val images = scanMedia(
            source = MediaSource.Folder(root.absolutePath),
            includeImages = true,
            includeVideos = false,
        ).getOrThrow()
        assertEquals(listOf("b.jpg"), images.map { it.name })
        assertTrue(images.all { it.type == MediaType.IMAGE })

        val videos = scanMedia(
            source = MediaSource.Folder(root.absolutePath),
            includeImages = false,
            includeVideos = true,
        ).getOrThrow()
        assertEquals(listOf("a.mp4"), videos.map { it.name })
        assertTrue(videos.all { it.type == MediaType.VIDEO })
    }

    @Test
    fun failsForMissingFolder() = runBlocking {
        val result = scanMedia(
            source = MediaSource.Folder(File(root, "missing").absolutePath),
            includeImages = true,
            includeVideos = true,
        )
        assertTrue(result.isFailure)
    }
}
