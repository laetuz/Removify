package id.neotica.removify.platform

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import id.neotica.removify.domain.model.MediaSource

private const val PREFS_NAME = "removify_source"
private const val KEY_TREE_URI = "tree_uri"
private const val KEY_DISPLAY_NAME = "display_name"

private fun prefs(): SharedPreferences? =
    AndroidAppContext.appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

actual suspend fun pickFolder(): String? = null

@Composable
actual fun rememberSourceFolderPicker(onPicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnPicked by rememberUpdatedState(onPicked)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            currentOnPicked(uri.toString())
        }
    }

    return remember(launcher) {
        { launcher.launch(null) }
    }
}

actual fun displayNameForPickedFolder(uri: String): String? {
    val context = AndroidAppContext.appContext ?: return null
    return runCatching {
        DocumentFile.fromTreeUri(context, Uri.parse(uri))?.name
    }.getOrNull()
}

actual fun mediaSourceForPickedLocation(value: String): MediaSource = MediaSource.Tree(value)

actual fun savedSourceFolderUri(): String? = prefs()?.getString(KEY_TREE_URI, null)

actual fun savedSourceFolderName(): String? = prefs()?.getString(KEY_DISPLAY_NAME, null)

actual fun saveSourceFolder(uri: String, name: String?) {
    prefs()?.edit()
        ?.putString(KEY_TREE_URI, uri)
        ?.putString(KEY_DISPLAY_NAME, name)
        ?.apply()
}

actual fun clearSourceFolder() {
    prefs()?.edit()
        ?.remove(KEY_TREE_URI)
        ?.remove(KEY_DISPLAY_NAME)
        ?.apply()
}
