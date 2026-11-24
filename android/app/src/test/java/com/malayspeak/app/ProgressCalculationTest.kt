package com.malayspeak.app

import com.malayspeak.app.data.local.entity.VocabularyCard
import com.malayspeak.app.ui.screens.progress.CategoryProgressItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressCalculationTest {

    private fun percentage(learned: Int, total: Int): Int =
        if (total > 0) (learned * 100) / total else 0

    @Test
    fun testProgressPercentageCalculation() {
        val total = 540
        val learned = 108
        assertEquals(20, percentage(learned, total))
    }

    @Test
    fun testProgressPercentageWithZeroTotal() {
        val total = 0
        val learned = 0
        assertEquals(0, percentage(learned, total))
    }

    @Test
    fun testCategoryProgressBreakdown() {
        val cards = listOf(
            VocabularyCard(1, "GREETINGS", "နှုတ်ဆက်ခြင်း", "Hai", "ဟိုင်း", "ဟိုင်း", "Hi", "Hi", true, 1000L),
            VocabularyCard(2, "GREETINGS", "နှုတ်ဆက်ခြင်း", "Khabar baik", "ခါဘာရ် ဘာအစ်ခ်", "နေကောင်း", "Fine", "Fine", false, null),
            VocabularyCard(3, "MARKET", "ဈေးဝယ်ခြင်း", "Berapa", "ဘရာပါ", "ဘယ်လောက်", "How much", "How much", true, 2000L),
            VocabularyCard(4, "MARKET", "ဈေးဝယ်ခြင်း", "Mahal", "မာဟယ်လ်", "ဈေးကြီး", "Expensive", "Expensive", false, null)
        )

        val categoryGroups = cards.groupBy { it.category }
        val catProgress = categoryGroups.map { (cat, catCards) ->
            val catLearned = catCards.count { it.isLearned }
            val catPct = if (catCards.isNotEmpty()) (catLearned * 100) / catCards.size else 0
            val catMm = catCards.firstOrNull()?.categoryMm ?: cat
            CategoryProgressItem(
                category = cat,
                categoryMm = catMm,
                totalCount = catCards.size,
                learnedCount = catLearned,
                percentage = catPct
            )
        }

        assertEquals(2, catProgress.size)
        val greetings = catProgress.first { it.category == "GREETINGS" }
        assertEquals(2, greetings.totalCount)
        assertEquals(1, greetings.learnedCount)
        assertEquals(50, greetings.percentage)
        assertEquals("နှုတ်ဆက်ခြင်း", greetings.categoryMm)

        val market = catProgress.first { it.category == "MARKET" }
        assertEquals(2, market.totalCount)
        assertEquals(1, market.learnedCount)
        assertEquals(50, market.percentage)
        assertEquals("ဈေးဝယ်ခြင်း", market.categoryMm)
    }

    @Test
    fun testRecentlyLearnedSortingAndLimit() {
        val learnedCards = (1..20).map { i ->
            VocabularyCard(
                id = i.toLong(),
                category = "GREETINGS",
                categoryMm = "နှုတ်ဆက်ခြင်း",
                malayPhrase = "Phrase $i",
                malayPronunciation = "Pron $i",
                myanmarMeaning = "Meaning $i",
                englishMeaning = "English $i",
                culturalContext = "Context $i",
                isLearned = true,
                learnedTimestamp = i * 1000L
            )
        }.sortedByDescending { it.learnedTimestamp }

        val recent = learnedCards.take(15)
        assertEquals(15, recent.size)
        // Most recent should have the highest timestamp (id 20)
        assertEquals(20L, recent.first().id)
        // 15th item should have id 6
        assertEquals(6L, recent.last().id)
    }

    @Test
    fun testLearnedToggleAndResetLogic() {
        val cards = mutableListOf(
            VocabularyCard(1, "GREETINGS", "နှုတ်ဆက်ခြင်း", "Hai", "ဟိုင်း", "ဟိုင်း", "Hi", "Hi", false, null),
            VocabularyCard(2, "MARKET", "ဈေးဝယ်ခြင်း", "Berapa", "ဘရာပါ", "ဘယ်လောက်", "How much", "How much", false, null)
        )

        // Mark card 1 as learned
        val now = System.currentTimeMillis()
        cards[0] = cards[0].copy(isLearned = true, learnedTimestamp = now)
        assertTrue(cards[0].isLearned)
        assertEquals(now, cards[0].learnedTimestamp)

        // Reset all progress
        val resetCards = cards.map { it.copy(isLearned = false, learnedTimestamp = null) }
        assertEquals(0, resetCards.count { it.isLearned })
        assertTrue(resetCards.all { it.learnedTimestamp == null })
    }
}
