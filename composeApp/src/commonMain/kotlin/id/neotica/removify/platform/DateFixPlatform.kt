package id.neotica.removify.platform

import androidx.compose.runtime.Composable

expect val isGalleryDateFixTarget: Boolean

expect val isDateFixWriteApprovalRequired: Boolean

@Composable
expect fun rememberDateFixWriteLauncher(onResult: (Boolean) -> Unit): (List<String>) -> Unit
