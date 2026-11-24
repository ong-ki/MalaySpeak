package com.malayspeak.app.ui.screens.vocabulary

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malayspeak.app.data.local.entity.VocabularyCard
import com.malayspeak.app.data.repository.OfflinePhraseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Immutable
data class VocabularyUiState(
    val searchQuery: String = "",
    val deck: List<VocabularyCard> = emptyList(),
    val currentIndex: Int = 0,
    val selectedCategory: String = "ALL",
    val isFlipped: Boolean = false,
    val isLoading: Boolean = true
)

class VocabularyViewModel(
    private val repository: OfflinePhraseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabularyUiState())
    val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        loadDeck("ALL")
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadDeck(_uiState.value.selectedCategory)
    }

    fun loadDeck(category: String) {
        // One collector at a time: cancel the previous observation before replacing it.
        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, selectedCategory = category) }

        observeJob = viewModelScope.launch {
            val query = _uiState.value.searchQuery.trim()
            val flow = when {
                query.isNotEmpty() -> repository.searchPhrases(query).map { cards ->
                    if (category == "ALL") cards else cards.filter { it.category == category }
                }
                category == "ALL" -> repository.getAllCards()
                else -> repository.getCardsByCategory(category)
            }

            // Reset the visible position for a new filter/search, but keep the
            // current card selected when Room emits an update (e.g. learned toggle).
            var shouldResetPosition = true
            flow.collect { cards ->
                val resetPosition = shouldResetPosition
                shouldResetPosition = false
                _uiState.update { state ->
                    val currentId = state.deck.getOrNull(state.currentIndex)?.id
                    val nextIndex = if (resetPosition || currentId == null) {
                        0
                    } else {
                        cards.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
                    }
                    state.copy(
                        deck = cards,
                        currentIndex = nextIndex,
                        isFlipped = if (resetPosition) false else state.isFlipped,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun nextCard() {
        _uiState.update { state ->
            if (state.deck.isEmpty()) state
            else {
                val nextIdx = (state.currentIndex + 1) % state.deck.size
                state.copy(currentIndex = nextIdx, isFlipped = false)
            }
        }
    }

    fun previousCard() {
        _uiState.update { state ->
            if (state.deck.isEmpty()) state
            else {
                val prevIdx = if (state.currentIndex > 0) state.currentIndex - 1 else state.deck.size - 1
                state.copy(currentIndex = prevIdx, isFlipped = false)
            }
        }
    }

    fun toggleFlip() {
        _uiState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    fun toggleCardLearned(cardId: Long, currentLearned: Boolean) {
        viewModelScope.launch {
            repository.updateCardLearned(cardId, !currentLearned)
        }
    }

    fun currentCard(): VocabularyCard? {
        val state = _uiState.value
        return state.deck.getOrNull(state.currentIndex)
    }
}
