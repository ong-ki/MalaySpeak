package com.malayspeak.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.malayspeak.app.data.local.entity.LearningProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningProgressDao {

    @Query("SELECT * FROM learning_progress WHERE id = 1")
    fun getProgress(): Flow<LearningProgress?>

    @Query("SELECT * FROM learning_progress WHERE id = 1")
    suspend fun getProgressOnce(): LearningProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: LearningProgress)

    @Query("UPDATE learning_progress SET totalPhrases = :totalPhrases WHERE id = 1")
    suspend fun updateTotalPhrases(totalPhrases: Int)

    @Query("UPDATE learning_progress SET learnedCount = :learnedCount, lastStudyTimestamp = :timestamp WHERE id = 1")
    suspend fun updateLearnedCount(learnedCount: Int, timestamp: Long)

    @Query("UPDATE learning_progress SET learnedCount = 0 WHERE id = 1")
    suspend fun resetProgress()
}
