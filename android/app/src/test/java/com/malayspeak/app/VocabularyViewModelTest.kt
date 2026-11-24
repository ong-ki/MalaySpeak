package com.malayspeak.app

import com.malayspeak.app.data.local.dao.LearningProgressDao
import com.malayspeak.app.data.local.dao.VocabularyCardDao
import com.malayspeak.app.data.local.entity.VocabularyCard
import com.malayspeak.app.data.repository.OfflinePhraseRepository
import com.malayspeak.app.ui.screens.vocabulary.VocabularyViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val cardDao = mockk<VocabularyCardDao>(relaxed = true)
    private val progressDao = mockk<LearningProgressDao>(relaxed = true)

    private lateinit var allCardsFlow: MutableStateFlow<List<VocabularyCard>>
    private lateinit var categoryFlow: MutableStateFlow<List<VocabularyCard>>
    private lateinit var searchFlow: MutableStateFlow<List<VocabularyCard>>
    private lateinit var viewModel: VocabularyViewModel

    private val greetings = card(id = 1, malay = "Apa khabar?", category = "GREETINGS")
    private val market = card(id = 2, malay = "Berapa harga ini?", category = "MARKET")
    private val work = card(id = 3, malay = "Kerja siap", category = "WORK")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        allCardsFlow = MutableStateFlow(listOf(greetings, market))
        categoryFlow = MutableStateFlow(listOf(work))
        searchFlow = MutableStateFlow(emptyList())

        every { cardDao.getAllCards() } returns allCardsFlow
        every { cardDao.getCardsByCategory(any()) } returns categoryFlow
        every { cardDao.searchPhrases(any()) } returns searchFlow

        viewModel = VocabularyViewModel(
            OfflinePhraseRepository(cardDao, progressDao, null, testDispatcher)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoadCollectsAllCards() = runTest(testDispatcher) {
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.deck.size)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun learnedUpdateFromRoomPropagatesWithoutReloadAndKeepsPosition() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.nextCard()
        assertEquals(market.id, viewModel.currentCard()?.id)

        // Simulate Room emitting an updated list after the learned toggle.
        allCardsFlow.value = listOf(
            greetings,
            market.copy(isLearned = true, learnedTimestamp = 1_234L)
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.deck.size)
        assertEquals(1, viewModel.uiState.value.currentIndex)
        assertEquals(market.id, viewModel.currentCard()?.id)
        assertTrue(viewModel.currentCard()?.isLearned == true)
    }

    @Test
    fun categoryChangeObservesContinuousRoomUpdates() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.loadDeck("WORK")
        advanceUntilIdle()
        assertEquals("WORK", viewModel.uiState.value.selectedCategory)
        assertEquals(1, viewModel.uiState.value.deck.size)

        categoryFlow.value = listOf(
            work,
            card(id = 4, malay = "Kerja siap juga", category = "WORK")
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.deck.size)
        assertEquals("WORK", viewModel.uiState.value.deck.first().category)
    }

    @Test
    fun searchObservesContinuousRoomUpdates() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("berapa")
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.deck.size)

        searchFlow.value = listOf(market)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.deck.size)
        assertEquals(market.id, viewModel.currentCard()?.id)
    }

    @Test
    fun learnedToggleDelegatesToRepository() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.toggleCardLearned(market.id, currentLearned = false)
        advanceUntilIdle()

        coVerify { cardDao.updateLearnedStatus(market.id, true, any()) }
    }

    private fun card(
        id: Long,
        malay: String,
        category: String
    ) = VocabularyCard(
        id = id,
        category = category,
        categoryMm = "စမ်းသပ်",
        malayPhrase = malay,
        malayPronunciation = "ဆမ်းသပ်",
        myanmarMeaning = "စမ်းသပ်အဓိပ္ပာယ်",
        englishMeaning = "Test meaning",
        culturalContext = "ViewModel test",
        isLearned = false,
        learnedTimestamp = null
    )
}
