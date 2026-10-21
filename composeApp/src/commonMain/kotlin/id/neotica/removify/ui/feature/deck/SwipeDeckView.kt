package id.neotica.removify.ui.feature.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.neotica.removify.domain.model.SwipeDecision
import id.neotica.removify.platform.installPlatformKeyDispatcher
import id.neotica.removify.ui.components.ButtonBasic
import id.neotica.removify.ui.components.DarkBackground
import id.neotica.removify.ui.components.DarkPrimary
import id.neotica.removify.ui.components.DarkPrimaryCard
import id.neotica.removify.ui.components.NegativePrimary
import id.neotica.removify.ui.components.PositivePrimary
import id.neotica.removify.ui.components.SkipGray
import id.neotica.removify.ui.components.TransparentText40
import id.neotica.removify.utils.formatBytes
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SwipeDeckView(
    onFinish: () -> Unit,
    viewModel: SwipeDeckViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) onFinish()
    }

    DisposableEffect(Unit) {
        val handle = installPlatformKeyDispatcher { event ->
            when {
                event.isLeft -> {
                    viewModel.decide(SwipeDecision.TRASH)
                    true
                }

                event.isRight -> {
                    viewModel.decide(SwipeDecision.KEEP)
                    true
                }

                event.isUp -> {
                    viewModel.decide(SwipeDecision.SKIP)
                    true
                }

                event.isDown -> {
                    viewModel.undo()
                    true
                }

                event.isZ && (event.isCtrlDown || event.isMetaDown) -> {
                    viewModel.undo()
                    true
                }

                else -> false
            }
        }
        onDispose { handle() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            DeckHeader(state = state, onFinish = viewModel::requestFinish)
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val current = state.currentItem
                if (current != null) {
                    SwipeCardStack(
                        current = current,
                        upcoming = state.nextItems,
                        onDecision = viewModel::decide,
                    )
                } else if (state.isDeckFinished) {
                    FinishPanel(state = state, onFinish = viewModel::requestFinish)
                } else {
                    Text(text = "No media to review", color = TransparentText40)
                }
            }

            if (state.currentItem != null) {
                Spacer(modifier = Modifier.height(12.dp))
                DeckActions(
                    onUndo = viewModel::undo,
                    onTrash = { viewModel.decide(SwipeDecision.TRASH) },
                    onSkip = { viewModel.decide(SwipeDecision.SKIP) },
                    onKeep = { viewModel.decide(SwipeDecision.KEEP) },
                )
            }
        }

        state.errorMessage?.let { message ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                color = NegativePrimary,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(12.dp),
                    color = Color.White,
                )
            }
        }

        if (state.isCommitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = DarkPrimary)
                    Text(text = "Moving to Trash...", color = Color.White)
                }
            }
        }
    }

    if (state.showFinishDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissFinishDialog,
            title = { Text(text = "Move to Trash?") },
            text = {
                Text(
                    text = "${state.trashedCount} items (${formatBytes(state.trashBytes)}) will be moved to the Removify Trash. You can restore them anytime.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmFinish) {
                    Text(text = "Move to Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissFinishDialog) {
                    Text(text = "Cancel")
                }
            },
        )
    }
}

@Composable
private fun DeckHeader(
    state: SwipeDeckUiState,
    onFinish: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${state.decidedCount} / ${state.totalCount}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "\u2713 ${state.keptCount}   \u2715 ${state.trashedCount}   \u21B7 ${state.skippedCount}",
                style = MaterialTheme.typography.bodySmall,
                color = TransparentText40,
            )
            TextButton(onClick = onFinish) {
                Text(text = "Finish", color = DarkPrimary)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val fraction = if (state.totalCount == 0) {
            0f
        } else {
            state.decidedCount.toFloat() / state.totalCount
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = DarkPrimary,
            trackColor = DarkPrimaryCard,
        )
    }
}

@Composable
private fun FinishPanel(
    state: SwipeDeckUiState,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "All items reviewed",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Text(
            text = if (state.trashedCount == 0) {
                "Nothing marked for Trash."
            } else {
                "${state.trashedCount} items (${formatBytes(state.trashBytes)}) marked for Trash."
            },
            color = TransparentText40,
            textAlign = TextAlign.Center,
        )
        ButtonBasic(
            text = if (state.trashedCount == 0) "Finish session" else "Move to Trash & finish",
            onClick = onFinish,
        )
    }
}

@Composable
private fun DeckActions(
    onUndo: () -> Unit,
    onTrash: () -> Unit,
    onSkip: () -> Unit,
    onKeep: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeckAction("\u21BA", "Undo", DarkPrimaryCard, onUndo)
        DeckAction("\u2715", "Trash", NegativePrimary, onTrash)
        DeckAction("\u21B7", "Skip", SkipGray, onSkip)
        DeckAction("\u2713", "Keep", PositivePrimary, onKeep)
    }
}

@Composable
private fun DeckAction(
    glyph: String,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = Color.White,
            ),
        ) {
            Text(text = glyph, style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TransparentText40,
        )
    }
}
