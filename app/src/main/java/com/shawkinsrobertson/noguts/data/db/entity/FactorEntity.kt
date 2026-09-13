package com.shawkinsrobertson.noguts.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shawkinsrobertson.noguts.scoring.FactorCategory
import com.shawkinsrobertson.noguts.scoring.InputType
import java.time.Instant

/**
 * A trackable factor: a load contributor, a recovery behavior, or a symptom. Symptom
 * factors are stored and logged the same way as the others but are never passed to the
 * scoring engine (see DailyLogRepository) - they exist purely for pattern-spotting.
 */
@Entity(tableName = "factors")
data class FactorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: FactorCategory,
    val inputType: InputType,
    /** Maximum point contribution this factor can make on a given day. */
    val weight: Double,
    val active: Boolean = true,
    /** Lower sorts first; used to surface the most relevant factors on the dashboard
     * and in the notification without showing the whole library. */
    val sortOrder: Int = 0,
    val isSystemDefault: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
)
