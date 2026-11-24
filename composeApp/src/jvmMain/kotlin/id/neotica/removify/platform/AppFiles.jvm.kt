package id.neotica.removify.platform

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun writeAppFile(name: String, content: String): Result<Unit> =
    withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(appDataDir())
            writeAtomic(File(directory, name), content)
        }
    }

actual suspend fun readAppFile(name: String): String? = withContext(Dispatchers.IO) {
    runCatching {
        val file = File(appDataDir(), name)
        if (file.isFile) file.readText() else null
    }.getOrNull()
}

actual suspend fun deleteAppFile(name: String): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        File(appDataDir(), name).delete()
        Unit
    }
}

private fun writeAtomic(target: File, content: String) {
    target.parentFile?.mkdirs()
    val temp = File(target.parentFile, "${target.name}.tmp")
    temp.writeText(content)
    try {
        Files.move(
            temp.toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    } catch (_: Exception) {
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}
