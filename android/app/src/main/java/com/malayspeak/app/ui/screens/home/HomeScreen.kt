package com.malayspeak.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.malayspeak.app.R
import com.malayspeak.app.data.local.entity.VocabularyCard
import com.malayspeak.app.ui.catalog.categoryLabelRes
import com.malayspeak.app.ui.components.AudioPlayButton
import com.malayspeak.app.ui.components.BrandLoading
import com.malayspeak.app.ui.components.CountPill
import com.malayspeak.app.ui.components.DAILY_GOAL_LABEL
import com.malayspeak.app.ui.components.DAILY_GOAL_TARGET
import com.malayspeak.app.ui.components.Eyebrow
import com.malayspeak.app.ui.components.GlassCard
import com.malayspeak.app.ui.components.LearnedPillButton
import com.malayspeak.app.ui.components.PillAction
import com.malayspeak.app.ui.components.ProgressRing
import com.malayspeak.app.ui.components.SectionHeader
import com.malayspeak.app.ui.components.countLearnedToday
import com.malayspeak.app.ui.components.ringPercentDescription
import com.malayspeak.app.ui.screens.progress.ProgressViewModel
import com.malayspeak.app.ui.theme.MalaySpeakTheme
import kotlin.math.roundToInt

/**
 * Glassmorphism learning dashboard with circular progress ring graphs.
 */
