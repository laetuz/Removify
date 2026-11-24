package id.neotica.removify.utils

data class FilenameDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val hasTime: Boolean,
)

object FilenameDateParser {

    private val dateWithTimePatterns = listOf(
        Regex("""^PXL_(\d{8})_(\d{6})"""),
        Regex("""^(?:IMG|VID)_(\d{8})_(\d{6})"""),
        Regex("""^Screenshot[_-](\d{8})[-_](\d{6})"""),
    )

    private val dateOnlyPatterns = listOf(
        Regex("""^(?:IMG|VID|null)-(\d{8})-WA\d+"""),
        Regex("""^Screenshot[_-](\d{8})"""),
    )

    fun parse(name: String): FilenameDate? {
        for (pattern in dateWithTimePatterns) {
            val match = pattern.find(name) ?: continue
            return parseDateTime(match.groupValues[1], match.groupValues[2])
        }
        for (pattern in dateOnlyPatterns) {
            val match = pattern.find(name) ?: continue
            return parseDateOnly(match.groupValues[1])
        }
        return null
    }

    private fun parseDateTime(datePart: String, timePart: String): FilenameDate? {
        val (year, month, day) = parseDateParts(datePart) ?: return null

        val hour = timePart.substring(0, 2).toIntOrNull() ?: return null
        val minute = timePart.substring(2, 4).toIntOrNull() ?: return null
        val second = timePart.substring(4, 6).toIntOrNull() ?: return null

        if (hour !in 0..23 || minute !in 0..59 || second !in 0..59) return null

        return FilenameDate(year, month, day, hour, minute, hasTime = true)
    }

    private fun parseDateOnly(datePart: String): FilenameDate? {
        val (year, month, day) = parseDateParts(datePart) ?: return null
        return FilenameDate(year, month, day, 12, 0, hasTime = false)
    }

    private fun parseDateParts(datePart: String): Triple<Int, Int, Int>? {
        if (datePart.length != 8) return null

        val year = datePart.substring(0, 4).toIntOrNull() ?: return null
        val month = datePart.substring(4, 6).toIntOrNull() ?: return null
        val day = datePart.substring(6, 8).toIntOrNull() ?: return null

        if (year !in 1970..2100) return null
        if (month !in 1..12) return null
        if (day !in 1..daysInMonth(year, month)) return null

        return Triple(year, month, day)
    }

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 0
    }

    private fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
}
