package com.application.polarapplication.ai.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.application.polarapplication.model.TrainingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TrainingSessionEntity)

    @Query("SELECT * FROM training_sessions ORDER BY date DESC")
    fun getAllSessionsFlow(): Flow<List<TrainingSessionEntity>>

    @Delete
    suspend fun deleteSession(session: TrainingSessionEntity)
}
