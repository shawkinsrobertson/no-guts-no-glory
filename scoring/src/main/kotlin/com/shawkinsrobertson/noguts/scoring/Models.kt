package com.shawkinsrobertson.noguts.scoring

import java.time.LocalDate

/**
 * A factor's role in the load calculation. Symptoms are tracked but never contribute
 * points — they exist so the app can look for correlations without conflating
 * "things that might raise load" with "things that indicate a flare."
 */
enum class FactorCategory {
    LOAD,
    RECOVERY,
    SYMPTOM
}

/**
 * How a factor is logged. BOOLEAN factors are simple yes/no toggles; LEVEL factors
 * carry an intensity that scales their contribution.
 */
enum class InputType {
    BOOLEAN,
    LEVEL
}

/**
 * The four intensity tiers available to LEVEL factors. The multiplier is applied to a
 * factor's configured weight to produce that day's points for the factor.
 */
enum class IntensityLevel(val multiplier: Double) {
    NONE(0.0),
    MILD(0.3),
    MODERATE(0.6),
    SEVERE(1.0);

    companion object {
        /** The multiplier used for a BOOLEAN factor that was selected. */
        const val BOOLEAN_SELECTED_MULTIPLIER = 1.0
    }
}

/**
 * One factor's contribution to a single day's log, already resolved to a point value.
 *
 * [weight] is a *snapshot* of the factor's configured weight at the time it was logged,
 * not a live lookup — this is what keeps historical days stable when a factor's weight
 * is edited later. Callers (the repository layer) are responsible for capturing that
 * snapshot; this engine never re-reads a factor's current configuration.
 */
data class FactorSelection(
    val factorId: Long,
    val category: FactorCategory,
    val weight: Double,
    val intensityMultiplier: Double
) {
    init {
        require(category != FactorCategory.SYMPTOM) {
            "Symptom factors do not contribute points and should not be passed to the scoring engine"
        }
        require(weight >= 0.0) { "weight must be non-negative" }
        require(intensityMultiplier in 0.0..1.0) { "intensityMultiplier must be within 0.0..1.0" }
    }

    val points: Double get() = weight * intensityMultiplier
}

/**
 * The fully resolved result of scoring a single calendar day.
 */
data class DailyLoad(
    val date: LocalDate,
    val grossLoadPoints: Double,
    val recoveryPointsRaw: Double,
    val appliedRecoveryPoints: Double,
    val netLoadPoints: Double,
    val maxPossibleDailyLoad: Double,
    val normalizedLoadPercent: Double,
    val calculationVersion: Int
)

/**
 * The result of averaging load across the last three calendar days. This is deliberately
 * not just a Double — a rolling load is meaningless (and must not be silently treated as
 * zero) until three consecutive days of real data exist.
 */
sealed class RollingLoad {
    data class Established(
        val meanLoadPoints: Double,
        val meanLoadPercent: Double,
        val daysIncluded: Int
    ) : RollingLoad()

    data class Building(
        val daysLogged: Int,
        val daysNeeded: Int = REQUIRED_WINDOW_DAYS
    ) : RollingLoad()
}

/** 0-49 / 50-74 / 75+ absolute gauge bands, independent of the user's personal target. */
enum class LoadTier {
    GREEN,
    YELLOW,
    RED
}
