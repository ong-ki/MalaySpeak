package com.malayspeak.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.malayspeak.app.data.local.dao.LearningProgressDao
import com.malayspeak.app.data.local.dao.VocabularyCardDao
import com.malayspeak.app.data.local.entity.LearningProgress
import com.malayspeak.app.data.local.entity.VocabularyCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

@Database(
    entities = [VocabularyCard::class, LearningProgress::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vocabularyCardDao(): VocabularyCardDao
    abstract fun learningProgressDao(): LearningProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Removes only known junk rows. Real phrase cards and their learned state
         * must never be removed by a migration.
         */
        private const val DELETE_PLACEHOLDER_CARDS_SQL =
            "DELETE FROM vocabulary_cards WHERE malayPhrase LIKE 'Frasa harian praktikal%' " +
                "OR malayPhrase = '' OR malayPhrase IS NULL"

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(DELETE_PLACEHOLDER_CARDS_SQL)
            }
        }

        /**
         * Version 6 ships the expanded 540-phrase catalogue. The previous version of
         * this migration deleted every phrase and the progress row, silently erasing
         * the user's learning history. It now performs cleanup only; missing catalogue
         * entries are merged in by [seedMissingPhrases] without overwriting existing
         * rows or learned flags.
         */
        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(DELETE_PLACEHOLDER_CARDS_SQL)
            }
        }

        /**
         * Version 7 adds schema-level protection against duplicate phrases.
         *
         * Existing rows are consolidated first, so the unique index can never
         * fail on an existing installation. Consolidation keeps one canonical row
         * per normalized phrase (preferring a learned row, then the most recent
         * learned timestamp, then the lowest id), carries the learned flag and
         * newest timestamp onto that row, and deletes only the redundant copies.
         * IDs of surviving rows, streaks, and progress timestamps are preserved.
         *
         * If duplicates were actually removed, learned/total counts are
         * reconciled to the surviving rows because the progress row stores an
         * aggregate of those same values.
         */
        internal val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val removedRows = deduplicateVocabularyCards(db)
                if (removedRows > 0) {
                    db.execSQL(
                        "UPDATE learning_progress SET " +
                            "learnedCount = (SELECT COUNT(*) FROM vocabulary_cards WHERE isLearned = 1), " +
                            "totalPhrases = (SELECT COUNT(*) FROM vocabulary_cards) " +
                            "WHERE id = 1"
                    )
                }
                db.execSQL("DROP INDEX IF EXISTS `index_vocabulary_cards_malayPhrase`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_vocabulary_cards_malayPhrase` " +
                        "ON `vocabulary_cards` (`malayPhrase`)"
                )
            }
        }

        internal val ALL_MIGRATIONS = arrayOf(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "malayspeak_offline.db"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .addCallback(DatabasePrepopulateCallback(appContext))
                    // No fallbackToDestructiveMigration(): an unsupported schema must
                    // fail loudly instead of wiping local learning data.
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val WHITESPACE_REGEX = Regex("\\s+")

        /**
         * Shared normalization used by both the migration and the DAO seeder.
         * Case and whitespace differences are treated as the same phrase, while
         * punctuation and wording remain significant.
         */
        internal fun normalizePhraseKey(phrase: String): String =
            phrase.trim().lowercase(Locale.ROOT).replace(WHITESPACE_REGEX, " ")

        /**
         * Merges normalized duplicate rows in place and returns the number of
         * deleted rows. Called from the 6 -> 7 migration before the unique index
         * is created.
         */
        internal fun deduplicateVocabularyCards(database: SupportSQLiteDatabase): Int {
            data class StoredCard(
                val id: Long,
                val isLearned: Boolean,
                val learnedTimestamp: Long?
            )

            val groups = LinkedHashMap<String, MutableList<StoredCard>>()
            database.query(
                "SELECT id, malayPhrase, isLearned, learnedTimestamp FROM vocabulary_cards"
            ).use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow("id")
                val phraseIndex = cursor.getColumnIndexOrThrow("malayPhrase")
                val learnedIndex = cursor.getColumnIndexOrThrow("isLearned")
                val timestampIndex = cursor.getColumnIndexOrThrow("learnedTimestamp")
                while (cursor.moveToNext()) {
                    val phrase = cursor.getString(phraseIndex) ?: ""
                    val stored = StoredCard(
                        id = cursor.getLong(idIndex),
                        isLearned = cursor.getInt(learnedIndex) != 0,
                        learnedTimestamp = if (cursor.isNull(timestampIndex)) null else cursor.getLong(timestampIndex)
                    )
                    groups.getOrPut(normalizePhraseKey(phrase)) { mutableListOf() }.add(stored)
                }
            }

            var removedRows = 0
            groups.values.forEach { rows ->
                if (rows.size <= 1) return@forEach
                val keeper = rows.sortedWith(
                    compareByDescending<StoredCard> { it.isLearned }
                        .thenByDescending { it.learnedTimestamp ?: Long.MIN_VALUE }
                        .thenBy { it.id }
                ).first()
                val mergedLearned = rows.any { it.isLearned }
                val mergedTimestamp = rows.mapNotNull { it.learnedTimestamp }.maxOrNull()

                if (keeper.isLearned != mergedLearned || keeper.learnedTimestamp != mergedTimestamp) {
                    database.execSQL(
                        "UPDATE vocabulary_cards SET isLearned = ?, learnedTimestamp = ? WHERE id = ?",
                        arrayOf<Any?>(if (mergedLearned) 1 else 0, mergedTimestamp, keeper.id)
                    )
                }
                rows.forEach { row ->
                    if (row.id != keeper.id) {
                        database.execSQL(
                            "DELETE FROM vocabulary_cards WHERE id = ?",
                            arrayOf<Any?>(row.id)
                        )
                        removedRows++
                    }
                }
            }
            return removedRows
        }

        private class DatabasePrepopulateCallback(private val context: Context) : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Safe cleanup: only remove known placeholder/blank rows.
                db.execSQL(DELETE_PLACEHOLDER_CARDS_SQL)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedMissingPhrases(
                            database.vocabularyCardDao(),
                            database.learningProgressDao(),
                            loadPhrasesFromAssets(context)
                        )
                    }
                }
            }

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedMissingPhrases(
                            database.vocabularyCardDao(),
                            database.learningProgressDao(),
                            loadPhrasesFromAssets(context)
                        )
                    }
                }
            }
        }

        /**
         * Inserts only catalogue phrases that are not already stored, matched by
         * normalized Malay text. Existing rows (including learned cards) are never
         * updated or deleted, and new rows receive generated IDs so they cannot
         * collide with existing installations.
         */
        internal suspend fun seedMissingPhrases(
            cardDao: VocabularyCardDao,
            progressDao: LearningProgressDao,
            phrases: List<VocabularyCard>
        ): Int {
            val inserted = cardDao.insertMissingPhrases(phrases)

            val total = cardDao.getDirectCount()
            val progress = progressDao.getProgressOnce()
            if (progress == null) {
                progressDao.saveProgress(
                    LearningProgress(
                        id = 1,
                        totalPhrases = total,
                        learnedCount = cardDao.getDirectLearnedCount(),
                        dailyStreakDays = 1,
                        lastStudyTimestamp = System.currentTimeMillis()
                    )
                )
            } else if (progress.totalPhrases != total) {
                // Preserve learnedCount/dailyStreakDays/lastStudyTimestamp exactly;
                // only reconcile the catalogue total.
                progressDao.updateTotalPhrases(total)
            }
            return inserted
        }

        fun loadPhrasesFromAssets(context: Context): List<VocabularyCard> {
            val list = mutableListOf<VocabularyCard>()
            try {
                val inputStream = context.assets.open("phrases.json")
                val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                val content = reader.use { it.readText() }
                val jsonArray = JSONArray(content)

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val category = obj.optString("category", "GREETINGS")
                    val categoryMm = obj.optString("categoryMm", "")
                    val malay = obj.optString("malay", "")
                    val pronunciation = obj.optString("malayPronunciation", obj.optString("pronunciation", ""))
                    val english = obj.optString("english", "")
                    val myanmar = obj.optString("myanmar", "")
                    val explanation = obj.optString("explanation", "")

                    list.add(
                        VocabularyCard(
                            id = (i + 1).toLong(),
                            category = category,
                            categoryMm = categoryMm,
                            malayPhrase = malay,
                            malayPronunciation = pronunciation,
                            myanmarMeaning = myanmar,
                            englishMeaning = english,
                            culturalContext = explanation,
                            isLearned = false,
                            learnedTimestamp = null
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val filtered = list.filterNot {
                it.malayPhrase.startsWith("Frasa harian praktikal") ||
                it.malayPhrase.isBlank()
            }
            return if (filtered.isNotEmpty()) filtered else com.malayspeak.app.data.local.seed.PhraseSeedData.getInitial500Phrases()
        }
    }
}
