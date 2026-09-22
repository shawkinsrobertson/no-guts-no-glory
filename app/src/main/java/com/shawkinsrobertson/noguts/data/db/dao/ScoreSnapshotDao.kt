package com.shawkinsrobertson.noguts.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shawkinsrobertson.noguts.data.db.entity.ScoreSnapshotEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ScoreSnapshotDao {
    @Query("SELECT * FROM score_snapshots WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: LocalDate): ScoreSnapshotEntity?

    @Query("SELECT * FROM score_snapshots ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ScoreSnapshotEntity>>

    @Query("SELECT * FROM score_snapshots ORDER BY date DESC LIMIT 1")
    fun observeLatest(): Flow<ScoreSnapshotEntity?>

    @Query("SELECT * FROM score_snapshots WHERE date = :date LIMIT 1")
    fun observeByDate(date: LocalDate): Flow<ScoreSnapshotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScoreSnapshotEntity)
}
