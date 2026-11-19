package id.neotica.removify.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import id.neotica.removify.domain.model.MediaSource
import java.io.File
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val sourcePrefs: Preferences by lazy {
    Preferences.userRoot().node("id.neotica.removify")
}

actual suspend fun pickFolder(): String? = withContext(Dispatchers.Main) {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Choose a media folder"
    }
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile?.absolutePath
    } else {
        null
    }
}

@Composable
actual fun rememberSourceFolderPicker(onPicked: (String) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)

    return remember(scope) {
        {
            scope.launch {
                pickFolder()?.let(currentOnPicked)
            }
        }
    }
}

actual fun displayNameForPickedFolder(uri: String): String? =
    File(uri).name.takeIf { it.isNotBlank() }

actual fun mediaSourceForPickedLocation(value: String): MediaSource = MediaSource.Folder(value)

actual fun savedSourceFolderUri(): String? = sourcePrefs.get("source_folder_uri", null)

actual fun savedSourceFolderName(): String? = sourcePrefs.get("source_folder_name", null)

actual fun saveSourceFolder(uri: String, name: String?) {
    sourcePrefs.put("source_folder_uri", uri)
    if (name == null) {
        sourcePrefs.remove("source_folder_name")
    } else {
        sourcePrefs.put("source_folder_name", name)
    }
}

actual fun clearSourceFolder() {
    sourcePrefs.remove("source_folder_uri")
    sourcePrefs.remove("source_folder_name")
}
