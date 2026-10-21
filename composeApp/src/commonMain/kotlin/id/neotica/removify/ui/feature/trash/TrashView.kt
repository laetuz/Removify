package id.neotica.removify.ui.feature.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.neotica.removify.platform.formatMediaDate
import id.neotica.removify.ui.components.ButtonBasic
import id.neotica.removify.ui.components.DarkPrimary
import id.neotica.removify.ui.components.NegativePrimary
import id.neotica.removify.ui.components.NeoCard
import id.neotica.removify.ui.components.PositivePrimary
import id.neotica.removify.ui.components.TransparentText40
import id.neotica.removify.utils.formatBytes
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TrashView(viewModel: TrashViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Trash",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Text(
                    text = "${state.entries.size} items  \u00B7  ${formatBytes(state.totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TransparentText40,
                )
            }
            if (state.entries.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ButtonBasic(text = "Restore all", onClick = viewModel::restoreAll)
                    ButtonBasic(text = "Empty trash", onClick = viewModel::requestEmpty)
                }
            }
        }

        state.lastFreedBytes?.let { freed ->
            Text(
                text = "Freed ${formatBytes(freed)}",
                color = PositivePrimary,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        state.errorMessage?.let { message ->
            Text(text = message, color = NegativePrimary)
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DarkPrimary)
            }
        } else if (state.entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Trash is empty",
                    color = TransparentText40,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    NeoCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.name,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${formatBytes(entry.sizeBytes)}  \u00B7  ${formatMediaDate(entry.trashedAtEpochMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TransparentText40,
                                )
                            }
                            TextButton(onClick = { viewModel.restore(entry) }) {
                                Text(text = "Restore", color = DarkPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showEmptyDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEmpty,
            title = { Text(text = "Empty Trash?") },
            text = {
                Text(
                    text = "${state.entries.size} items (${formatBytes(state.totalBytes)}) will be permanently deleted. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmEmpty) {
                    Text(text = "Delete permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissEmpty) {
                    Text(text = "Cancel")
                }
            },
        )
    }
}
