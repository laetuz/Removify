package id.neotica.removify.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.neotica.removify.domain.model.MediaItem

@Composable
expect fun VideoPreview(item: MediaItem, modifier: Modifier)
