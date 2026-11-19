package id.neotica.removify.platform

import android.content.Intent
import android.net.Uri
import id.neotica.removify.domain.model.MediaItem
import java.io.File

actual suspend fun openVideo(item: MediaItem): Boolean {
    val context = AndroidAppContext.appContext ?: return false

    return runCatching {
        val uri = if (item.id.startsWith("content://") || item.id.startsWith("file://")) {
            Uri.parse(item.id)
        } else {
            Uri.fromFile(File(item.id))
        }

        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "video/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
