package com.shawkinsrobertson.noguts.data.repository

import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import com.shawkinsrobertson.noguts.scoring.FactorCategory
import com.shawkinsrobertson.noguts.scoring.InputType
import java.time.Instant

/**
 * The V1 seed library from the implementation plan (section 4) - a modest starting set
 * rather than the whole space of possible factors. Weights are a reasonable starting
 * point; they're fully editable from Settings and every historical log snapshots its own
 * weight, so retuning them later never rewrites history.
 *
 * [sortOrder] doubles as the "most relevant/common" ranking used to decide which factors
 * show first on the dashboard's quick-select and in the initial notification layout
 * before any personal usage history exists.
 */
object DefaultFactors {

    private data class Seed(
        val name: String,
        val category: FactorCategory,
        val inputType: InputType,
        val weight: Double,
        val sortOrder: Int
    )

    private val seeds = listOf(
        // Load - the six most commonly reached-for ones get low sortOrder values so they
        // surface first (matching the plan's dashboard/notification mockups).
        Seed("Poor sleep", FactorCategory.LOAD, InputType.LEVEL, weight = 9.0, sortOrder = 0),
        Seed("Major stress", FactorCategory.LOAD, InputType.LEVEL, weight = 9.0, sortOrder = 1),
        Seed("Social load", FactorCategory.LOAD, InputType.LEVEL, weight = 8.0, sortOrder = 2),
        Seed("Alcohol", FactorCategory.LOAD, InputType.LEVEL, weight = 10.0, sortOrder = 3),
        Seed("Caffeine above normal", FactorCategory.LOAD, InputType.LEVEL, weight = 6.0, sortOrder = 4),
        Seed("Lack of alone/recovery time", FactorCategory.LOAD, InputType.LEVEL, weight = 7.0, sortOrder = 5),
        Seed("NSAIDs", FactorCategory.LOAD, InputType.BOOLEAN, weight = 8.0, sortOrder = 10),
        Seed("Rich/fatty food", FactorCategory.LOAD, InputType.LEVEL, weight = 7.0, sortOrder = 11),
        Seed("Spicy food", FactorCategory.LOAD, InputType.LEVEL, weight = 7.0, sortOrder = 12),
        Seed("Known personal food trigger", FactorCategory.LOAD, InputType.LEVEL, weight = 9.0, sortOrder = 13),
        Seed("Travel/time-zone disruption", FactorCategory.LOAD, InputType.BOOLEAN, weight = 6.0, sortOrder = 14),
        Seed("Significant meal disruption", FactorCategory.LOAD, InputType.BOOLEAN, weight = 5.0, sortOrder = 15),

        // Recovery
        Seed("Full recovery/alone day", FactorCategory.RECOVERY, InputType.BOOLEAN, weight = 8.0, sortOrder = 6),
        Seed("Several hours of genuine decompression", FactorCategory.RECOVERY, InputType.LEVEL, weight = 6.0, sortOrder = 20),
        Seed("Excellent sleep", FactorCategory.RECOVERY, InputType.BOOLEAN, weight = 6.0, sortOrder = 21),
        Seed("Low-stress restorative day", FactorCategory.RECOVERY, InputType.BOOLEAN, weight = 6.0, sortOrder = 22),
        Seed("Regular gentle meals", FactorCategory.RECOVERY, InputType.BOOLEAN, weight = 4.0, sortOrder = 23),
        Seed("Gentle restorative movement", FactorCategory.RECOVERY, InputType.BOOLEAN, weight = 4.0, sortOrder = 24),
        Seed("Recovery after travel", FactorCategory.RECOVERY, InputType.BOOLEAN, weight = 4.0, sortOrder = 25),
        Seed("Alcohol-free day following recent drinking", FactorCategory.RECOVERY, InputType.BOOLEAN, weight = 4.0, sortOrder = 26),
        Seed("NSAID-free day following recent exposure", FactorCategory.RECOVERY, InputType.BOOLEAN, weight = 3.0, sortOrder = 27),

        // Symptoms - tracked with intensity, never scored (weight is unused/zero).
        Seed("Burping", FactorCategory.SYMPTOM, InputType.LEVEL, weight = 0.0, sortOrder = 30),
        Seed("Upper-abdominal tightening", FactorCategory.SYMPTOM, InputType.LEVEL, weight = 0.0, sortOrder = 31),
        Seed("Gnawing/irritated feeling", FactorCategory.SYMPTOM, InputType.LEVEL, weight = 0.0, sortOrder = 32),
        Seed("Cramping/spasm", FactorCategory.SYMPTOM, InputType.LEVEL, weight = 0.0, sortOrder = 33),
        Seed("Other familiar early symptom", FactorCategory.SYMPTOM, InputType.LEVEL, weight = 0.0, sortOrder = 34)
    )

    fun seedEntities(): List<FactorEntity> {
        val now = Instant.now()
        return seeds.map { seed ->
            FactorEntity(
                name = seed.name,
                category = seed.category,
                inputType = seed.inputType,
                weight = seed.weight,
                active = true,
                sortOrder = seed.sortOrder,
                isSystemDefault = true,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
