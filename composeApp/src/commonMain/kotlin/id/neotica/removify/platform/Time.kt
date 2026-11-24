package id.neotica.removify.platform

expect fun nowEpochMs(): Long

expect fun formatMediaDate(epochMs: Long): String

expect fun formatMediaDateTime(epochMs: Long): String

expect fun localEpochMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long?

expect fun localEpochDay(epochMs: Long): Long

expect fun localMinuteOfDay(epochMs: Long): Int
