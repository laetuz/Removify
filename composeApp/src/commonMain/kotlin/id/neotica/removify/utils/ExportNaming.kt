package id.neotica.removify.utils

object ExportNaming {

    fun relativeFolders(relativePath: String): List<String> {
        val segments = relativePath.trim('/').split('/').filter { it.isNotBlank() }
        val subfolders = if (segments.firstOrNull()?.equals("DCIM", ignoreCase = true) == true) {
            segments.drop(1)
        } else {
            segments
        }
        return subfolders.filter { it != "." && it != ".." }
    }

    fun sanitizeFileName(name: String): String {
        val cleaned = name.trim().ifEmpty { "file" }
        return cleaned.replace(Regex("[/\\\\:*?\"<>|]"), "_")
    }

    fun uniqueName(original: String, existing: Set<String>): String {
        if (original !in existing) return original

        val dot = original.lastIndexOf('.')
        val base = if (dot > 0) original.substring(0, dot) else original
        val extension = if (dot > 0) original.substring(dot) else ""

        var index = 1
        while (true) {
            val candidate = "$base ($index)$extension"
            if (candidate !in existing) return candidate
            index++
        }
    }
}
