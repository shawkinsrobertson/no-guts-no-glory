package com.shawkinsrobertson.noguts.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shawkinsrobertson.noguts.data.db.entity.DailyLogFactorEntity
import java.time.LocalDate

@Dao
interface DailyLogFactorDao {
    @Query("SELECT * FROM daily_log_factors WHERE dailyLogId = :dailyLogId")
    suspend fun getForLog(dailyLogId: Long): List<DailyLogFactorEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<DailyLogFactorEntity>)

    @Query("DELETE FROM daily_log_factors WHERE dailyLogId = :dailyLogId")
    suspend fun deleteForLog(dailyLogId: Long)

    /** Selection frequency/recency for the notification's personalized ranking (section 24). */
    @Query(
        """
        SELECT dlf.factorId AS factorId, COUNT(*) AS selectionCount, MAX(dl.date) AS lastSelectedDate
        FROM daily_log_factors dlf
        INNER JOIN daily_logs dl ON dl.id = dlf.dailyLogId
        WHERE dl.date >= :sinceDate
        GROUP BY dlf.factorId
        """
    )
    suspend fun getSelectionStatsSince(sinceDate: LocalDate): List<FactorSelectionStat>
}
