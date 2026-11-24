package com.malayspeak.app.ui.screens.translate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.malayspeak.app.R
import com.malayspeak.app.data.local.entity.VocabularyCard
import com.malayspeak.app.ui.components.BrandLoading
import com.malayspeak.app.ui.components.BrandSearchField
import com.malayspeak.app.ui.components.CategoryFilterRow
import com.malayspeak.app.ui.components.CountPill
import com.malayspeak.app.ui.components.EmptyState
import com.malayspeak.app.ui.components.Eyebrow
import com.malayspeak.app.ui.components.PhraseCard
import com.malayspeak.app.ui.theme.MalaySpeakTheme
import kotlinx.coroutines.launch

/**
 * Words: the complete offline phrase library.
 *
 * Header, search, category rail and the expandable bilingual phrase cards.
 * Search and filtering still flow through TranslationViewModel unchanged.
 */
@Composable
fun TranslationScreen(
    viewModel: TranslationViewModel,
    onToggleLearned: (Long, Boolean) -> Unit = { _, _ -> },
    onPlayAudio: (VocabularyCard) -> Unit = {},
    speakingCardId: () -> Long? = { null }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MalaySpeakTheme.spacing
    val isLargeFont = LocalDensity.current.fontScale >= 1.5f
    val expandedCardIdsState = rememberSaveable(
        stateSaver = listSaver(
            save = { it.toList() },
            restore = { it.toSet() }
        )
    ) { mutableStateOf(emptySet<Long>()) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val onQueryChange = remember(viewModel) { viewModel::onSearchQueryChanged }
    val onClearSearch = remember(viewModel) { { viewModel.onSearchQueryChanged("") } }
    val onCategorySelected = remember(viewModel, coroutineScope, listState, expandedCardIdsState) {
        { category: String ->
            viewModel.onCategorySelected(category)
            expandedCardIdsState.value = emptySet()
            coroutineScope.launch { listState.scrollToItem(0) }
            Unit
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.itemGap),
        contentPadding = PaddingValues(
            start = spacing.screenHorizontal,
            end = spacing.screenHorizontal,
            top = spacing.screenTop,
            bottom = spacing.screenBottom
        )
    ) {
        item(key = "header", contentType = "header") {
            Column {
                if (isLargeFont) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Eyebrow(text = stringResource(R.string.phrase_library_eyebrow))
                        Text(
                            text = stringResource(R.string.phrase_library_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = stringResource(R.string.phrase_library_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CountPill(text = stringResource(R.string.result_count, uiState.searchResults.size))
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Eyebrow(text = stringResource(R.string.phrase_library_eyebrow))
                            Spacer(modifier = Modifier.height(spacing.xxs))
                            Text(
                                text = stringResource(R.string.phrase_library_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.semantics { heading() }
                            )
                            Spacer(modifier = Modifier.height(spacing.xxs))
                            Text(
                                text = stringResource(R.string.phrase_library_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(spacing.sm))
                        CountPill(text = stringResource(R.string.result_count, uiState.searchResults.size))
                    }
                }
                Spacer(modifier = Modifier.height(spacing.xs))
            }
        }

        item(key = "search", contentType = "search") {
            Column {
                BrandSearchField(
                    query = uiState.searchQuery,
                    onQueryChange = onQueryChange,
                    onClear = onClearSearch
                )
                Spacer(modifier = Modifier.height(spacing.xxs))
            }
        }

        item(key = "categories", contentType = "categories") {
            Column {
                CategoryFilterRow(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = onCategorySelected
                )
                Spacer(modifier = Modifier.height(spacing.xs))
            }
        }

        when {
            uiState.isLoading -> {
                item(key = "loading", contentType = "loading") {
                    BrandLoading(label = stringResource(R.string.searching_phrases))
                }
            }

            uiState.searchResults.isEmpty() -> {
                item(key = "empty", contentType = "empty") {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = stringResource(R.string.no_phrases_found),
                        message = stringResource(R.string.no_phrases_message),
                        actionLabel = if (uiState.searchQuery.isNotBlank()) {
                            stringResource(R.string.clear_search)
                        } else null,
                        onAction = if (uiState.searchQuery.isNotBlank()) {
                            onClearSearch
                        } else null
                    )
                }
            }

            else -> {
                items(
                    items = uiState.searchResults,
                    key = { it.id },
                    contentType = { "phrase_card" }
                ) { card ->
                    val isExpanded by remember(card.id, expandedCardIdsState) {
                        derivedStateOf { expandedCardIdsState.value.contains(card.id) }
                    }
                    val isSpeaking by remember(speakingCardId, card.id) {
                        derivedStateOf { speakingCardId() == card.id }
                    }
                    val onToggleExpand = remember(card.id, expandedCardIdsState) {
                        {
                            val current = expandedCardIdsState.value
                            expandedCardIdsState.value = if (current.contains(card.id)) {
                                current - card.id
                            } else {
                                current + card.id
                            }
                        }
                    }
                    val onToggleCardLearned = remember(onToggleLearned, card.id, card.isLearned) {
                        { onToggleLearned(card.id, !card.isLearned) }
                    }
                    val onPlayCardAudio = remember(onPlayAudio, card) {
                        { onPlayAudio(card) }
                    }
                    PhraseCard(
                        card = card,
                        isExpanded = isExpanded,
                        onToggleExpand = onToggleExpand,
                        onToggleLearned = onToggleCardLearned,
                        onPlayAudio = onPlayCardAudio,
                        isSpeaking = isSpeaking
                    )
                }
            }
        }
    }
}
