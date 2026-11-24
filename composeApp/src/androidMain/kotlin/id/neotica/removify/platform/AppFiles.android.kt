package id.neotica.removify.platform

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun writeAppFile(name: String, content: String): Result<Unit> =
    withContext(Dispatchers.IO) {
        runCatching {
            val context = AndroidAppContext.appContext ?: error("Android context is not available")
            writeAtomic(File(context.filesDir, name), content)
        }
    }

actual suspend fun readAppFile(name: String): String? = withContext(Dispatchers.IO) {
    runCatching {
        val context = AndroidAppContext.appContext ?: return@runCatching null
        val file = File(context.filesDir, name)
        if (file.isFile) file.readText() else null
    }.getOrNull()
}

actual suspend fun deleteAppFile(name: String): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val context = AndroidAppContext.appContext ?: error("Android context is not available")
        File(context.filesDir, name).delete()
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
