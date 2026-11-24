package com.malayspeak.app.data.repository

import android.content.Context
import com.malayspeak.app.data.local.AppDatabase
import com.malayspeak.app.data.local.dao.LearningProgressDao
import com.malayspeak.app.data.local.dao.VocabularyCardDao
import com.malayspeak.app.data.local.entity.LearningProgress
import com.malayspeak.app.data.local.entity.VocabularyCard
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class OfflinePhraseRepository(
    private val cardDao: VocabularyCardDao,
    private val progressDao: LearningProgressDao,
    private val context: Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun getAllCards(): Flow<List<VocabularyCard>> = cardDao.getAllCards()

    fun getCardsByCategory(category: String): Flow<List<VocabularyCard>> =
        cardDao.getCardsByCategory(category)

    fun searchPhrases(query: String): Flow<List<VocabularyCard>> =
        cardDao.searchPhrases(query)

    fun getLearnedCards(): Flow<List<VocabularyCard>> = cardDao.getLearnedCards()

    fun getTotalCount(): Flow<Int> = cardDao.getTotalCount()

    fun getLearnedCount(): Flow<Int> = cardDao.getLearnedCount()

    fun getProgress(): Flow<LearningProgress?> = progressDao.getProgress()

    suspend fun updateCardLearned(cardId: Long, isLearned: Boolean) = withContext(ioDispatcher) {
        val timestamp = if (isLearned) System.currentTimeMillis() else null
        cardDao.updateLearnedStatus(cardId, isLearned, timestamp)
        val currentLearned = cardDao.getDirectLearnedCount()
        progressDao.updateLearnedCount(currentLearned, System.currentTimeMillis())
    }

    suspend fun resetAllProgress() = withContext(ioDispatcher) {
        cardDao.resetAllProgress()
        progressDao.resetProgress()
    }

    suspend fun clearPlaceholdersAndRefreshSeed() = withContext(ioDispatcher) {
        cardDao.deletePlaceholderCards()
        val phrases = if (context != null) {
            val fromAssets = AppDatabase.loadPhrasesFromAssets(context)
            if (fromAssets.isNotEmpty()) fromAssets else com.malayspeak.app.data.local.seed.PhraseSeedData.getInitial500Phrases()
        } else {
            com.malayspeak.app.data.local.seed.PhraseSeedData.getInitial500Phrases()
        }
        AppDatabase.seedMissingPhrases(cardDao, progressDao, phrases)
    }

    suspend fun ensureDatabaseSeeded() = withContext(ioDispatcher) {
        clearPlaceholdersAndRefreshSeed()
    }
}
