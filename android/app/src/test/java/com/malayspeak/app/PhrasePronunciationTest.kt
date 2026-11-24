package com.malayspeak.app

import com.malayspeak.app.data.local.seed.PhraseSeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhrasePronunciationTest {

    @Test
    fun testAllPhrasesHaveValidBurmesePronunciation() {
        val phrases = PhraseSeedData.getInitial500Phrases()
        assertTrue("Seed phrases must not be empty", phrases.isNotEmpty())

        val missingOrEmptyPronunciations = phrases.filter { it.malayPronunciation.isBlank() }
        assertEquals("All phrases must have a Burmese phonetic guide", 0, missingOrEmptyPronunciations.size)
    }

    @Test
    fun testNoPlaceholderOrObsoleteCardsInSeedData() {
        val phrases = PhraseSeedData.getInitial500Phrases()

        val placeholders = phrases.filter {
            it.malayPhrase.startsWith("Frasa harian praktikal") ||
            it.malayPhrase.isBlank() ||
            it.myanmarMeaning.isBlank() ||
            it.englishMeaning.isBlank()
        }
        assertEquals("Seed data must not contain placeholder or empty phrases", 0, placeholders.size)
    }

    @Test
    fun testAllTenCategoriesHaveRichPhrases() {
        val phrases = PhraseSeedData.getInitial500Phrases()
        val categoryCounts = phrases.groupBy { it.category }.mapValues { it.value.size }

        val expectedCategories = listOf(
            "GREETINGS", "MARKET", "DINING", "TRANSPORT", "WORK",
            "HOUSING", "NUMBERS_TIME", "HEALTH_EMERGENCY", "DAILY_LIFE", "SLANG_PARTICLES"
        )

        for (cat in expectedCategories) {
            val count = categoryCounts[cat] ?: 0
            assertTrue("Category $cat must have at least 30 phrases", count >= 30)
        }
    }

    @Test
    fun testPronunciationMatchesBurmeseScript() {
        val phrases = PhraseSeedData.getInitial500Phrases()
        // Burmese Unicode range: \u1000-\u109F
        val burmeseRegex = Regex("[\u1000-\u109F]")

        val nonBurmesePronunciations = phrases.filter {
            !burmeseRegex.containsMatchIn(it.malayPronunciation)
        }
        assertEquals("Pronunciations must contain Burmese script characters", 0, nonBurmesePronunciations.size)
    }
}
