package id.neotica.removify.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile

private const val PREFS_NAME = "removify_dcim_export"
private const val KEY_TREE_URI = "tree_uri"
private const val KEY_DISPLAY_NAME = "display_name"

private fun prefs(): SharedPreferences? =
    AndroidAppContext.appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

actual val isDcimExportSupported: Boolean = true

@Composable
actual fun rememberDcimFolderPicker(onPicked: (String) -> Unit): () -> Unit {
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

@Composable
actual fun rememberDcimDeleteLauncher(onResult: (Boolean) -> Unit): (List<String>) -> Unit {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        currentOnResult(result.resultCode == Activity.RESULT_OK)
    }

    return remember(launcher) {
        { uris ->
            if (uris.isNotEmpty()) {
                val request = runCatching {
                    MediaStore.createDeleteRequest(context.contentResolver, uris.map(Uri::parse))
                }.getOrNull()

                if (request == null) {
                    currentOnResult(false)
                } else {
                    runCatching {
                        launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                    }.onFailure { currentOnResult(false) }
                }
            }
        }
    }
}

@Composable
actual fun rememberMediaPermissionRequester(onResult: (Boolean) -> Unit): () -> Unit {
    val currentOnResult by rememberUpdatedState(onResult)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        currentOnResult(hasMediaPermission())
    }

    return remember(launcher) {
        {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                )
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            launcher.launch(permissions)
        }
    }
}

actual fun savedDcimDestinationUri(): String? = prefs()?.getString(KEY_TREE_URI, null)

actual fun savedDcimDestinationName(): String? = prefs()?.getString(KEY_DISPLAY_NAME, null)

actual fun saveDcimDestination(uri: String, name: String?) {
    prefs()?.edit()
        ?.putString(KEY_TREE_URI, uri)
        ?.putString(KEY_DISPLAY_NAME, name)
        ?.apply()
}

actual fun dcimDestinationDisplayName(treeUri: String): String? {
    val context = AndroidAppContext.appContext ?: return null
    return runCatching {
        DocumentFile.fromTreeUri(context, Uri.parse(treeUri))?.name
    }.getOrNull()
}
