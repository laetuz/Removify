package id.neotica.removify.utils

import id.neotica.removify.domain.model.MediaType

object MediaClassification {

    private val imageExtensions = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "dng",
    )
    private val videoExtensions = setOf(
        "mp4", "mov", "m4v", "mkv", "webm", "avi", "3gp",
    )

    fun classify(name: String, mimeType: String?): MediaType? {
        val mime = mimeType?.lowercase()
        if (mime != null) {
            if (mime.startsWith("image/")) return MediaType.IMAGE
            if (mime.startsWith("video/")) return MediaType.VIDEO
        }

        val extension = name.substringAfterLast('.', "").lowercase()
        return when (extension) {
            in imageExtensions -> MediaType.IMAGE
            in videoExtensions -> MediaType.VIDEO
            else -> null
        }
    }
}
