package com.shawkinsrobertson.noguts.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * The in-progress state of a day's log as it's being built from notification taps. Kept
 * in Room (not just in-memory in the receiver) so it survives the process being killed
 * between taps - the notification is not allowed to depend on the app staying alive.
 */
@Entity(tableName = "pending_notification_logs", indices = [Index(value = ["date"], unique = true)])
data class PendingNotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val selectedFactorIds: List<Long>,
    val notes: String? = null,
    val createdAt: Instant
)
