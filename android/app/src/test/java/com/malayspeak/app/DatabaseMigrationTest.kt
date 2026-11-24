package com.malayspeak.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.malayspeak.app.data.local.AppDatabase
import com.malayspeak.app.data.local.entity.VocabularyCard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Verifies the real migration chain (4 -> 5 -> 6 -> 7) against real SQLite files.
 * Migrations must keep learned phrases, IDs, and progress records intact while
 * removing only known placeholder rows and normalized duplicate rows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun migration5To7PreservesLearnedCardsAndProgress() = runTest {
        val databaseName = "migration-${UUID.randomUUID()}.db"
        context.deleteDatabase(databaseName)
        try {
            createVersionedDatabase(databaseName, version = 5)

            val migrated = openMigrated(databaseName)
            try {
                val cards = migrated.vocabularyCardDao().getAllCards().first()

                // Placeholder row removed, both real rows retained.
                assertEquals(2, cards.size)
                assertTrue(cards.none { it.malayPhrase.startsWith("Frasa harian praktikal") })

                val learnedCard = cards.single { it.malayPhrase == "Apa khabar?" }
                assertEquals(1L, learnedCard.id)
                assertTrue(learnedCard.isLearned)
                assertEquals(1_234L, learnedCard.learnedTimestamp)

                val unlearnedCard = cards.single { it.malayPhrase == "Legacy phrase" }
                assertFalse(unlearnedCard.isLearned)
                assertNull(unlearnedCard.learnedTimestamp)

                assertEquals(1, migrated.vocabularyCardDao().getDirectLearnedCount())
                assertEquals(7, migrated.openHelper.readableDatabase.version)

                // Progress row is preserved exactly as it was before migration.
                val progress = migrated.learningProgressDao().getProgressOnce()
                assertNotNull(progress)
                assertEquals(1, progress?.learnedCount)
                assertEquals(500, progress?.totalPhrases)
                assertEquals(7, progress?.dailyStreakDays)
                assertEquals(999L, progress?.lastStudyTimestamp)

                // The post-migration merge adds only new catalogue phrases and
                // reconciles the total without changing learned progress.
                val inserted = AppDatabase.seedMissingPhrases(
                    migrated.vocabularyCardDao(),
                    migrated.learningProgressDao(),
                    listOf(
                        seedCard("Apa khabar?"),
                        seedCard("Brand new phrase")
                    )
                )
                assertEquals(1, inserted)
                assertEquals(3, migrated.vocabularyCardDao().getAllCards().first().size)

                val mergedProgress = migrated.learningProgressDao().getProgressOnce()
                assertEquals(3, mergedProgress?.totalPhrases)
                assertEquals(1, mergedProgress?.learnedCount)
                assertEquals(7, mergedProgress?.dailyStreakDays)
                assertEquals(999L, mergedProgress?.lastStudyTimestamp)
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun migration4To7PreservesLearnedCardsAndProgress() = runTest {
        val databaseName = "migration-${UUID.randomUUID()}.db"
        context.deleteDatabase(databaseName)
        try {
            createVersionedDatabase(databaseName, version = 4)

            val migrated = openMigrated(databaseName)
            try {
                assertEquals(7, migrated.openHelper.readableDatabase.version)

                val cards = migrated.vocabularyCardDao().getAllCards().first()
                assertEquals(2, cards.size)
                val learnedCard = cards.single { it.malayPhrase == "Apa khabar?" }
                assertEquals(1L, learnedCard.id)
                assertTrue(learnedCard.isLearned)
                assertEquals(1_234L, learnedCard.learnedTimestamp)

                val progress = migrated.learningProgressDao().getProgressOnce()
                assertEquals(1, progress?.learnedCount)
                assertEquals(500, progress?.totalPhrases)
                assertEquals(7, progress?.dailyStreakDays)
                assertEquals(999L, progress?.lastStudyTimestamp)
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun migration6To7ConsolidatesNormalizedDuplicatesAndPreservesLearning() = runTest {
        val databaseName = "migration-${UUID.randomUUID()}.db"
        context.deleteDatabase(databaseName)
        try {
            createDuplicateVersion6Database(databaseName)

            val migrated = openMigrated(databaseName)
            try {
                assertEquals(7, migrated.openHelper.readableDatabase.version)

                // Three normalized copies of "Apa khabar?" collapse to the
                // canonical learned row (highest timestamp, then lowest id).
                val cards = migrated.vocabularyCardDao().getAllCards().first()
                assertEquals(2, cards.size)

                val kept = cards.single { it.malayPhrase == "APA KHABAR?" }
                assertEquals(3L, kept.id)
                assertTrue(kept.isLearned)
                assertEquals(900L, kept.learnedTimestamp)

                val untouched = cards.single { it.malayPhrase == "Terima kasih" }
                assertEquals(4L, untouched.id)
                assertFalse(untouched.isLearned)

                // Aggregate progress is reconciled to the surviving rows, while
                // the streak and last-study timestamp are preserved.
                val progress = migrated.learningProgressDao().getProgressOnce()
                assertNotNull(progress)
                assertEquals(2, progress?.totalPhrases)
                assertEquals(1, progress?.learnedCount)
                assertEquals(5, progress?.dailyStreakDays)
                assertEquals(999L, progress?.lastStudyTimestamp)

                // The forward migration leaves a working unique index behind.
                migrated.vocabularyCardDao().insertAll(listOf(seedCard("Terima kasih")))
                assertEquals(2, migrated.vocabularyCardDao().getAllCards().first().size)
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun unsupportedVersionsFailWithoutDeletingExistingData() {
        for (version in 1..3) {
            val databaseName = "legacy-$version-${UUID.randomUUID()}.db"
            context.deleteDatabase(databaseName)
            try {
                createLegacyDatabase(databaseName, version)

                val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
                    .addMigrations(*AppDatabase.ALL_MIGRATIONS)
                    .allowMainThreadQueries()
                    .build()
                try {
                    // No migration path exists for 1-3: opening must fail loudly
                    // rather than silently wiping the file.
                    assertThrows(IllegalStateException::class.java) {
                        database.openHelper.writableDatabase
                    }
                } finally {
                    database.close()
                }

                // The original data must still be present after the failed open.
                val raw = SQLiteDatabase.openDatabase(
                    context.getDatabasePath(databaseName).path,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                try {
                    raw.rawQuery("SELECT COUNT(*) FROM legacy_data", null).use { cursor ->
                        assertTrue(cursor.moveToFirst())
                        assertEquals(1, cursor.getInt(0))
                    }
                } finally {
                    raw.close()
                }
            } finally {
                context.deleteDatabase(databaseName)
            }
        }
    }

    private fun openMigrated(databaseName: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

    private fun seedCard(malay: String) = VocabularyCard(
        category = "GREETINGS",
        categoryMm = "နှုတ်ဆက်ခြင်း",
        malayPhrase = malay,
        malayPronunciation = "ဆမ်းသပ်",
        myanmarMeaning = "စမ်းသပ်အဓိပ္ပာယ်",
        englishMeaning = "Test meaning",
        culturalContext = "Migration merge test",
        isLearned = false,
        learnedTimestamp = null
    )

    private fun createVersionedDatabase(databaseName: String, version: Int) {
        withDatabase(databaseName) { sqlite ->
            createSchema(sqlite)
            insertCard(sqlite, id = 1, malay = "Apa khabar?", isLearned = true, learnedTimestamp = 1_234L)
            insertCard(sqlite, id = 2, malay = "Legacy phrase", isLearned = false, learnedTimestamp = null)
            insertCard(
                sqlite,
                id = 3,
                malay = "Frasa harian praktikal 1",
                isLearned = false,
                learnedTimestamp = null
            )
            insertProgress(sqlite, total = 500, learned = 1, streak = 7, timestamp = 999L)
            sqlite.version = version
        }
    }

    /**
     * Version 6 database containing case/whitespace variants of the same phrase.
     * The learned copies have different timestamps so the merge rule is testable.
     */
    private fun createDuplicateVersion6Database(databaseName: String) {
        withDatabase(databaseName) { sqlite ->
            createSchema(sqlite)
            insertCard(sqlite, id = 1, malay = "Apa khabar?", isLearned = false, learnedTimestamp = null)
            insertCard(sqlite, id = 2, malay = "apa  khabar?", isLearned = true, learnedTimestamp = 500L)
            insertCard(sqlite, id = 3, malay = "APA KHABAR?", isLearned = true, learnedTimestamp = 900L)
            insertCard(sqlite, id = 4, malay = "Terima kasih", isLearned = false, learnedTimestamp = null)
            insertProgress(sqlite, total = 4, learned = 2, streak = 5, timestamp = 999L)
            sqlite.version = 6
        }
    }

    private fun createLegacyDatabase(databaseName: String, version: Int) {
        withDatabase(databaseName) { sqlite ->
            sqlite.execSQL("CREATE TABLE legacy_data (value TEXT NOT NULL)")
            sqlite.execSQL("INSERT INTO legacy_data (value) VALUES ('keep-me')")
            sqlite.version = version
        }
    }

    private fun withDatabase(databaseName: String, block: (SQLiteDatabase) -> Unit) {
        val databaseFile = context.getDatabasePath(databaseName)
        databaseFile.parentFile?.mkdirs()
        val sqlite = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
        try {
            block(sqlite)
        } finally {
            sqlite.close()
        }
    }

    private fun createSchema(sqlite: SQLiteDatabase) {
        sqlite.execSQL(
            "CREATE TABLE IF NOT EXISTS `vocabulary_cards` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`category` TEXT NOT NULL, " +
                "`categoryMm` TEXT NOT NULL, " +
                "`malayPhrase` TEXT NOT NULL, " +
                "`malayPronunciation` TEXT NOT NULL, " +
                "`myanmarMeaning` TEXT NOT NULL, " +
                "`englishMeaning` TEXT NOT NULL, " +
                "`culturalContext` TEXT NOT NULL, " +
                "`isLearned` INTEGER NOT NULL, " +
                "`learnedTimestamp` INTEGER)"
        )
        sqlite.execSQL("CREATE INDEX IF NOT EXISTS `index_vocabulary_cards_category` ON `vocabulary_cards` (`category`)")
        sqlite.execSQL("CREATE INDEX IF NOT EXISTS `index_vocabulary_cards_malayPhrase` ON `vocabulary_cards` (`malayPhrase`)")
        sqlite.execSQL("CREATE INDEX IF NOT EXISTS `index_vocabulary_cards_englishMeaning` ON `vocabulary_cards` (`englishMeaning`)")
        sqlite.execSQL("CREATE INDEX IF NOT EXISTS `index_vocabulary_cards_myanmarMeaning` ON `vocabulary_cards` (`myanmarMeaning`)")
        sqlite.execSQL("CREATE INDEX IF NOT EXISTS `index_vocabulary_cards_isLearned` ON `vocabulary_cards` (`isLearned`)")
        sqlite.execSQL(
            "CREATE TABLE IF NOT EXISTS `learning_progress` (" +
                "`id` INTEGER NOT NULL, " +
                "`totalPhrases` INTEGER NOT NULL, " +
                "`learnedCount` INTEGER NOT NULL, " +
                "`dailyStreakDays` INTEGER NOT NULL, " +
                "`lastStudyTimestamp` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
    }

    private fun insertCard(
        sqlite: SQLiteDatabase,
        id: Int,
        malay: String,
        isLearned: Boolean,
        learnedTimestamp: Long?
    ) {
        sqlite.execSQL(
            "INSERT INTO vocabulary_cards " +
                "(id, category, categoryMm, malayPhrase, malayPronunciation, myanmarMeaning, " +
                "englishMeaning, culturalContext, isLearned, learnedTimestamp) " +
                "VALUES ($id, 'GREETINGS', 'နှုတ်ဆက်ခြင်း', ?, 'အာပါ ခါဘာရ်', 'နေကောင်းလား။', " +
                "'How are you?', 'Test context', ${if (isLearned) 1 else 0}, ?)",
            arrayOf<Any?>(malay, learnedTimestamp)
        )
    }

    private fun insertProgress(
        sqlite: SQLiteDatabase,
        total: Int,
        learned: Int,
        streak: Int,
        timestamp: Long
    ) {
        sqlite.execSQL(
            "INSERT INTO learning_progress " +
                "(id, totalPhrases, learnedCount, dailyStreakDays, lastStudyTimestamp) " +
                "VALUES (1, $total, $learned, $streak, $timestamp)"
        )
    }
}
