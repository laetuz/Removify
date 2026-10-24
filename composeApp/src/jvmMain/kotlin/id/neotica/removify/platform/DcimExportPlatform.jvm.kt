package id.neotica.removify.platform

import androidx.compose.runtime.Composable

actual val isDcimExportSupported: Boolean = false

@Composable
actual fun rememberDcimFolderPicker(onPicked: (String) -> Unit): () -> Unit = {}

@Composable
actual fun rememberDcimDeleteLauncher(onResult: (Boolean) -> Unit): (List<String>) -> Unit = { _ -> }

@Composable
actual fun rememberMediaPermissionRequester(onResult: (Boolean) -> Unit): () -> Unit = {}

actual fun savedDcimDestinationUri(): String? = null

actual fun savedDcimDestinationName(): String? = null

actual fun saveDcimDestination(uri: String, name: String?) {}

actual fun clearDcimDestination() {}

actual fun dcimDestinationDisplayName(treeUri: String): String? = null

actual fun dcimDestinationError(treeUri: String): String? = null
