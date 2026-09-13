package com.shawkinsrobertson.noguts.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shawkinsrobertson.noguts.data.db.entity.DailyLogFactorEntity

@Dao
interface DailyLogFactorDao {
    @Query("SELECT * FROM daily_log_factors WHERE dailyLogId = :dailyLogId")
    suspend fun getForLog(dailyLogId: Long): List<DailyLogFactorEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<DailyLogFactorEntity>)

    @Query("DELETE FROM daily_log_factors WHERE dailyLogId = :dailyLogId")
    suspend fun deleteForLog(dailyLogId: Long)
}
