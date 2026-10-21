package id.neotica.removify.platform

actual fun appDataDir(): String {
    val os = System.getProperty("os.name").lowercase()
    val home = System.getProperty("user.home")
    return when {
        os.contains("mac") -> "$home/Library/Application Support/Removify"
        os.contains("win") -> "${System.getenv("APPDATA") ?: home}\\Removify"
        else -> "$home/.local/share/Removify"
    }
}
