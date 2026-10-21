package id.neotica.removify.platform

import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

actual fun installPlatformKeyDispatcher(onKey: (PlatformKeyEvent) -> Boolean): () -> Unit {
    val dispatcher = KeyEventDispatcher { event ->
        if (event.id != KeyEvent.KEY_PRESSED) return@KeyEventDispatcher false

        onKey(
            PlatformKeyEvent(
                isLeft = event.keyCode == KeyEvent.VK_LEFT,
                isRight = event.keyCode == KeyEvent.VK_RIGHT,
                isUp = event.keyCode == KeyEvent.VK_UP,
                isDown = event.keyCode == KeyEvent.VK_DOWN,
                isZ = event.keyCode == KeyEvent.VK_Z,
                isCtrlDown = event.isControlDown,
                isMetaDown = event.isMetaDown,
            ),
        )
    }

    val manager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
    manager.addKeyEventDispatcher(dispatcher)
    return { manager.removeKeyEventDispatcher(dispatcher) }
}
