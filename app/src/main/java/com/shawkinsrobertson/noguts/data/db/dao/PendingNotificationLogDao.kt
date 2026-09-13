package com.shawkinsrobertson.noguts.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shawkinsrobertson.noguts.data.db.entity.PendingNotificationLogEntity
import java.time.LocalDate

@Dao
interface PendingNotificationLogDao {
    @Query("SELECT * FROM pending_notification_logs WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: LocalDate): PendingNotificationLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingNotificationLogEntity)

    @Query("DELETE FROM pending_notification_logs WHERE date = :date")
    suspend fun deleteByDate(date: LocalDate)
}
