package id.neotica.removify.ui.feature.source

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.neotica.removify.ui.components.ButtonBasic
import id.neotica.removify.ui.components.DarkPrimary
import id.neotica.removify.ui.components.NegativePrimary
import id.neotica.removify.ui.components.NeoCard
import id.neotica.removify.ui.components.TransparentText40
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SourceView(
    onContinue: () -> Unit,
    viewModel: SourceViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

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
                    text = "Choose your media",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Pick a folder on desktop to start a swipe session. Android gallery support arrives in M1.",
                    color = TransparentText40,
                    textAlign = TextAlign.Center,
                )

                state.folderPath?.let { path ->
                    Text(
                        text = path,
                        color = TransparentText40,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = when {
                            state.isLoading -> "Scanning..."
                            state.mediaCount == 0 -> "No media found in this folder"
                            else -> "${state.mediaCount} items ready"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = NegativePrimary,
                        textAlign = TextAlign.Center,
                    )
                }

                if (state.isLoading) {
                    CircularProgressIndicator(color = DarkPrimary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ButtonBasic(text = "Choose folder", onClick = viewModel::chooseFolder)
                    if (state.mediaCount > 0) {
                        ButtonBasic(text = "Continue", onClick = onContinue)
                    }
                }
            }
        }
    }
}
