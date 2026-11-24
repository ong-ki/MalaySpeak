package com.malayspeak.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.malayspeak.app.data.local.entity.VocabularyCard
import com.malayspeak.app.ui.theme.MalaySpeakTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

const val DAILY_GOAL_TARGET = 5
const val DAILY_GOAL_LABEL = "Daily goal"

/** Formats a clear screen-reader description such as "Greetings, 60 percent". */
fun ringPercentDescription(label: String, percentage: Int): String =
    "$label, $percentage percent"

/** Counts learned phrases marked today in the user's local timezone. */
fun countLearnedToday(cards: List<VocabularyCard>): Int {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    return cards.count { card ->
        val ts = card.learnedTimestamp ?: return@count false
        card.isLearned && Instant.ofEpochMilli(ts).atZone(zone).toLocalDate() == today
    }
}

@Immutable
data class RingLayer(
    val progress: Float,
    val color: Color,
    val label: String,
    val valueText: String
)

/**
 * Reusable circular progress ring graph:
 * Canvas arc with rounded stroke cap, low-alpha track ring, single 700ms animated
 * sweep, center label (percentage or number), optional caption below, and full
 * accessibility semantics.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringSize: Dp = MalaySpeakTheme.sizes.progressRing,
    ringColor: Color = MalaySpeakTheme.rings.overall,
    trackColor: Color = MalaySpeakTheme.rings.track,
    capColor: Color? = null,
    strokeWidth: Dp = MalaySpeakTheme.sizes.progressRingStroke,
    centerLabel: String? = null,
    caption: String? = null,
    captionInside: Boolean = false,
    contentDescription: String? = null,
    centerLabelStyle: TextStyle = MaterialTheme.typography.titleMedium,
    captionStyle: TextStyle = MaterialTheme.typography.labelSmall,
    content: (@Composable () -> Unit)? = null
) {
    val spacing = MalaySpeakTheme.spacing
    val clampedProgress = progress.coerceIn(0f, 1f)
    val isLargeFont = LocalDensity.current.fontScale >= 1.5f

    var hasEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hasEntered = true }

    val animated by animateFloatAsState(
        targetValue = if (hasEntered) clampedProgress else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "progressRingSweep"
    )

    val resolvedDescription = contentDescription ?: caption?.let {
        ringPercentDescription(it, (clampedProgress * 100f).roundToInt())
    }

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            progressBarRangeInfo = ProgressBarRangeInfo(clampedProgress, 0f..1f)
            if (resolvedDescription != null) {
                this.contentDescription = resolvedDescription
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = strokeWidth.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)

                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                if (animated > 0.001f) {
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    if (capColor != null) {
                        val angle = Math.toRadians((-90f + 360f * animated).toDouble())
                        val radius = (size.minDimension - stroke) / 2f
                        val capCenter = Offset(
                            x = center.x + radius * cos(angle).toFloat(),
                            y = center.y + radius * sin(angle).toFloat()
                        )
                        drawCircle(
                            color = capColor,
                            radius = stroke / 2.6f,
                            center = capCenter
                        )
                    }
                }
            }

            if (content != null) {
                content()
            } else if (!isLargeFont && centerLabel != null) {
                Column(
                    modifier = Modifier.padding(horizontal = strokeWidth + spacing.xxs),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = centerLabel,
                        style = centerLabelStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    if (captionInside && caption != null) {
                        Text(
                            text = caption,
                            style = captionStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (content == null && isLargeFont && centerLabel != null) {
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = centerLabel,
                style = centerLabelStyle,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        if (content == null && caption != null && (!captionInside || isLargeFont)) {
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = caption,
                style = captionStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Multi-ring concentric circular progress graph (2-3 nested rings) drawn in a
 * single Canvas pass with one synchronized 720ms sweep animation.
 */
@Composable
fun ConcentricProgressRings(
    layers: List<RingLayer>,
    centerLabel: String,
    caption: String,
    modifier: Modifier = Modifier,
    ringSize: Dp = MalaySpeakTheme.sizes.progressRing,
    strokeWidth: Dp = MalaySpeakTheme.sizes.progressRingStroke,
    ringGap: Dp = MalaySpeakTheme.sizes.concentricGap,
    trackColor: Color = MalaySpeakTheme.rings.track
) {
    val spacing = MalaySpeakTheme.spacing
    val isLargeFont = LocalDensity.current.fontScale >= 1.5f

    var hasEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hasEntered = true }

    val sweepFactor by animateFloatAsState(
        targetValue = if (hasEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        label = "concentricRingSweep"
    )

    val overallProgress = layers.firstOrNull()?.progress?.coerceIn(0f, 1f) ?: 0f
    val combinedDescription = remember(layers) {
        layers.joinToString(separator = "; ") { layer ->
            ringPercentDescription(layer.label, (layer.progress.coerceIn(0f, 1f) * 100f).roundToInt())
        }
    }

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            progressBarRangeInfo = ProgressBarRangeInfo(overallProgress, 0f..1f)
            contentDescription = combinedDescription
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = strokeWidth.toPx()
                val gap = ringGap.toPx()

                layers.forEachIndexed { index, layer ->
                    val offset = (stroke / 2f) + index * (stroke + gap)
                    val diameter = size.minDimension - (offset * 2f)
                    if (diameter > stroke) {
                        val arcTopLeft = Offset(offset, offset)
                        val arcSize = Size(diameter, diameter)
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        val sweep = 360f * layer.progress.coerceIn(0f, 1f) * sweepFactor
                        if (sweep > 0.3f) {
                            drawArc(
                                color = layer.color,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }

            if (!isLargeFont) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = centerLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (isLargeFont) {
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
