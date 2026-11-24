package id.neotica.removify.platform

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.getDefault())

actual fun nowEpochMs(): Long = System.currentTimeMillis()

actual fun formatMediaDate(epochMs: Long): String =
    dateFormatter.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epochMs))

actual fun formatMediaDateTime(epochMs: Long): String =
    dateTimeFormatter.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epochMs))

actual fun localEpochMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long? =
    runCatching {
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

actual fun localEpochDay(epochMs: Long): Long =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

actual fun localMinuteOfDay(epochMs: Long): Int {
    val time = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalTime()
    return time.hour * 60 + time.minute
}
