package id.neotica.removify.platform

actual fun appDataDir(): String =
    AndroidAppContext.appContext?.filesDir?.absolutePath
        ?: System.getProperty("java.io.tmpdir")
        ?: "."
