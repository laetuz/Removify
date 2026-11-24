package id.neotica.removify.platform

import android.net.Uri
import android.provider.MediaStore
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

actual val isGalleryDateFixTarget: Boolean = true

actual val isDateFixWriteApprovalRequired: Boolean = true

@Composable
actual fun rememberDateFixWriteLauncher(onResult: (Boolean) -> Unit): (List<String>) -> Unit {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        currentOnResult(result.resultCode == Activity.RESULT_OK)
    }

    return remember(launcher) {
        { ids ->
            if (ids.isEmpty()) {
                currentOnResult(false)
            } else {
                val request = runCatching {
                    MediaStore.createWriteRequest(context.contentResolver, ids.map(Uri::parse))
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
