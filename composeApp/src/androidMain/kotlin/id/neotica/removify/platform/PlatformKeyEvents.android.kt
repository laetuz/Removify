package id.neotica.removify.platform

actual fun installPlatformKeyDispatcher(onKey: (PlatformKeyEvent) -> Boolean): () -> Unit = {}
