package com.malayspeak.app

import com.malayspeak.app.data.local.dao.LearningProgressDao
import com.malayspeak.app.data.local.dao.VocabularyCardDao
import com.malayspeak.app.data.local.entity.VocabularyCard
import com.malayspeak.app.data.repository.OfflinePhraseRepository
import com.malayspeak.app.ui.screens.translate.TranslationViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val cardDao = mockk<VocabularyCardDao>(relaxed = true)
    private val progressDao = mockk<LearningProgressDao>(relaxed = true)
    private lateinit var repository: OfflinePhraseRepository
    private lateinit var viewModel: TranslationViewModel

    private val sampleCards = listOf(
        VocabularyCard(
            id = 1,
            category = "GREETINGS",
            categoryMm = "နှုတ်ဆက်ခြင်း",
            malayPhrase = "Apa khabar?",
            malayPronunciation = "အာပါ ခါဘာရ်?",
            myanmarMeaning = "နေကောင်းလား။",
            englishMeaning = "How are you?",
            culturalContext = "Friendly greeting",
            isLearned = false
        ),
        VocabularyCard(
            id = 2,
            category = "WORK",
            categoryMm = "လုပ်ငန်းခွင်",
            malayPhrase = "Kerja siap",
            malayPronunciation = "ကာဂျာ ဆီယပ်",
            myanmarMeaning = "အလုပ်ပြီးပြီ",
            englishMeaning = "Work is done",
            culturalContext = "Factory reporting",
            isLearned = true
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { cardDao.getDirectCount() } returns 540
        coEvery { cardDao.getDirectLearnedCount() } returns 1
        coEvery { cardDao.deletePlaceholderCards() } returns 0
        every { cardDao.getAllCards() } returns flowOf(sampleCards)
        every { cardDao.getCardsByCategory("GREETINGS") } returns flowOf(listOf(sampleCards[0]))
        every { cardDao.getCardsByCategory("WORK") } returns flowOf(listOf(sampleCards[1]))
        every { cardDao.searchPhrases(any()) } returns flowOf(listOf(sampleCards[0]))

        repository = OfflinePhraseRepository(cardDao, progressDao, null, testDispatcher)
        viewModel = TranslationViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadLoadsAllCards() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        val results = viewModel.uiState.value.searchResults
        assertEquals(2, results.size)
        assertEquals("Apa khabar?", results[0].malayPhrase)
    }

    @Test
    fun testCategoryFilteringWithLegacyAliasMapping() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()

        // WORKPLACE is mapped to WORK
        viewModel.onCategorySelected("WORKPLACE")
        testScheduler.advanceUntilIdle()

        verify { cardDao.getCardsByCategory("WORK") }
        val results = viewModel.uiState.value.searchResults
        assertEquals(1, results.size)
        assertEquals("Kerja siap", results[0].malayPhrase)
    }

    @Test
    fun testToggleLearnedDelegatesToRepository() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()

        viewModel.toggleCardLearned(1L, true)
        testScheduler.advanceUntilIdle()

        coVerify { cardDao.updateLearnedStatus(1L, true, any()) }
    }
}
