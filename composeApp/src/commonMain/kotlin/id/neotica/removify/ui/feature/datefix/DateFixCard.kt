package id.neotica.removify.ui.feature.datefix

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.neotica.removify.domain.model.DateFixReport
import id.neotica.removify.platform.formatMediaDateTime
import id.neotica.removify.platform.rememberDateFixWriteLauncher
import id.neotica.removify.platform.rememberMediaPermissionRequester
import id.neotica.removify.platform.rememberSourceFolderPicker
import id.neotica.removify.ui.components.ButtonBasic
import id.neotica.removify.ui.components.DarkPrimary
import id.neotica.removify.ui.components.DarkPrimaryCard
import id.neotica.removify.ui.components.NegativePrimary
import id.neotica.removify.ui.components.NeoCard
import id.neotica.removify.ui.components.PositivePrimary
import id.neotica.removify.ui.components.SkipGray
import id.neotica.removify.ui.components.TransparentText40
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DateFixCard(viewModel: DateFixViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val folderPicker = rememberSourceFolderPicker(viewModel::onFolderPicked)
    val permissionRequester = rememberMediaPermissionRequester(viewModel::onPermissionResult)
    val writeLauncher = rememberDateFixWriteLauncher(viewModel::onWriteApprovalResult)

    LaunchedEffect(state.awaitingWriteApproval, state.pendingWriteIds) {
        if (state.awaitingWriteApproval && state.pendingWriteIds.isNotEmpty()) {
            writeLauncher(state.pendingWriteIds)
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
                text = "Fix media dates",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Set dates from filenames such as IMG-20251022-WA0055.jpg.",
                color = TransparentText40,
                style = MaterialTheme.typography.bodySmall,
            )

            if (state.isGalleryTarget) {
                Text(
                    text = "Target: Gallery (all photos & videos)",
                    color = TransparentText40,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!state.hasPermission) {
                    Text(
                        text = "Allow access to photos and videos to continue.",
                        color = NegativePrimary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    ButtonBasic(text = "Allow access", onClick = permissionRequester)
                }
            } else {
                Text(
                    text = "Folder: ${state.folderName ?: "not selected"}",
                    color = TransparentText40,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ButtonBasic(text = "Choose folder", onClick = folderPicker)
            }

            val canPreview = (state.isGalleryTarget && state.hasPermission) ||
                (!state.isGalleryTarget && state.folderPath != null)

            if (state.isLoading) {
                CircularProgressIndicator(color = DarkPrimary)
            } else if (canPreview && !state.isApplying) {
                ButtonBasic(
                    text = if (state.preview == null) "Preview" else "Preview again",
                    onClick = viewModel::preview,
                )
            }

            state.preview?.let { preview ->
                Text(
                    text = "Will update: ${state.updateCount}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "Already dated: ${preview.alreadyCount}",
                    color = TransparentText40,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (preview.skippedCount > 0) {
                    Text(
                        text = "Skipped: ${preview.skippedCount} unrecognized names",
                        color = TransparentText40,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                preview.candidates.take(3).forEach { candidate ->
                    Text(
                        text = "${candidate.name} \u2192 ${formatMediaDateTime(candidate.targetEpochMs)}",
                        color = TransparentText40,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Switch(
                        checked = state.skipMatching,
                        onCheckedChange = { viewModel.toggleSkipMatching() },
                    )
                    Text(
                        text = "Skip items whose date already matches",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (!state.isApplying && !state.awaitingWriteApproval && state.updateCount > 0) {
                    ButtonBasic(text = "Apply", onClick = viewModel::requestApply)
                }
            }

            if (state.isApplying) {
                val progress = state.progress
                val fraction = if (progress == null || progress.total <= 0) {
                    0f
                } else {
                    (progress.processed.toFloat() / progress.total).coerceIn(0f, 1f)
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
                    text = progress?.currentName ?: "Applying...",
                    style = MaterialTheme.typography.bodySmall,
                    color = TransparentText40,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${progress?.processed ?: 0} / ${progress?.total ?: 0}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TransparentText40,
                )
                TextButton(onClick = viewModel::cancelApply) {
                    Text(text = "Cancel", color = NegativePrimary)
                }
            }

            if (state.awaitingWriteApproval) {
                Text(
                    text = "Waiting for write permission...",
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
                DateFixReportContent(report = report, onDismiss = viewModel::dismissReport)
            }
        }
    }

    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissConfirm,
            title = { Text(text = "Update media dates?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${state.updateCount} items will be updated to the dates in their filenames.",
                    )
                    if (state.isGalleryTarget) {
                        Text(text = "Android will ask you to allow these changes.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmApply) {
                    Text(text = "Update")
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
private fun DateFixReportContent(
    report: DateFixReport,
    onDismiss: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Updated ${report.updatedCount}",
            color = PositivePrimary,
            style = MaterialTheme.typography.bodySmall,
        )
        if (report.skippedCount > 0) {
            Text(
                text = "Skipped ${report.skippedCount} already matching",
                color = TransparentText40,
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
        if (report.cancelled) {
            Text(
                text = "Cancelled before completion",
                color = SkipGray,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(onClick = onDismiss) {
            Text(text = "Dismiss", color = DarkPrimary)
        }
    }
}
