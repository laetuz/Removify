package id.neotica.removify.platform

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

actual fun nowEpochMs(): Long = System.currentTimeMillis()

actual fun formatMediaDate(epochMs: Long): String =
    dateFormatter.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epochMs))
