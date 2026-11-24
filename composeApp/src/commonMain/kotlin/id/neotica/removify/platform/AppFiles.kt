package id.neotica.removify.platform

expect suspend fun writeAppFile(name: String, content: String): Result<Unit>

expect suspend fun readAppFile(name: String): String?

expect suspend fun deleteAppFile(name: String): Result<Unit>
