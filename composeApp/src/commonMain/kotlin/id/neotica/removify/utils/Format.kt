package id.neotica.removify.utils

import kotlin.math.round

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"

    var value = bytes / 1024.0
    var unit = "KB"
    for (nextUnit in listOf("MB", "GB", "TB")) {
        if (value < 1024.0) break
        value /= 1024.0
        unit = nextUnit
    }

    val rounded = round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) "${rounded.toLong()} $unit" else "$rounded $unit"
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
