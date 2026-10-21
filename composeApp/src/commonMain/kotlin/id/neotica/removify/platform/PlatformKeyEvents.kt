package id.neotica.removify.platform

data class PlatformKeyEvent(
    val isLeft: Boolean = false,
    val isRight: Boolean = false,
    val isUp: Boolean = false,
    val isDown: Boolean = false,
    val isZ: Boolean = false,
    val isCtrlDown: Boolean = false,
    val isMetaDown: Boolean = false,
)

expect fun installPlatformKeyDispatcher(onKey: (PlatformKeyEvent) -> Boolean): () -> Unit
