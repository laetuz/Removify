package id.neotica.removify.ui.feature.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import id.neotica.removify.domain.model.MediaItem
import id.neotica.removify.domain.model.MediaType
import id.neotica.removify.domain.model.SwipeDecision
import id.neotica.removify.platform.formatMediaDate
import id.neotica.removify.platform.openVideo
import id.neotica.removify.ui.components.DarkPrimaryCard
import id.neotica.removify.ui.components.NegativePrimary
import id.neotica.removify.ui.components.PositivePrimary
import id.neotica.removify.ui.components.SkipGray
import id.neotica.removify.ui.components.TransparentText40
import id.neotica.removify.utils.formatBytes
import id.neotica.removify.utils.formatDuration
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun SwipeCardStack(
    current: MediaItem,
    upcoming: List<MediaItem>,
    onDecision: (SwipeDecision) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var isAnimating by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val horizontalThreshold = widthPx * 0.3f
        val verticalThreshold = heightPx * 0.2f

        upcoming.asReversed().forEachIndexed { index, item ->
            val depth = index + 1
            MediaCard(
                item = item,
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = 1f - 0.05f * depth
                        scaleY = 1f - 0.05f * depth
                        translationY = 16f * depth
                        alpha = 0.6f
                    },
            )
        }

        MediaCard(
            item = current,
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = offset.value.x
                    translationY = offset.value.y
                    rotationZ = (offset.value.x / widthPx) * 12f
                }
                .pointerInput(current.id) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (!isAnimating) {
                                scope.launch {
                                    offset.snapTo(offset.value + Offset(dragAmount.x, dragAmount.y))
                                }
                            }
                        },
                        onDragEnd = {
                            if (isAnimating) return@detectDragGestures
                            val x = offset.value.x
                            val y = offset.value.y
                            val decision = when {
                                abs(y) > abs(x) && y < -verticalThreshold -> SwipeDecision.SKIP
                                x < -horizontalThreshold -> SwipeDecision.TRASH
                                x > horizontalThreshold -> SwipeDecision.KEEP
                                else -> null
                            }

                            if (decision == null) {
                                scope.launch { offset.animateTo(Offset.Zero, spring()) }
                            } else {
                                val target = when (decision) {
                                    SwipeDecision.KEEP -> Offset(widthPx * 1.2f, y)
                                    SwipeDecision.TRASH -> Offset(-widthPx * 1.2f, y)
                                    SwipeDecision.SKIP -> Offset(x, -heightPx * 1.2f)
                                }
                                isAnimating = true
                                scope.launch {
                                    offset.animateTo(target, tween(durationMillis = 220))
                                    offset.snapTo(Offset.Zero)
                                    isAnimating = false
                                    onDecision(decision)
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(Offset.Zero, spring()) }
                        },
                    )
                }
                .pointerInput(current.id, current.type) {
                    if (current.type == MediaType.VIDEO) {
                        detectTapGestures {
                            scope.launch { openVideo(current) }
                        }
                    }
                },
        ) {
            Stamp(
                text = "KEEP",
                color = PositivePrimary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp)
                    .rotate(-15f)
                    .graphicsLayer { alpha = (offset.value.x / horizontalThreshold).coerceIn(0f, 1f) },
            )
            Stamp(
                text = "TRASH",
                color = NegativePrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .rotate(15f)
                    .graphicsLayer { alpha = (-offset.value.x / horizontalThreshold).coerceIn(0f, 1f) },
            )
            Stamp(
                text = "SKIP",
                color = SkipGray,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .graphicsLayer { alpha = (-offset.value.y / verticalThreshold).coerceIn(0f, 1f) },
            )
        }
    }
}

@Composable
private fun Stamp(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
            .border(width = 3.dp, color = color, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun MediaCard(
    item: MediaItem,
    modifier: Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(DarkPrimaryCard),
    ) {
        if (item.type == MediaType.IMAGE) {
            AsyncImage(
                model = item.id,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B0E20)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\u25B6",
                    fontSize = 72.sp,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.85f),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = metaLine(item),
                style = MaterialTheme.typography.bodySmall,
                color = TransparentText40,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = if (item.type == MediaType.IMAGE) "IMAGE" else "VIDEO",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }

        overlay()
    }
}

private fun metaLine(item: MediaItem): String {
    val parts = mutableListOf(
        formatMediaDate(item.dateTakenEpochMs),
        formatBytes(item.sizeBytes),
    )
    item.durationMs?.let { parts += formatDuration(it) }
    return parts.joinToString("  \u00B7  ")
}
