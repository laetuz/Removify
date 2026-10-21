package id.neotica.removify

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import id.neotica.removify.di.initializeKoin

fun main() {
    initializeKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Removify",
            state = rememberWindowState(size = DpSize(1280.dp, 800.dp)),
        ) {
            setSingletonImageLoaderFactory { context ->
                ImageLoader.Builder(context)
                    .crossfade(true)
                    .build()
            }
            App()
        }
    }
}
