package id.neotica.removify.platform

import id.neotica.removify.domain.model.MediaItem
import java.awt.Desktop
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun openVideo(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        if (!Desktop.isDesktopSupported()) return@runCatching false
        val file = File(item.id)
        if (!file.isFile) return@runCatching false
        Desktop.getDesktop().open(file)
        true
    }.getOrDefault(false)
}
