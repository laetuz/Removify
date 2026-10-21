package id.neotica.removify.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import id.neotica.removify.domain.model.MediaItem

@Composable
actual fun VideoPreview(item: MediaItem, modifier: Modifier) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Video preview", color = Color.White)
    }
}
