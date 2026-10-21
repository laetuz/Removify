package id.neotica.removify.platform

actual fun hasMediaPermission(): Boolean = true

actual suspend fun requestMediaPermission(): Boolean = true
