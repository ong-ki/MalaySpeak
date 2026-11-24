package com.malayspeak.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.malayspeak.app.data.local.seed.PhraseSeedData

/**
 * Room Database entity representing the user's overall local learning progress.
 * Maintained completely offline on device.
 */
@Entity(tableName = "learning_progress")
data class LearningProgress(
    @PrimaryKey
    val id: Int = 1,
    val totalPhrases: Int = PhraseSeedData.TOTAL_PHRASE_COUNT,
    val learnedCount: Int = 0,
    val dailyStreakDays: Int = 1,
    val lastStudyTimestamp: Long = System.currentTimeMillis()
)
