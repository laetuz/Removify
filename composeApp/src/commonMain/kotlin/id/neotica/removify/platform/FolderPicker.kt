package id.neotica.removify.platform

import androidx.compose.runtime.Composable
import id.neotica.removify.domain.model.MediaSource

expect suspend fun pickFolder(): String?

@Composable
expect fun rememberSourceFolderPicker(onPicked: (String) -> Unit): () -> Unit

expect fun displayNameForPickedFolder(uri: String): String?

expect fun mediaSourceForPickedLocation(value: String): MediaSource

expect fun savedSourceFolderUri(): String?

expect fun savedSourceFolderName(): String?

expect fun saveSourceFolder(uri: String, name: String?)

expect fun clearSourceFolder()
