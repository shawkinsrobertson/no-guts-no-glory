package com.shawkinsrobertson.noguts.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.shawkinsrobertson.noguts.data.db.entity.DailyLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: LocalDate): DailyLogEntity?

    @Query("SELECT * FROM daily_logs WHERE date = :date LIMIT 1")
    fun observeByDate(date: LocalDate): Flow<DailyLogEntity?>

    @Transaction
    @Query("SELECT * FROM daily_logs WHERE date = :date LIMIT 1")
    fun observeWithFactorsByDate(date: LocalDate): Flow<DailyLogWithFactorEntities?>

    @Query("SELECT * FROM daily_logs ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DailyLogEntity>>

    @Transaction
    @Query("SELECT * FROM daily_logs ORDER BY date DESC LIMIT :limit")
    fun observeRecentWithFactors(limit: Int): Flow<List<DailyLogWithFactorEntities>>

    @Query("SELECT * FROM daily_logs WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getBetween(start: LocalDate, end: LocalDate): List<DailyLogEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DailyLogEntity): Long

    @Update
    suspend fun update(entity: DailyLogEntity)
}
