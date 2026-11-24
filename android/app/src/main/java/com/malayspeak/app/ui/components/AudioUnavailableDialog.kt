package com.malayspeak.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import com.malayspeak.app.R
import com.malayspeak.app.data.audio.SpeechMessage
import com.malayspeak.app.ui.theme.MalaySpeakTheme

/**
 * Clear, non-silent fallback when the device has no usable offline Malay voice.
 *
 * Rendered with the shared frosted GlassCard container.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioUnavailableDialog(
    message: SpeechMessage,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit
) {
    val spacing = MalaySpeakTheme.spacing
    val sizes = MalaySpeakTheme.sizes
    val styles = MalaySpeakTheme.styles
    val glass = MalaySpeakTheme.glass

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            shape = glass.dialogShape,
            elevated = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.dialogPadding)
            ) {
                Box(
                    modifier = Modifier
                        .size(sizes.minTouchTarget)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(sizes.iconLg)
                    )
                }
                Spacer(modifier = Modifier.height(spacing.md))
                Text(
                    text = message.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(spacing.lg))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(spacing.xxs)
                ) {
                    if (message.showRetry) {
                        TextButton(
                            onClick = onRetry,
                            shape = styles.buttonShape,
                            modifier = Modifier.heightIn(min = sizes.minTouchTarget)
                        ) {
                            Text(
                                text = stringResource(R.string.retry),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    if (message.showSettings) {
                        TextButton(
                            onClick = onOpenSettings,
                            shape = styles.buttonShape,
                            modifier = Modifier.heightIn(min = sizes.minTouchTarget)
                        ) {
                            Text(
                                text = stringResource(R.string.settings),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    TextButton(
                        onClick = onDismiss,
                        shape = styles.buttonShape,
                        modifier = Modifier.heightIn(min = sizes.minTouchTarget)
                    ) {
                        Text(
                            text = stringResource(R.string.close),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
