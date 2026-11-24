package id.neotica.removify.utils

import id.neotica.removify.platform.localEpochDay
import id.neotica.removify.platform.localMinuteOfDay

fun matchesFilenameDate(
    currentEpochMs: Long,
    targetEpochMs: Long,
    hasTime: Boolean,
): Boolean {
    if (localEpochDay(currentEpochMs) != localEpochDay(targetEpochMs)) return false
    if (!hasTime) return true
    return localMinuteOfDay(currentEpochMs) == localMinuteOfDay(targetEpochMs)
}
