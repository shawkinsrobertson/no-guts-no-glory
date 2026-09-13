package com.shawkinsrobertson.noguts.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * LOGGED with zero attached factors means "nothing notable happened" - a real, legitimate
 * observation. SKIPPED means the user explicitly chose not to log. The absence of any
 * DailyLog row for a date means something different from either: "we don't know."
 */
enum class DailyLogStatus {
    LOGGED,
    SKIPPED
}

@Entity(tableName = "daily_logs", indices = [Index(value = ["date"], unique = true)])
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val status: DailyLogStatus,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
