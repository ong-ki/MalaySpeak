package com.malayspeak.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import com.malayspeak.app.ui.theme.MalaySpeakTheme

/**
 * Reusable frosted glass container used for all cards, dialogs, search bar,
 * and floating bottom navigation bar.
 *
 * Uses translucent fill + 1dp light-to-transparent gradient border + large
 * rounded corners + soft shadow without Modifier.blur on card content.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MalaySpeakTheme.glass.cardShape,
    elevated: Boolean = false,
    highlighted: Boolean = false,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    role: Role? = if (onClick != null) Role.Button else null,
    onClickLabel: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = MalaySpeakTheme.glass
    val sizes = MalaySpeakTheme.sizes
    val fillColor = if (elevated) glass.fillElevated else glass.fill
    val borderStart = if (highlighted) glass.borderHighlightStart else glass.borderStart
    val borderEnd = if (highlighted) glass.borderHighlightEnd else glass.borderEnd
    val borderBrush = remember(borderStart, borderEnd) {
        Brush.linearGradient(colors = listOf(borderStart, borderEnd))
    }
    val elevation = if (elevated) glass.elevatedShadowElevation else glass.cardShadowElevation

    val interactiveModifier = if (onClick != null) {
        Modifier
            .heightIn(min = sizes.minTouchTarget)
            .clickable(
                role = role,
                onClickLabel = onClickLabel,
                onClick = onClick
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = glass.shadowColor,
                spotColor = glass.shadowColor,
                clip = false
            )
            .clip(shape)
            .background(fillColor)
            .border(
                width = glass.borderWidth,
                brush = borderBrush,
                shape = shape
            )
            .then(interactiveModifier)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            content()
        }
    }
}

/**
 * Soft muted 3-color backdrop with diffused ambient color blobs drawn behind
 * the app scaffold so frosted GlassCard surfaces sit over gentle depth.
 */
@Composable
fun GlassBackdrop(
    modifier: Modifier = Modifier
) {
    val glass = MalaySpeakTheme.glass
    val drawBackdrop = remember(glass) {
        Modifier.drawWithCache {
            val verticalBrush = Brush.verticalGradient(
                colors = listOf(
                    glass.backdropTop,
                    glass.backdropMid,
                    glass.backdropBottom
                )
            )
            val primaryCenter = Offset(x = size.width * 0.88f, y = size.height * 0.12f)
            val primaryRadius = size.width * 0.78f
            val primaryBrush = Brush.radialGradient(
                colors = listOf(glass.blobPrimary, Color.Transparent),
                center = primaryCenter,
                radius = primaryRadius
            )
            val secondaryCenter = Offset(x = size.width * 0.10f, y = size.height * 0.46f)
            val secondaryRadius = size.width * 0.74f
            val secondaryBrush = Brush.radialGradient(
                colors = listOf(glass.blobSecondary, Color.Transparent),
                center = secondaryCenter,
                radius = secondaryRadius
            )
            val accentCenter = Offset(x = size.width * 0.82f, y = size.height * 0.86f)
            val accentRadius = size.width * 0.64f
            val accentBrush = Brush.radialGradient(
                colors = listOf(glass.blobAccent, Color.Transparent),
                center = accentCenter,
                radius = accentRadius
            )
            onDrawBehind {
                drawRect(brush = verticalBrush)
                drawCircle(brush = primaryBrush, radius = primaryRadius, center = primaryCenter)
                drawCircle(brush = secondaryBrush, radius = secondaryRadius, center = secondaryCenter)
                drawCircle(brush = accentBrush, radius = accentRadius, center = accentCenter)
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(drawBackdrop)
    )
}
