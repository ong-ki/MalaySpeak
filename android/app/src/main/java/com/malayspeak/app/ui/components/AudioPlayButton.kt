package com.malayspeak.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.malayspeak.app.R
import com.malayspeak.app.ui.theme.MalaySpeakTheme

/**
 * Shared one-tap pronunciation control.
 *
 * The icon switches to Stop while this phrase is being spoken, so the same
 * control can also interrupt playback. Always keeps at least a 48dp touch target.
 */
@Composable
fun AudioPlayButton(
    isSpeaking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = MalaySpeakTheme.sizes.audioButton,
    shape: Shape = MalaySpeakTheme.styles.iconButtonShape,
    containerColor: Color = if (isSpeaking) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    },
    contentColor: Color = if (isSpeaking) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    },
    borderColor: Color? = null
) {
    val sizes = MalaySpeakTheme.sizes
    val effectiveSize = size.coerceAtLeast(sizes.minTouchTarget)
    val container by animateColorAsState(
        targetValue = containerColor,
        label = "audioContainer"
    )
    val content by animateColorAsState(
        targetValue = contentColor,
        label = "audioContent"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .sizeIn(
                minWidth = sizes.minTouchTarget,
                minHeight = sizes.minTouchTarget
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
            },
        shape = shape,
        color = container,
        contentColor = content,
        border = borderColor?.let { BorderStroke(sizes.borderWidth, it) }
    ) {
        Box(
            modifier = Modifier.size(effectiveSize),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSpeaking) {
                    Icons.Default.Stop
                } else {
                    Icons.AutoMirrored.Filled.VolumeUp
                },
                contentDescription = stringResource(
                    if (isSpeaking) R.string.stop_audio else R.string.play_malay_audio
                ),
                modifier = Modifier.size(sizes.iconMd)
            )
        }
    }
}
