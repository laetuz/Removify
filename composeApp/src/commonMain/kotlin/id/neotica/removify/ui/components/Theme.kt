package id.neotica.removify.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun RemovifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = DarkPrimary,
            onPrimary = Color.White,
            primaryContainer = DarkPrimaryCard,
            onPrimaryContainer = Color.White,
            secondary = DarkPrimaryCard,
            onSecondary = Color.White,
            background = DarkBackground,
            onBackground = Color.White,
            surface = DarkBackground,
            onSurface = Color.White,
            surfaceVariant = DarkPrimaryCard,
            onSurfaceVariant = TransparentText40,
            error = NegativePrimary,
            onError = Color.White,
        ),
        content = content,
    )
}
