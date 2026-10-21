package id.neotica.removify.platform

expect fun nowEpochMs(): Long

expect fun formatMediaDate(epochMs: Long): String
