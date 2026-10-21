package id.neotica.removify.ui.feature.setup

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
import org.koin.compose.koinInject

@Composable
fun SessionSetupView(
    onBack: () -> Unit,
    onStart: () -> Unit,
    sessionStore: SessionStore = koinInject(),
) {
    val draft by sessionStore.draft.collectAsState()

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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Session setup",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = when {
                        draft.items.isEmpty() -> "No media loaded yet"
                        else -> "${draft.items.size} items ready to review"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Image, video and date filters will live here.",
                    color = TransparentText40,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ButtonBasic(text = "Back", onClick = onBack)
                    ButtonBasic(text = "Start", onClick = onStart)
                }
            }
        }
    }
}
