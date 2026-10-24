package id.neotica.removify.ui.feature.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.neotica.removify.domain.model.DcimExportReport
import id.neotica.removify.platform.isDcimExportSupported
import id.neotica.removify.platform.rememberDcimDeleteLauncher
import id.neotica.removify.platform.rememberDcimFolderPicker
import id.neotica.removify.platform.rememberMediaPermissionRequester
import id.neotica.removify.ui.components.ButtonBasic
import id.neotica.removify.ui.components.DarkPrimary
import id.neotica.removify.ui.components.DarkPrimaryCard
import id.neotica.removify.ui.components.NegativePrimary
import id.neotica.removify.ui.components.NeoCard
import id.neotica.removify.ui.components.PositivePrimary
import id.neotica.removify.ui.components.SkipGray
import id.neotica.removify.ui.components.TransparentText40
import id.neotica.removify.utils.formatBytes
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DcimExportCard() {
    if (!isDcimExportSupported) return
    DcimExportCardContent(viewModel = koinViewModel())
}

@Composable
private fun DcimExportCardContent(viewModel: DcimExportViewModel) {
    val state by viewModel.state.collectAsState()
    val folderPicker = rememberDcimFolderPicker(viewModel::onDestinationPicked)
    val deleteLauncher = rememberDcimDeleteLauncher(viewModel::onDeleteRequestResult)
    val permissionRequester = rememberMediaPermissionRequester(viewModel::onPermissionResult)

    LaunchedEffect(state.awaitingDeleteApproval, state.pendingDeleteUris) {
        if (state.awaitingDeleteApproval && state.pendingDeleteUris.isNotEmpty()) {
            deleteLauncher(state.pendingDeleteUris)
        }
    }

    NeoCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Export DCIM",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Move camera photos and videos to an external folder.",
                color = TransparentText40,
                style = MaterialTheme.typography.bodySmall,
            )

            when {
                !state.hasPermission -> {
                    Text(
                        text = "Allow access to photos and videos to continue.",
                        color = NegativePrimary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    ButtonBasic(text = "Allow access", onClick = permissionRequester)
                }

                state.isLoading -> {
                    CircularProgressIndicator(color = DarkPrimary)
                }

                else -> {
                    Text(
                        text = "${state.photoCount} photos  \u00B7  ${state.videoCount} videos  \u00B7  ${formatBytes(state.totalBytes)}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Destination: ${state.destinationName ?: "not selected"}",
                        color = TransparentText40,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ButtonBasic(text = "Choose folder", onClick = folderPicker)
                        if (state.items.isNotEmpty() &&
                            state.destinationUri != null &&
                            !state.isExporting &&
                            !state.awaitingDeleteApproval
                        ) {
                            ButtonBasic(text = "Move", onClick = viewModel::requestMove)
                        }
                    }

                    if (state.isExporting) {
                        val progress = state.progress
                        val fraction = if (progress == null || progress.totalBytes <= 0L) {
                            0f
                        } else {
                            (progress.copiedBytes.toFloat() / progress.totalBytes).coerceIn(0f, 1f)
                        }
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = DarkPrimary,
                            trackColor = DarkPrimaryCard,
                        )
                        Text(
                            text = progress?.currentName ?: "Preparing...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TransparentText40,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${progress?.processedCount ?: 0} / ${progress?.totalCount ?: state.items.size} files",
                            style = MaterialTheme.typography.bodySmall,
                            color = TransparentText40,
                        )
                        TextButton(onClick = viewModel::cancelExport) {
                            Text(text = "Cancel", color = NegativePrimary)
                        }
                    }

                    if (state.awaitingDeleteApproval) {
                        Text(
                            text = "Waiting for delete confirmation...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TransparentText40,
                        )
                    }

                    state.errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = NegativePrimary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    state.report?.let { report ->
                        ExportReportContent(
                            report = report,
                            onDismiss = viewModel::dismissReport,
                        )
                    }
                }
            }
        }
    }

    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissConfirm,
            title = { Text(text = "Move to external storage?") },
            text = {
                Text(
                    text = "${state.items.size} items (${formatBytes(state.totalBytes)}) will be copied to ${state.destinationName ?: "the selected folder"}. Originals are deleted only after each file is verified.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmMove) {
                    Text(text = "Move")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissConfirm) {
                    Text(text = "Cancel")
                }
            },
        )
    }
}

@Composable
private fun ExportReportContent(
    report: DcimExportReport,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Copied ${report.copiedCount} of ${report.totalCount} (${formatBytes(report.copiedBytes)})",
            color = PositivePrimary,
            style = MaterialTheme.typography.bodySmall,
        )
        if (report.deleteApproved) {
            Text(
                text = "Deleted ${report.deletedCount} originals",
                color = PositivePrimary,
                style = MaterialTheme.typography.bodySmall,
            )
        } else if (report.copiedCount > 0) {
            Text(
                text = "Originals kept",
                color = TransparentText40,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (report.cancelled) {
            Text(
                text = "Cancelled before completion",
                color = SkipGray,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (report.failedCount > 0) {
            Text(
                text = "Failed: ${report.failedCount}",
                color = NegativePrimary,
                style = MaterialTheme.typography.bodySmall,
            )
            report.failures.take(5).forEach { failure ->
                Text(
                    text = "\u2022 ${failure.name}: ${failure.reason}",
                    color = TransparentText40,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        TextButton(onClick = onDismiss) {
            Text(text = "Dismiss", color = DarkPrimary)
        }
    }
}
