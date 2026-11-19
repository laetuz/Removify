package id.neotica.removify.utils

import id.neotica.removify.domain.model.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaClassificationTest {

    @Test
    fun classifiesByExtension() {
        assertEquals(MediaType.IMAGE, MediaClassification.classify("photo.jpg", null))
        assertEquals(MediaType.IMAGE, MediaClassification.classify("raw.DNG", null))
        assertEquals(MediaType.VIDEO, MediaClassification.classify("clip.mp4", null))
        assertEquals(MediaType.VIDEO, MediaClassification.classify("old.3gp", null))
        assertNull(MediaClassification.classify("notes.txt", null))
        assertNull(MediaClassification.classify("no-extension", null))
    }

    @Test
    fun prefersMimeTypeOverExtension() {
        assertEquals(MediaType.VIDEO, MediaClassification.classify("photo.jpg", "video/mp4"))
        assertEquals(MediaType.IMAGE, MediaClassification.classify("clip.mp4", "image/jpeg"))
    }
}
