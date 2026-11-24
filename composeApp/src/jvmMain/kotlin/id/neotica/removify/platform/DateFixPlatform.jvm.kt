package id.neotica.removify.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

actual val isGalleryDateFixTarget: Boolean = false

actual val isDateFixWriteApprovalRequired: Boolean = false

@Composable
actual fun rememberDateFixWriteLauncher(onResult: (Boolean) -> Unit): (List<String>) -> Unit {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember { { _: List<String> -> currentOnResult(true) } }
}
