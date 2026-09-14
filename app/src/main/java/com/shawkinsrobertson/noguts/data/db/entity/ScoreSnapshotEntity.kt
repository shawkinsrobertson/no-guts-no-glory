package com.shawkinsrobertson.noguts.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * The calculated result for one calendar day, stored rather than recomputed on every
 * screen. Written once when that day's log is saved and left untouched afterward, even
 * if factor weights, active/inactive state, or the personal target change later -
 * exactly the "historical logs unchanged" guarantee the scoring engine's tests assume.
 *
 * [rolling72Load]/[rolling72Percent] are null while the 3-day baseline is still building;
 * [daysInRollingWindow] records how many of the trailing 3 days had a log at the time this
 * snapshot was written, for the "Building your baseline" UI state.
 */
@Entity(tableName = "score_snapshots", indices = [Index(value = ["date"], unique = true)])
data class ScoreSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val dailyLoad: Double,
    val normalizedDailyPercent: Double,
    val rolling72Load: Double?,
    val rolling72Percent: Double?,
    val daysInRollingWindow: Int,
    val targetPercent: Double,
    val calculationVersion: Int,
    val createdAt: Instant
)
