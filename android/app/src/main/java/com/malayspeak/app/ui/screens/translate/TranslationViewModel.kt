package com.malayspeak.app.ui.screens.translate

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malayspeak.app.data.local.entity.VocabularyCard
import com.malayspeak.app.data.repository.OfflinePhraseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Immutable
data class TranslationUiState(
    val searchQuery: String = "",
    val searchResults: List<VocabularyCard> = emptyList(),
    val selectedCategory: String = "ALL",
    val isLoading: Boolean = true
)

class TranslationViewModel(
    private val repository: OfflinePhraseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranslationUiState())
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureDatabaseSeeded()
            loadPhrases()
        }
    }

    private fun normalizeCategory(category: String): String = when (category) {
        "WORKPLACE" -> "WORK"
        "SHOPPING" -> "MARKET"
        "EMERGENCY" -> "HEALTH_EMERGENCY"
        "FOOD" -> "DINING"
        else -> category
    }

    private fun loadPhrases() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            val query = _uiState.value.searchQuery.trim()
            val category = normalizeCategory(_uiState.value.selectedCategory)

            val flow = when {
                query.isNotEmpty() -> repository.searchPhrases(query).map { list ->
                    if (category == "ALL") list else list.filter { it.category == category }
                }
                category == "ALL" -> repository.getAllCards()
                else -> repository.getCardsByCategory(category)
            }

            flow.collect { cards ->
                _uiState.update { it.copy(searchResults = cards, isLoading = false) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadPhrases()
    }

    fun onCategorySelected(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadPhrases()
    }

    fun toggleCardLearned(cardId: Long, isLearned: Boolean) {
        viewModelScope.launch {
            repository.updateCardLearned(cardId, isLearned)
        }
    }
}