@Composable
fun HomeScreen(
    viewModel: ProgressViewModel,
    onOpenWords: (String?) -> Unit,
    onToggleLearned: (VocabularyCard) -> Unit,
    onPlayAudio: (VocabularyCard) -> Unit = {},
    speakingCardId: () -> Long? = { null }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MalaySpeakTheme.spacing
    val rings = MalaySpeakTheme.rings
    val isLargeFont = LocalDensity.current.fontScale >= 1.5f

    val focusCategories = remember(uiState.categoriesProgress) {
        uiState.categoriesProgress
            .filter { it.totalCount > 0 && it.learnedCount < it.totalCount }
            .sortedBy { it.percentage }
            .take(3)
    }
    val startedCategories = remember(uiState.categoriesProgress) {
        uiState.categoriesProgress.count { it.learnedCount > 0 }
    }
    val todayLearned = remember(uiState.recentlyLearned) {
        countLearnedToday(uiState.recentlyLearned)
    }
    val onOpenAllWords = remember(onOpenWords) { { onOpenWords(null) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.screenHorizontal,
            end = spacing.screenHorizontal,
            top = spacing.screenTop,
            bottom = spacing.screenBottom
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap)
    ) {
        item(key = "top_bar", contentType = "top_bar") {
            HomeTopBar(totalCount = uiState.totalCount)
        }

        item(key = "hero_progress", contentType = "hero") {
            HeroProgressCard(
                percentage = uiState.progressPercentage,
                learnedCount = uiState.learnedCount,
                totalCount = uiState.totalCount,
                onContinue = onOpenAllWords
            )
        }

        item(key = "stats_rings", contentType = "stats_rings") {
            val totalCats = uiState.categoriesProgress.size
            val learnedProgress = if (uiState.totalCount > 0) {
                uiState.learnedCount.toFloat() / uiState.totalCount
            } else 0f
            val dailyProgress = (todayLearned.toFloat() / DAILY_GOAL_TARGET).coerceIn(0f, 1f)
            val categoriesProgress = if (totalCats > 0) {
                startedCategories.toFloat() / totalCats
            } else 0f

            val learnedLabel = stringResource(R.string.learned)
            val categoriesLabel = stringResource(R.string.categories)
            val dailyValue = stringResource(R.string.ratio_format, todayLearned, DAILY_GOAL_TARGET)
            val categoriesValue = stringResource(R.string.ratio_format, startedCategories, totalCats)

            if (isLargeFont) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
                ) {
                    RingStatCard(
                        progress = learnedProgress,
                        ringColor = rings.learned,
                        centerLabel = uiState.learnedCount.toString(),
                        caption = learnedLabel,
                        description = ringPercentDescription(learnedLabel, uiState.progressPercentage),
                        modifier = Modifier.fillMaxWidth()
                    )
                    RingStatCard(
                        progress = dailyProgress,
                        ringColor = rings.goal,
                        centerLabel = dailyValue,
                        caption = DAILY_GOAL_LABEL,
                        description = ringPercentDescription(
                            DAILY_GOAL_LABEL,
                            (dailyProgress * 100f).roundToInt()
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    RingStatCard(
                        progress = categoriesProgress,
                        ringColor = rings.overall,
                        centerLabel = categoriesValue,
                        caption = categoriesLabel,
                        description = ringPercentDescription(
                            categoriesLabel,
                            (categoriesProgress * 100f).roundToInt()
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)
                ) {
                    RingStatCard(
                        progress = learnedProgress,
                        ringColor = rings.learned,
                        centerLabel = uiState.learnedCount.toString(),
                        caption = learnedLabel,
                        description = ringPercentDescription(learnedLabel, uiState.progressPercentage),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    RingStatCard(
                        progress = dailyProgress,
                        ringColor = rings.goal,
                        centerLabel = dailyValue,
                        caption = DAILY_GOAL_LABEL,
                        description = ringPercentDescription(
                            DAILY_GOAL_LABEL,
                            (dailyProgress * 100f).roundToInt()
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    RingStatCard(
                        progress = categoriesProgress,
                        ringColor = rings.overall,
                        centerLabel = categoriesValue,
                        caption = categoriesLabel,
                        description = ringPercentDescription(
                            categoriesLabel,
                            (categoriesProgress * 100f).roundToInt()
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }

        item(key = "spotlight_section", contentType = "spotlight") {
            Column {
                SectionHeader(
                    title = stringResource(R.string.continue_learning),
                    subtitle = stringResource(R.string.next_phrase_subtitle),
                    actionLabel = stringResource(R.string.see_all),
                    onAction = onOpenAllWords
                )
                Spacer(modifier = Modifier.height(spacing.sm))
                if (uiState.totalCount == 0) {
                    BrandLoading(label = stringResource(R.string.loading_phrases))
                } else {
                    val spotlightCard = uiState.spotlight
                    val spotlightId = spotlightCard?.id
                    val isSpeaking by remember(speakingCardId, spotlightId) {
                        derivedStateOf { spotlightId != null && speakingCardId() == spotlightId }
                    }
                    SpotlightCard(
                        card = spotlightCard,
                        totalCount = uiState.totalCount,
                        onToggleLearned = onToggleLearned,
                        onPlayAudio = onPlayAudio,
                        isSpeaking = isSpeaking
                    )
                }
            }
        }

        if (focusCategories.isNotEmpty()) {
            item(key = "focus_areas", contentType = "focus_areas") {
                Column {
                    SectionHeader(
                        title = stringResource(R.string.focus_areas),
                        subtitle = stringResource(R.string.focus_subtitle)
                    )
                    Spacer(modifier = Modifier.height(spacing.sm))
                    if (isLargeFont) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
                            focusCategories.forEachIndexed { index, category ->
                                key(category.category) {
                                    val onCategoryClick = remember(onOpenWords, category.category) {
                                        { onOpenWords(category.category) }
                                    }
                                    FocusCategoryRingCard(
                                        label = stringResource(category.category.categoryLabelRes()),
                                        learned = category.learnedCount,
                                        total = category.totalCount,
                                        percentage = category.percentage,
                                        ringColor = rings.categoryPalette[index % rings.categoryPalette.size],
                                        onClick = onCategoryClick,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)
                        ) {
                            focusCategories.forEachIndexed { index, category ->
                                key(category.category) {
                                    val onCategoryClick = remember(onOpenWords, category.category) {
                                        { onOpenWords(category.category) }
                                    }
                                    FocusCategoryRingCard(
                                        label = stringResource(category.category.categoryLabelRes()),
                                        learned = category.learnedCount,
                                        total = category.totalCount,
                                        percentage = category.percentage,
                                        ringColor = rings.categoryPalette[index % rings.categoryPalette.size],
                                        onClick = onCategoryClick,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(totalCount: Int) {
    val spacing = MalaySpeakTheme.spacing
    val sizes = MalaySpeakTheme.sizes
    val glass = MalaySpeakTheme.glass
    val isLargeFont = LocalDensity.current.fontScale >= 1.5f

    val badgeContent: @Composable () -> Unit = {
        GlassCard(shape = glass.compactShape) {
            Row(
                modifier = Modifier.padding(
                    horizontal = spacing.sm,
                    vertical = spacing.xs
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(sizes.iconSm)
                )
                Spacer(modifier = Modifier.width(spacing.xs))
                Text(
                    text = if (totalCount > 0) {
                        stringResource(R.string.phrase_count, totalCount)
                    } else {
                        stringResource(R.string.offline)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (isLargeFont) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Eyebrow(text = stringResource(R.string.home_eyebrow))
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() }
            )
            badgeContent()
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Eyebrow(text = stringResource(R.string.home_eyebrow))
                Spacer(modifier = Modifier.height(spacing.xxs))
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() }
                )
            }
            Spacer(modifier = Modifier.width(spacing.sm))
            badgeContent()
        }
    }
}

@Composable
private fun HeroProgressCard(
    percentage: Int,
    learnedCount: Int,
    totalCount: Int,
    onContinue: () -> Unit
) {
    val spacing = MalaySpeakTheme.spacing
    val sizes = MalaySpeakTheme.sizes
    val glass = MalaySpeakTheme.glass
    val rings = MalaySpeakTheme.rings
    val isLargeFont = LocalDensity.current.fontScale >= 1.5f
    val progressLabel = stringResource(R.string.your_progress)
    val percentText = stringResource(R.string.percent_format, percentage)

    GlassCard(
        shape = glass.heroShape,
        elevated = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLargeFont) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.heroPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                ProgressRing(
                    progress = percentage / 100f,
                    ringSize = sizes.ringHero,
                    strokeWidth = sizes.ringHeroStroke,
                    ringColor = rings.overall,
                    centerLabel = percentText,
                    caption = stringResource(R.string.complete),
                    captionInside = true,
                    centerLabelStyle = MaterialTheme.typography.headlineSmall,
                    contentDescription = ringPercentDescription(progressLabel, percentage)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Eyebrow(text = progressLabel)
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = stringResource(R.string.phrases_learned_count, learnedCount, totalCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = stringResource(R.string.keep_learning),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                PillAction(
                    text = stringResource(R.string.continue_action),
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.heroPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Eyebrow(text = progressLabel)
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = stringResource(R.string.keep_learning),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = stringResource(R.string.phrases_learned_count, learnedCount, totalCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(spacing.md))
                    PillAction(
                        text = stringResource(R.string.continue_action),
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = onContinue
                    )
                }
                Spacer(modifier = Modifier.width(spacing.md))
                ProgressRing(
                    progress = percentage / 100f,
                    ringSize = sizes.ringHero,
                    strokeWidth = sizes.ringHeroStroke,
                    ringColor = rings.overall,
                    centerLabel = percentText,
                    caption = stringResource(R.string.complete),
                    captionInside = true,
                    centerLabelStyle = MaterialTheme.typography.headlineSmall,
                    contentDescription = ringPercentDescription(progressLabel, percentage)
                )
            }
        }
    }
}

@Composable
private fun RingStatCard(
    progress: Float,
    ringColor: Color,
    centerLabel: String,
    caption: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val spacing = MalaySpeakTheme.spacing
    val sizes = MalaySpeakTheme.sizes
    val glass = MalaySpeakTheme.glass

    GlassCard(
        modifier = modifier,
        shape = glass.compactShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.xs, vertical = spacing.cardPaddingCompact),
            contentAlignment = Alignment.Center
        ) {
            ProgressRing(
                progress = progress,
                ringSize = sizes.ringCompact,
                strokeWidth = sizes.ringCompactStroke,
                ringColor = ringColor,
                centerLabel = centerLabel,
                caption = caption,
                centerLabelStyle = MalaySpeakTheme.textStyles.ringSmallValue,
                contentDescription = description
            )
        }
    }
}

@Composable
private fun SpotlightCard(
    card: VocabularyCard?,
    totalCount: Int,
    onToggleLearned: (VocabularyCard) -> Unit,
    onPlayAudio: (VocabularyCard) -> Unit,
    isSpeaking: Boolean
) {
    val spacing = MalaySpeakTheme.spacing
    val sizes = MalaySpeakTheme.sizes
    val glass = MalaySpeakTheme.glass

    if (card == null) {
        GlassCard(
            shape = glass.cardShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(spacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(sizes.iconXl)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Column {
                    Text(
                        text = stringResource(R.string.all_done),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = stringResource(R.string.all_phrases_learned, totalCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    val onPlayCardAudio = remember(onPlayAudio, card) { { onPlayAudio(card) } }
    val onToggleCardLearned = remember(onToggleLearned, card) { { onToggleLearned(card) } }

    GlassCard(
        shape = glass.heroShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(spacing.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    CountPill(text = stringResource(card.category.categoryLabelRes()))
                }
                if (card.isLearned) {
                    CountPill(
                        text = stringResource(R.string.learned),
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.malayPhrase,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                AudioPlayButton(
                    isSpeaking = isSpeaking,
                    onClick = onPlayCardAudio,
                    size = sizes.audioButton
                )
            }
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = card.myanmarMeaning,
                style = MalaySpeakTheme.textStyles.bodyEmphasis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = card.englishMeaning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (card.malayPronunciation.isNotBlank()) {
                Spacer(modifier = Modifier.height(spacing.md))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = spacing.sm,
                            vertical = spacing.itemGap
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(sizes.iconSm)
                        )
                        Spacer(modifier = Modifier.width(spacing.xs))
                        Text(
                            text = card.malayPronunciation,
                            style = MalaySpeakTheme.textStyles.pronunciation
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(spacing.md))
            LearnedPillButton(
                isLearned = card.isLearned,
                onClick = onToggleCardLearned,
                fillWidth = true
            )
        }
    }
}

@Composable
private fun FocusCategoryRingCard(
    label: String,
    learned: Int,
    total: Int,
    percentage: Int,
    ringColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MalaySpeakTheme.spacing
    val sizes = MalaySpeakTheme.sizes
    val glass = MalaySpeakTheme.glass

    GlassCard(
        onClick = onClick,
        role = Role.Button,
        shape = glass.compactShape,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.xs, vertical = spacing.cardPaddingCompact),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProgressRing(
                progress = percentage / 100f,
                ringSize = sizes.ringCompact,
                strokeWidth = sizes.ringCompactStroke,
                ringColor = ringColor,
                centerLabel = stringResource(R.string.percent_format, percentage),
                centerLabelStyle = MalaySpeakTheme.textStyles.ringSmallValue,
                contentDescription = ringPercentDescription(label, percentage)
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = stringResource(R.string.learned_count_short, learned, total),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
