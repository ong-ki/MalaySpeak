package com.malayspeak.app.ui.screens.progress

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.malayspeak.app.R
import com.malayspeak.app.data.local.preferences.ThemeMode
import com.malayspeak.app.ui.catalog.categoryLabelRes
import com.malayspeak.app.ui.components.ConcentricProgressRings
import com.malayspeak.app.ui.components.DAILY_GOAL_LABEL
import com.malayspeak.app.ui.components.DAILY_GOAL_TARGET
import com.malayspeak.app.ui.components.Eyebrow
import com.malayspeak.app.ui.components.GlassCard
import com.malayspeak.app.ui.components.ProgressRing
import com.malayspeak.app.ui.components.RingLayer
import com.malayspeak.app.ui.components.SectionHeader
import com.malayspeak.app.ui.components.countLearnedToday
import com.malayspeak.app.ui.components.ringPercentDescription
import com.malayspeak.app.ui.theme.MalaySpeakTheme

/**
 * Progress and settings built with frosted GlassCards and circular progress rings.
 */
@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MalaySpeakTheme.spacing
    val sizes = MalaySpeakTheme.sizes
    val styles = MalaySpeakTheme.styles
    val glass = MalaySpeakTheme.glass
    val rings = MalaySpeakTheme.rings
    val isLargeFont = LocalDensity.current.fontScale >= 1.5f

    val showResetDialogState = rememberSaveable { mutableStateOf(false) }
    val onShowResetDialog = remember(showResetDialogState) { { showResetDialogState.value = true } }
    val onDismissResetDialog = remember(showResetDialogState) { { showResetDialogState.value = false } }
    val onConfirmReset = remember(viewModel, showResetDialogState) {
        {
            viewModel.resetProgress()
            showResetDialogState.value = false
        }
    }

    val todayLearned = remember(uiState.recentlyLearned) {
        countLearnedToday(uiState.recentlyLearned)
    }
    val startedCategories = remember(uiState.categoriesProgress) {
        uiState.categoriesProgress.count { it.learnedCount > 0 }
    }
    val categoryPairs = remember(uiState.categoriesProgress) {
        uiState.categoriesProgress.chunked(2)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.screenHorizontal,
            end = spacing.screenHorizontal,
            top = spacing.screenTop,
            bottom = spacing.screenBottom
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg)
    ) {
        item(key = "header", contentType = "header") {
            Column {
                Eyebrow(text = stringResource(R.string.insights_eyebrow))
                Spacer(modifier = Modifier.height(spacing.xxs))
                Text(
                    text = stringResource(R.string.progress_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(spacing.xxs))
                Text(
                    text = stringResource(R.string.learning_journey),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "overview", contentType = "overview") {
            OverviewCard(
                percentage = uiState.progressPercentage,
                learnedCount = uiState.learnedCount,
                totalCount = uiState.totalCount,
                todayLearned = todayLearned,
                startedCategories = startedCategories,
                totalCategories = uiState.categoriesProgress.size
            )
        }

        item(key = "by_category", contentType = "by_category") {
            Column {
                SectionHeader(
                    title = stringResource(R.string.by_category),
                    subtitle = stringResource(R.string.by_category_subtitle)
                )
                Spacer(modifier = Modifier.height(spacing.sm))
                if (isLargeFont) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
                        uiState.categoriesProgress.forEachIndexed { index, item ->
                            key(item.category) {
                                CategoryRingTile(
                                    item = item,
                                    ringColor = rings.categoryPalette[index % rings.categoryPalette.size],
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
                        categoryPairs.forEachIndexed { rowIndex, pair ->
                            key(pair.first().category) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)
                                ) {
                                    pair.forEachIndexed { colIndex, item ->
                                        key(item.category) {
                                            val paletteIndex = rowIndex * 2 + colIndex
                                            CategoryRingTile(
                                                item = item,
                                                ringColor = rings.categoryPalette[paletteIndex % rings.categoryPalette.size],
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                            )
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "appearance", contentType = "appearance") {
            Column {
                SectionHeader(
                    title = stringResource(R.string.appearance),
                    subtitle = stringResource(R.string.appearance_subtitle)
                )
                Spacer(modifier = Modifier.height(spacing.sm))
                GlassCard(
                    shape = glass.cardShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.cardPaddingCompact)) {
                        ThemeModeSelector(
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange
                        )
                        Spacer(modifier = Modifier.height(spacing.sm))
                        Text(
                            text = stringResource(
                                when (themeMode) {
                                    ThemeMode.SYSTEM -> R.string.theme_system_description
                                    ThemeMode.LIGHT -> R.string.theme_light_description
                                    ThemeMode.DARK -> R.string.theme_dark_description
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = spacing.xxs)
                        )
                    }
                }
            }
        }

        item(key = "data_section", contentType = "data_section") {
            Column {
                SectionHeader(
                    title = stringResource(R.string.data_section),
                    subtitle = stringResource(R.string.data_subtitle)
                )
                Spacer(modifier = Modifier.height(spacing.sm))
                GlassCard(
                    shape = glass.compactShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(spacing.xs)) {
                        OutlinedButton(
                            onClick = onShowResetDialog,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = sizes.buttonHeightLarge),
                            shape = styles.buttonShape,
                            border = MalaySpeakTheme.destructiveBorder(),
                            colors = MalaySpeakTheme.destructiveOutlinedButtonColors()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(sizes.iconSm)
                            )
                            Spacer(modifier = Modifier.width(spacing.xs))
                            Text(
                                text = stringResource(R.string.reset_progress),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        item(key = "footer", contentType = "footer") {
            Text(
                text = stringResource(R.string.footer_offline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xxs)
            )
        }
    }

    ResetDialogHost(
        showDialogState = showResetDialogState,
        onDismiss = onDismissResetDialog,
        onConfirm = onConfirmReset
    )
}

@Composable
private fun ResetDialogHost(
    showDialogState: State<Boolean>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialogState.value) {
        ResetDialog(
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

@Composable
private fun OverviewCard(
    percentage: Int,
    learnedCount: Int,
    totalCount: Int,
    todayLearned: Int,
    startedCategories: Int,
    totalCategories: Int
) {
    val spacing = MalaySpeakTheme.spacing
    val glass = MalaySpeakTheme.glass
    val rings = MalaySpeakTheme.rings
    val isLargeFont = LocalDensity.current.fontScale >= 1.5f

    val percentText = stringResource(R.string.percent_format, percentage)
    val learnedLabel = stringResource(R.string.learned)
    val categoriesLabel = stringResource(R.string.categories)
    val remainingCount = (totalCount - learnedCount).coerceAtLeast(0)

    val layers = remember(
        percentage,
        learnedCount,
        totalCount,
        todayLearned,
        startedCategories,
        totalCategories,
        rings,
        learnedLabel,
        categoriesLabel
    ) {
        val dailyRatio = (todayLearned.toFloat() / DAILY_GOAL_TARGET).coerceIn(0f, 1f)
        val categoriesRatio = if (totalCategories > 0) {
            startedCategories.toFloat() / totalCategories
        } else 0f
        listOf(
            RingLayer(
                progress = percentage / 100f,
                color = rings.overall,
                label = learnedLabel,
                valueText = "$learnedCount/$totalCount"
            ),
            RingLayer(
                progress = dailyRatio,
                color = rings.goal,
                label = DAILY_GOAL_LABEL,
                valueText = "$todayLearned/$DAILY_GOAL_TARGET"
            ),
            RingLayer(
                progress = categoriesRatio,
                color = rings.learned,
                label = categoriesLabel,
                valueText = "$startedCategories/$totalCategories"
            )
        )
    }

    GlassCard(
        shape = glass.heroShape,
        elevated = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ConcentricProgressRings(
                layers = layers,
                centerLabel = percentText,
                caption = stringResource(R.string.complete)
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            if (isLargeFont) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    layers.forEach { layer ->
                        key(layer.label) {
                            RingLegendRow(
                                color = layer.color,
                                label = layer.label,
                                value = layer.valueText
                            )
                        }
                    }
                    RingLegendRow(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        label = stringResource(R.string.remaining),
                        value = remainingCount.toString()
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalAlignment = Alignment.Top
                ) {
                    layers.forEach { layer ->
                        key(layer.label) {
                            RingLegendColumn(
                                color = layer.color,
                                label = layer.label,
                                value = layer.valueText,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RingLegendColumn(
    color: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val spacing = MalaySpeakTheme.spacing
    val sizes = MalaySpeakTheme.sizes

    Column(
        modifier = modifier.padding(horizontal = spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(sizes.badgeDot)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(spacing.xxs))
            Text(
                text = value,
                style = MalaySpeakTheme.textStyles.metricValue,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(spacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RingLegendRow(
    color: Color,
    label: String,
    value: String
) {
    val spacing = MalaySpeakTheme.spacing
    val sizes = MalaySpeakTheme.sizes

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(sizes.badgeDot)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(color)
        )
        Spacer(modifier = Modifier.width(spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MalaySpeakTheme.textStyles.metricValue,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CategoryRingTile(
    item: CategoryProgressItem,
    ringColor: Color,
    modifier: Modifier = Modifier
) {
    val spacing = MalaySpeakTheme.spacing
    val sizes = MalaySpeakTheme.sizes
    val glass = MalaySpeakTheme.glass
    val categoryLabel = stringResource(item.category.categoryLabelRes())

    GlassCard(
        shape = glass.compactShape,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.sm, vertical = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProgressRing(
                progress = item.percentage / 100f,
                ringSize = sizes.ringMedium,
                strokeWidth = sizes.ringMediumStroke,
                ringColor = ringColor,
                centerLabel = stringResource(R.string.percent_format, item.percentage),
                centerLabelStyle = MalaySpeakTheme.textStyles.ringSmallValue,
                contentDescription = ringPercentDescription(categoryLabel, item.percentage)
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = categoryLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = stringResource(
                    R.string.phrases_per_category,
                    item.learnedCount,
                    item.totalCount
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ThemeModeSelector(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val spacing = MalaySpeakTheme.spacing
    val glass = MalaySpeakTheme.glass
    val isLargeFont = LocalDensity.current.fontScale >= 1.5f
    val modes = ThemeMode.entries

    if (isLargeFont) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(glass.fillSubtle)
                .selectableGroup()
                .padding(spacing.xxs),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs)
        ) {
            modes.forEach { mode ->
                key(mode) {
                    ThemeModeOption(
                        mode = mode,
                        selected = mode == themeMode,
                        horizontal = true,
                        onSelect = remember(onThemeModeChange, mode) { { onThemeModeChange(mode) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(glass.fillSubtle)
                .selectableGroup()
                .padding(spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xxs)
        ) {
            modes.forEach { mode ->
                key(mode) {
                    ThemeModeOption(
                        mode = mode,
                        selected = mode == themeMode,
                        horizontal = false,
                        onSelect = remember(onThemeModeChange, mode) { { onThemeModeChange(mode) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeOption(
    mode: ThemeMode,
    selected: Boolean,
    horizontal: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MalaySpeakTheme.spacing
    val sizes = MalaySpeakTheme.sizes
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "themeSegmentContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "themeSegmentContent"
    )

    Surface(
        onClick = onSelect,
        modifier = modifier
            .heightIn(min = sizes.minTouchTarget)
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                this.selected = selected
            },
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content
    ) {
        if (horizontal) {
            Row(
                modifier = Modifier.padding(
                    horizontal = spacing.sm,
                    vertical = spacing.xs
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = themeModeIcon(mode),
                    contentDescription = null,
                    modifier = Modifier.size(sizes.iconMd)
                )
                Spacer(modifier = Modifier.width(spacing.sm))
                Text(
                    text = stringResource(themeModeLabelRes(mode)),
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(sizes.badgeDot)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(content)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(
                    horizontal = spacing.xxs,
                    vertical = spacing.xs
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = themeModeIcon(mode),
                    contentDescription = null,
                    modifier = Modifier.size(sizes.iconMd)
                )
                Spacer(modifier = Modifier.height(spacing.xxs))
                Text(
                    text = stringResource(themeModeLabelRes(mode)),
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(spacing.xxs))
                Box(
                    modifier = Modifier
                        .width(sizes.iconMd)
                        .height(sizes.borderWidth * 2)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(if (selected) content else Color.Transparent)
                )
            }
        }
    }
}

private fun themeModeIcon(mode: ThemeMode): ImageVector = when (mode) {
    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
    ThemeMode.LIGHT -> Icons.Default.LightMode
    ThemeMode.DARK -> Icons.Default.DarkMode
}

private fun themeModeLabelRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResetDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
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
                Text(
                    text = stringResource(R.string.reset_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = stringResource(R.string.reset_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(spacing.lg))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(spacing.xxs)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = styles.buttonShape,
                        modifier = Modifier.heightIn(min = sizes.minTouchTarget)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    TextButton(
                        onClick = onConfirm,
                        shape = styles.buttonShape,
                        modifier = Modifier.heightIn(min = sizes.minTouchTarget)
                    ) {
                        Text(
                            text = stringResource(R.string.reset_confirm),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
