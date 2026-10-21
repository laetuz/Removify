package id.neotica.removify.platform

expect fun hasMediaPermission(): Boolean

expect suspend fun requestMediaPermission(): Boolean
