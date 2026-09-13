package com.shawkinsrobertson.noguts.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.shawkinsrobertson.noguts.scoring.IntensityLevel

/**
 * One factor's contribution to one day's log. [weightSnapshot] and [calculatedPoints] are
 * captured at save time and never recomputed from the factor's current configuration -
 * that snapshot is what keeps a historical day stable when a factor's weight is edited
 * or the factor is later deactivated.
 */
@Entity(
    tableName = "daily_log_factors",
    foreignKeys = [
        ForeignKey(
            entity = DailyLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["dailyLogId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FactorEntity::class,
            parentColumns = ["id"],
            childColumns = ["factorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dailyLogId"), Index("factorId")]
)
data class DailyLogFactorEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dailyLogId: Long,
    val factorId: Long,
    /** Null for BOOLEAN-input factors, where selection alone implies full intensity. */
    val level: IntensityLevel?,
    val weightSnapshot: Double,
    val calculatedPoints: Double
)
