package com.malayspeak.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import com.malayspeak.app.R
import com.malayspeak.app.data.local.entity.VocabularyCard
import com.malayspeak.app.ui.catalog.PhraseCategories
import com.malayspeak.app.ui.catalog.categoryLabelRes
import com.malayspeak.app.ui.theme.MalaySpeakTheme

/**
 * Horizontal category rail shared by the words and study screens.
 */
@Composable
fun CategoryFilterRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MalaySpeakTheme.spacing
    val sizes = MalaySpeakTheme.sizes
    val styles = MalaySpeakTheme.styles
    val glass = MalaySpeakTheme.glass

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        contentPadding = PaddingValues(vertical = spacing.xxs)
    ) {
        items(
            items = PhraseCategories,
            key = { it.key },
            contentType = { "category_chip" }
        ) { category ->
            val onChipClick = remember(onCategorySelected, category.key) {
                { onCategorySelected(category.key) }
            }
            CategoryFilterChip(
                labelRes = category.categoryLabelRes(),
                selected = category.key == selectedCategory,
                onClick = onChipClick
            )
        }
    }
}

@Composable
private fun CategoryFilterChip(
    labelRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val spacing = MalaySpeakTheme.spacing
    val sizes = MalaySpeakTheme.sizes
    val styles = MalaySpeakTheme.styles
    val glass = MalaySpeakTheme.glass

    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else glass.fill,
        animationSpec = tween(200),
        label = "chipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "chipContent"
    )
    Surface(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = sizes.minTouchTarget)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
            },
        shape = styles.chipShape,
        color = container,
        contentColor = content,
        border = BorderStroke(
            sizes.borderWidth,
            if (selected) MaterialTheme.colorScheme.primary
            else glass.borderStart
        )
    ) {
        Row(
            modifier = Modifier.padding(styles.chipPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(sizes.badgeDot)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.onPrimary)
                )
                Spacer(modifier = Modifier.width(spacing.xs))
            }
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Expandable bilingual phrase card.
 *
 * Collapsed: category + Malay phrase only.
 * Expanded: pronunciation, Burmese and English meaning, usage note and the
 * learned toggle. The whole card is the expand target.
 */
@Composable
fun PhraseCard(
    card: VocabularyCard,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleLearned: () -> Unit,
    onPlayAudio: () -> Unit = {},
    isSpeaking: Boolean = false,
    modifier: Modifier = Modifier
) {
    val spacing = MalaySpeakTheme.spacing
    val sizes = MalaySpeakTheme.sizes
    val styles = MalaySpeakTheme.styles
    val glass = MalaySpeakTheme.glass

    val chevronRotationState = animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(240),
        label = "chevron"
    )
    val toggleActionLabel = stringResource(if (isExpanded) R.string.collapse else R.string.expand)
    val cardStateDescription = stringResource(
        if (isExpanded) R.string.expanded_state else R.string.collapsed_state
    )
    val isLargeFont = LocalDensity.current.fontScale >= 1.5f

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .semantics { stateDescription = cardStateDescription },
        shape = glass.cardShape,
        elevated = isExpanded,
        highlighted = isExpanded,
        onClick = onToggleExpand,
        role = Role.Button,
        onClickLabel = toggleActionLabel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(sizes.accentStripeWidth)
                        .height(sizes.accentStripeHeight)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(
                            if (card.isLearned) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                )
                Spacer(modifier = Modifier.width(spacing.sm))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(card.category.categoryLabelRes()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = card.malayPhrase,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(spacing.xs))

                AudioPlayButton(
                    isSpeaking = isSpeaking,
                    onClick = onPlayAudio,
                    size = sizes.audioButton
                )
                Spacer(modifier = Modifier.width(spacing.xs))

                if (card.isLearned) {
                    Surface(
                        shape = styles.pillShape,
                        color = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.learned),
                            modifier = Modifier
                                .padding(spacing.xxs)
                                .size(sizes.iconXs)
                        )
                    }
                    Spacer(modifier = Modifier.width(spacing.xs))
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(sizes.iconMd)
                        .graphicsLayer { rotationZ = chevronRotationState.value }
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(180)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(140)) + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(spacing.md))

                    if (card.malayPronunciation.isNotBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = spacing.sm,
                                    vertical = spacing.sm
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(sizes.iconSm)
                                )
                                Spacer(modifier = Modifier.width(spacing.sm))
                                Column {
                                    Text(
                                        text = stringResource(R.string.pronunciation),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f)
                                    )
                                    Text(
                                        text = card.malayPronunciation,
                                        style = MalaySpeakTheme.textStyles.pronunciation
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    MeaningBlock(
                        label = stringResource(R.string.burmese_label),
                        text = card.myanmarMeaning,
                        emphasized = true
                    )

                    Spacer(modifier = Modifier.height(spacing.sm))

                    MeaningBlock(
                        label = stringResource(R.string.english_label),
                        text = card.englishMeaning,
                        emphasized = false
                    )

                    if (card.culturalContext.isNotBlank()) {
                        Spacer(modifier = Modifier.height(spacing.md))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = glass.fillSubtle,
                            border = MalaySpeakTheme.cardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(spacing.cardPaddingCompact)) {
                                Eyebrow(text = stringResource(R.string.usage_note))
                                Spacer(modifier = Modifier.height(spacing.xs))
                                Text(
                                    text = card.culturalContext,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.md))

                    if (isLargeFont) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            CountPill(text = stringResource(card.category.categoryLabelRes()))
                            LearnedPillButton(
                                isLearned = card.isLearned,
                                onClick = onToggleLearned,
                                fillWidth = true
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CountPill(
                                text = stringResource(card.category.categoryLabelRes()),
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Spacer(modifier = Modifier.width(spacing.sm))

                            LearnedPillButton(
                                isLearned = card.isLearned,
                                onClick = onToggleLearned
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeaningBlock(
    label: String,
    text: String,
    emphasized: Boolean
) {
    val spacing = MalaySpeakTheme.spacing
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(spacing.xxs))
        Text(
            text = text,
            style = if (emphasized) MalaySpeakTheme.textStyles.bodyEmphasis
            else MaterialTheme.typography.bodyMedium,
            color = if (emphasized) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
