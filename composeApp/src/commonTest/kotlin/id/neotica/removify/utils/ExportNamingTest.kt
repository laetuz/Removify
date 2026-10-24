package id.neotica.removify.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class ExportNamingTest {

    @Test
    fun stripsDcimPrefixFromRelativePath() {
        assertEquals(listOf("Camera"), ExportNaming.relativeFolders("DCIM/Camera"))
        assertEquals(listOf("Camera", "Edit"), ExportNaming.relativeFolders("DCIM/Camera/Edit/"))
        assertEquals(emptyList(), ExportNaming.relativeFolders("DCIM/"))
        assertEquals(emptyList(), ExportNaming.relativeFolders("DCIM"))
    }

    @Test
    fun ignoresTraversalSegments() {
        assertEquals(listOf("Camera"), ExportNaming.relativeFolders("DCIM/../Camera/."))
    }

    @Test
    fun sanitizesIllegalCharacters() {
        assertEquals("a_b_c.jpg", ExportNaming.sanitizeFileName("a:b*c.jpg"))
        assertEquals("file", ExportNaming.sanitizeFileName("   "))
    }

    @Test
    fun appendsIndexForDuplicateNames() {
        assertEquals("photo.jpg", ExportNaming.uniqueName("photo.jpg", emptySet()))
        assertEquals("photo (1).jpg", ExportNaming.uniqueName("photo.jpg", setOf("photo.jpg")))
        assertEquals(
            "photo (2).jpg",
            ExportNaming.uniqueName("photo.jpg", setOf("photo.jpg", "photo (1).jpg")),
        )
    }

    @Test
    fun handlesNamesWithoutExtension() {
        assertEquals("backup (1)", ExportNaming.uniqueName("backup", setOf("backup")))
    }
}
