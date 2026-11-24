package id.neotica.removify.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FilenameDateParserTest {

    @Test
    fun parsesWhatsAppDateOnlyNames() {
        assertEquals(
            FilenameDate(2025, 10, 22, 12, 0, hasTime = false),
            FilenameDateParser.parse("IMG-20251022-WA0055.jpeg"),
        )
        assertEquals(
            FilenameDate(2025, 1, 23, 12, 0, hasTime = false),
            FilenameDateParser.parse("VID-20250123-WA0034.mp4"),
        )
        assertEquals(
            FilenameDate(2025, 9, 3, 12, 0, hasTime = false),
            FilenameDateParser.parse("null-20250903-WA0025.jpg"),
        )
        assertEquals(
            FilenameDate(2025, 11, 19, 12, 0, hasTime = false),
            FilenameDateParser.parse("IMG-20251119-WA0016~2.jpeg"),
        )
    }

    @Test
    fun parsesTimestampedNames() {
        assertEquals(
            FilenameDate(2026, 10, 4, 16, 47, hasTime = true),
            FilenameDateParser.parse("PXL_20261004_164750573.mp4"),
        )
        assertEquals(
            FilenameDate(2025, 11, 19, 12, 34, hasTime = true),
            FilenameDateParser.parse("PXL_20251119_123456789.jpg"),
        )
        assertEquals(
            FilenameDate(2025, 1, 1, 8, 15, hasTime = true),
            FilenameDateParser.parse("IMG_20250101_081500.jpg"),
        )
        assertEquals(
            FilenameDate(2025, 1, 1, 8, 15, hasTime = true),
            FilenameDateParser.parse("VID_20250101_081500.mp4"),
        )
        assertEquals(
            FilenameDate(2025, 11, 19, 12, 34, hasTime = true),
            FilenameDateParser.parse("Screenshot_20251119-123456.png"),
        )
        assertEquals(
            FilenameDate(2025, 11, 19, 12, 0, hasTime = false),
            FilenameDateParser.parse("Screenshot_20251119.png"),
        )
    }

    @Test
    fun rejectsInvalidDatesAndNonMatchingNames() {
        assertNull(FilenameDateParser.parse("IMG-20251340-WA0001.jpg"))
        assertNull(FilenameDateParser.parse("IMG-20250230-WA0001.jpg"))
        assertNull(FilenameDateParser.parse("PXL_20251119_256099.jpg"))
        assertNull(FilenameDateParser.parse("notes.txt"))
        assertNull(FilenameDateParser.parse("IMG-20251022-WA.jpg"))
    }
}
