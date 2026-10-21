package id.neotica.removify.ui.feature.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.neotica.removify.session.SessionStore
import id.neotica.removify.ui.components.ButtonBasic
import id.neotica.removify.ui.components.NeoCard
import id.neotica.removify.ui.components.TransparentText40
import id.neotica.removify.utils.formatBytes
import org.koin.compose.koinInject

@Composable
fun SessionSummaryView(
    onNewSession: () -> Unit,
    onOpenTrash: () -> Unit,
    sessionStore: SessionStore = koinInject(),
) {
    val draft by sessionStore.draft.collectAsState()
    val summary = draft.summary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Session summary",
                    style = MaterialTheme.typography.headlineSmall,
                )

                if (summary == null) {
                    Text(
                        text = "No session data available.",
                        color = TransparentText40,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        text = "Kept: ${summary.keptCount}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Moved to Trash: ${summary.trashCount} (${formatBytes(summary.trashBytes)})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Skipped: ${summary.skippedCount}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ButtonBasic(text = "Open trash", onClick = onOpenTrash)
                    ButtonBasic(
                        text = "New session",
                        onClick = {
                            sessionStore.clear()
                            onNewSession()
                        },
                    )
                }
            }
        }
    }
}
