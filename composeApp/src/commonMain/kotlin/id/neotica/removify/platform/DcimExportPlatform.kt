package id.neotica.removify.platform

import androidx.compose.runtime.Composable

expect val isDcimExportSupported: Boolean

@Composable
expect fun rememberDcimFolderPicker(onPicked: (String) -> Unit): () -> Unit

@Composable
expect fun rememberDcimDeleteLauncher(onResult: (Boolean) -> Unit): (List<String>) -> Unit

@Composable
expect fun rememberMediaPermissionRequester(onResult: (Boolean) -> Unit): () -> Unit

expect fun savedDcimDestinationUri(): String?

expect fun savedDcimDestinationName(): String?

expect fun saveDcimDestination(uri: String, name: String?)

expect fun dcimDestinationDisplayName(treeUri: String): String?
