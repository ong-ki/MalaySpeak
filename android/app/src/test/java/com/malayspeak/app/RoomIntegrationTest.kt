package com.malayspeak.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.malayspeak.app.data.local.AppDatabase
import com.malayspeak.app.data.local.entity.LearningProgress
import com.malayspeak.app.data.local.entity.VocabularyCard
import com.malayspeak.app.data.repository.OfflinePhraseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Genuine Room integration tests backed by a real SQLite database. DAOs are not
 * mocked here; every assertion goes through Room's generated implementation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveVocabularyCards() = runTest {
        val dao = database.vocabularyCardDao()
        dao.insertAll(
            listOf(
                card(malay = "Apa khabar?", category = "GREETINGS"),
                card(malay = "Berapa harga ini?", category = "MARKET")
            )
        )

        val cards = dao.getAllCards().first()
        assertEquals(2, cards.size)
        assertEquals("Apa khabar?", cards[0].malayPhrase)
        assertEquals("Berapa harga ini?", cards[1].malayPhrase)
        assertTrue(cards.all { !it.isLearned })
    }

    @Test
    fun markCardAsLearnedStoresFlagAndTimestamp() = runTest {
        val dao = database.vocabularyCardDao()
        dao.insertAll(listOf(card(malay = "Terima kasih")))
        val id = dao.getAllCards().first().single().id

        dao.updateLearnedStatus(id, isLearned = true, timestamp = 1_234L)

        val learned = dao.getLearnedCards().first()
        assertEquals(1, learned.size)
        assertEquals(id, learned.single().id)
        assertTrue(learned.single().isLearned)
        assertEquals(1_234L, learned.single().learnedTimestamp)
        assertEquals(1, dao.getDirectLearnedCount())
    }

    @Test
    fun markCardAsNotLearnedClearsTimestamp() = runTest {
        val dao = database.vocabularyCardDao()
        dao.insertAll(listOf(card(malay = "Terima kasih")))
        val id = dao.getAllCards().first().single().id

        dao.updateLearnedStatus(id, isLearned = true, timestamp = 1_234L)
        dao.updateLearnedStatus(id, isLearned = false, timestamp = null)

        val stored = dao.getAllCards().first().single()
        assertFalse(stored.isLearned)
        assertNull(stored.learnedTimestamp)
        assertEquals(0, dao.getDirectLearnedCount())
        assertEquals(0, dao.getLearnedCards().first().size)
    }

    @Test
    fun learnedAndCategoryCountsAreAccurate() = runTest {
        val dao = database.vocabularyCardDao()
        dao.insertAll(
            listOf(
                card(malay = "Apa khabar?", category = "GREETINGS"),
                card(malay = "Selamat pagi", category = "GREETINGS"),
                card(malay = "Berapa harga ini?", category = "MARKET")
            )
        )
        val greetingsId = dao.getAllCards().first().first { it.malayPhrase == "Apa khabar?" }.id
        dao.updateLearnedStatus(greetingsId, isLearned = true, timestamp = 10L)

        assertEquals(3, dao.getTotalCount().first())
        assertEquals(1, dao.getLearnedCount().first())
        assertEquals(2, dao.getTotalCountByCategory("GREETINGS").first())
        assertEquals(1, dao.getLearnedCountByCategory("GREETINGS").first())
        assertEquals(1, dao.getTotalCountByCategory("MARKET").first())
        assertEquals(0, dao.getLearnedCountByCategory("MARKET").first())
        assertEquals(
            setOf("GREETINGS", "MARKET"),
            dao.getAllCategories().first().toSet()
        )
    }

    @Test
    fun searchQueriesMatchAllFourSearchableFields() = runTest {
        val dao = database.vocabularyCardDao()
        dao.insertAll(
            listOf(
                card(
                    malay = "Berapa harga ini?",
                    english = "How much is this?",
                    myanmar = "ဒါ ဘယ်လောက်လဲ။",
                    pronunciation = "ဘရာပါ ဟာဂါ အိနိ?",
                    category = "MARKET"
                ),
                card(
                    malay = "Boleh bagi diskaun?",
                    english = "Can you give a discount?",
                    myanmar = "ဈေးလျှော့ပေးလို့ ရမလား။",
                    pronunciation = "ဘိုလဲ ဘာဂီ ဒစ်ကောင်း?",
                    category = "MARKET"
                )
            )
        )

        assertEquals("Berapa harga ini?", dao.searchPhrases("berapa").first().single().malayPhrase)
        assertEquals("Boleh bagi diskaun?", dao.searchPhrases("discount").first().single().malayPhrase)
        assertEquals("Berapa harga ini?", dao.searchPhrases("ဘယ်လောက်").first().single().malayPhrase)
        assertEquals("Boleh bagi diskaun?", dao.searchPhrases("ဒစ်ကောင်း").first().single().malayPhrase)
        assertEquals(0, dao.searchPhrases("tidak-ada-padanan").first().size)
    }

    @Test
    fun insertMissingPhrasesIsIdempotentAndPreservesLearnedFlags() = runTest {
        val dao = database.vocabularyCardDao()
        dao.insertAll(listOf(card(malay = "Apa khabar?")))
        val id = dao.getAllCards().first().single().id
        dao.updateLearnedStatus(id, isLearned = true, timestamp = 99L)

        val catalogue = listOf(
            card(malay = "Apa khabar?"),
            card(malay = "Selamat pagi")
        )
        assertEquals(1, dao.insertMissingPhrases(catalogue))
        assertEquals(0, dao.insertMissingPhrases(catalogue))

        val cards = dao.getAllCards().first()
        assertEquals(2, cards.size)
        val existing = cards.single { it.malayPhrase == "Apa khabar?" }
        assertTrue(existing.isLearned)
        assertEquals(99L, existing.learnedTimestamp)
    }

    @Test
    fun exactDuplicateInsertIsRejectedByUniqueIndex() = runTest {
        val dao = database.vocabularyCardDao()

        dao.insertAll(
            listOf(
                card(malay = "Apa khabar?"),
                card(malay = "Apa khabar?")
            )
        )

        assertEquals(1, dao.getTotalCount().first())
        assertEquals(1, dao.getAllCards().first().size)
    }

    @Test
    fun normalizedDuplicatesInOneSeedBatchAreCollapsed() = runTest {
        val dao = database.vocabularyCardDao()

        val inserted = dao.insertMissingPhrases(
            listOf(
                card(malay = "Terima kasih"),
                card(malay = "  terima   KASIH  ")
            )
        )

        assertEquals(1, inserted)
        assertEquals(1, dao.getDirectCount())
    }

    @Test
    fun normalizedDuplicateSeedDoesNotOverwriteLearnedState() = runTest {
        val dao = database.vocabularyCardDao()
        dao.insertAll(listOf(card(malay = "Apa khabar?")))
        val id = dao.getAllCards().first().single().id
        dao.updateLearnedStatus(id, isLearned = true, timestamp = 42L)

        val inserted = dao.insertMissingPhrases(
            listOf(
                card(malay = "  apa   KHABAR?  "),
                card(malay = "Selamat pagi")
            )
        )

        assertEquals(1, inserted)
        val cards = dao.getAllCards().first()
        assertEquals(2, cards.size)
        val original = cards.single { AppDatabase.normalizePhraseKey(it.malayPhrase) == "apa khabar?" }
        assertEquals(id, original.id)
        assertTrue(original.isLearned)
        assertEquals(42L, original.learnedTimestamp)
    }

    @Test
    fun repeatedCatalogueSeedingRemainsIdempotent() = runTest {
        val dao = database.vocabularyCardDao()
        val progressDao = database.learningProgressDao()
        val catalogue = listOf(
            card(malay = "Apa khabar?"),
            card(malay = "Selamat pagi"),
            card(malay = "Terima kasih")
        )

        repeat(3) {
            AppDatabase.seedMissingPhrases(dao, progressDao, catalogue)
        }

        assertEquals(3, dao.getDirectCount())
        assertEquals(3, progressDao.getProgressOnce()?.totalPhrases)
    }

    @Test
    fun repositorySeedingPathIsIdempotentAndPreservesLearnedState() = runTest {
        val dao = database.vocabularyCardDao()
        val progressDao = database.learningProgressDao()
        val repository = OfflinePhraseRepository(dao, progressDao, context)

        // This is the same seeding path used by the app on startup.
        repository.clearPlaceholdersAndRefreshSeed()
        assertEquals(660, dao.getDirectCount())

        val first = dao.getAllCards().first().first()
        dao.updateLearnedStatus(first.id, isLearned = true, timestamp = 321L)

        repository.clearPlaceholdersAndRefreshSeed()

        assertEquals(660, dao.getDirectCount())
        val stored = dao.getAllCards().first().single { it.id == first.id }
        assertTrue(stored.isLearned)
        assertEquals(321L, stored.learnedTimestamp)
        assertEquals(660, progressDao.getProgressOnce()?.totalPhrases)
    }

    @Test
    fun defaultProgressTotalMatchesShippedCatalogue() {
        assertEquals(540, LearningProgress().totalPhrases)
    }

    @Test
    fun resetAllProgressClearsLearnedFlags() = runTest {
        val dao = database.vocabularyCardDao()
        dao.insertAll(
            listOf(
                card(malay = "Apa khabar?"),
                card(malay = "Berapa harga ini?", category = "MARKET")
            )
        )
        val cards = dao.getAllCards().first()
        dao.updateLearnedStatus(cards[0].id, isLearned = true, timestamp = 10L)
        dao.updateLearnedStatus(cards[1].id, isLearned = true, timestamp = 20L)
        assertEquals(2, dao.getDirectLearnedCount())

        dao.resetAllProgress()

        assertEquals(0, dao.getDirectLearnedCount())
        assertTrue(dao.getAllCards().first().all { !it.isLearned && it.learnedTimestamp == null })
    }

    @Test
    fun fileBackedDatabasePersistsAcrossCloseAndReopen() = runTest {
        val databaseName = "persistence-${UUID.randomUUID()}.db"
        context.deleteDatabase(databaseName)

        var fileDatabase = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        try {
            fileDatabase.vocabularyCardDao().insertAll(listOf(card(malay = "Jumpa lagi")))
            val id = fileDatabase.vocabularyCardDao().getAllCards().first().single().id
            fileDatabase.vocabularyCardDao().updateLearnedStatus(id, isLearned = true, timestamp = 555L)
            fileDatabase.learningProgressDao().saveProgress(
                LearningProgress(
                    id = 1,
                    totalPhrases = 540,
                    learnedCount = 1,
                    dailyStreakDays = 4,
                    lastStudyTimestamp = 777L
                )
            )
        } finally {
            fileDatabase.close()
        }

        fileDatabase = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        try {
            val stored = fileDatabase.vocabularyCardDao().getAllCards().first().single()
            assertTrue(stored.isLearned)
            assertEquals(555L, stored.learnedTimestamp)

            val progress = fileDatabase.learningProgressDao().getProgressOnce()
            assertEquals(1, progress?.learnedCount)
            assertEquals(4, progress?.dailyStreakDays)
            assertEquals(777L, progress?.lastStudyTimestamp)
        } finally {
            fileDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun card(
        malay: String,
        category: String = "GREETINGS",
        english: String = "Test meaning",
        myanmar: String = "စမ်းသပ်အဓိပ္ပာယ်",
        pronunciation: String = "ဆမ်းသပ်"
    ) = VocabularyCard(
        category = category,
        categoryMm = "နှုတ်ဆက်ခြင်း",
        malayPhrase = malay,
        malayPronunciation = pronunciation,
        myanmarMeaning = myanmar,
        englishMeaning = english,
        culturalContext = "Integration test card",
        isLearned = false,
        learnedTimestamp = null
    )
}
