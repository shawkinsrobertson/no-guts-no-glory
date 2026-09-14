package com.shawkinsrobertson.noguts.scoring

/**
 * Pure Kotlin scoring engine for Julie's stomach load tracking.
 *
 * This file has no dependency on Android, Room, or any other framework, and no
 * dependency on the current wall-clock time or database state. Every function here is
 * a deterministic transformation of its inputs. That is a deliberate constraint, not an
 * accident: the app's data layer is responsible for gathering inputs (today's date,
 * which factors are currently active, what a user selected) and persisting outputs —
 * this file only does the math, and it does the same math forever for the same inputs
 * unless [CURRENT_CALCULATION_VERSION] changes.
 */

/** The current version of the scoring algorithm. Bump this whenever the math changes. */
const val CURRENT_CALCULATION_VERSION = 1

/** Number of consecutive calendar days required before a rolling load is meaningful. */
const val REQUIRED_WINDOW_DAYS = 3

/**
 * The default ceiling on how much of a day's gross load recovery factors may offset,
 * expressed as a fraction of that day's gross load. This is a behavioral-model
 * parameter, not a physiological one — see the V1 plan for rationale. It prevents a
 * single excellent recovery day from erasing a day of real load.
 */
const val DEFAULT_RECOVERY_CAP_FRACTION = 0.30

/**
 * Sums the maximum possible contribution of every currently active LOAD factor.
 * Recovery factors never affect this denominator: if they did, adding more recovery
 * options to track would artificially shrink the normalized load percentage.
 */
fun calculateMaxPossibleDailyLoad(activeLoadFactorWeights: List<Double>): Double {
    require(activeLoadFactorWeights.all { it >= 0.0 }) { "factor weights must be non-negative" }
    return activeLoadFactorWeights.sum()
}

/**
 * Scores a single calendar day from the factors selected for it.
 *
 * Net load is gross load minus applied recovery, floored at zero — recovery can never
 * push a day negative, and (via [recoveryCapFraction]) can never fully erase a day's
 * load either.
 */
fun calculateDailyLoad(
    date: java.time.LocalDate,
    selectedFactors: List<FactorSelection>,
    maxPossibleDailyLoad: Double,
    recoveryCapFraction: Double = DEFAULT_RECOVERY_CAP_FRACTION,
    calculationVersion: Int = CURRENT_CALCULATION_VERSION
): DailyLoad {
    require(maxPossibleDailyLoad >= 0.0) { "maxPossibleDailyLoad must be non-negative" }
    require(recoveryCapFraction in 0.0..1.0) { "recoveryCapFraction must be within 0.0..1.0" }

    val grossLoadPoints = selectedFactors
        .filter { it.category == FactorCategory.LOAD }
        .sumOf { it.points }

    val recoveryPointsRaw = selectedFactors
        .filter { it.category == FactorCategory.RECOVERY }
        .sumOf { it.points }

    val recoveryCeiling = grossLoadPoints * recoveryCapFraction
    val appliedRecoveryPoints = minOf(recoveryPointsRaw, recoveryCeiling)
    val netLoadPoints = maxOf(0.0, grossLoadPoints - appliedRecoveryPoints)

    val normalizedLoadPercent = if (maxPossibleDailyLoad <= 0.0) {
        0.0
    } else {
        (netLoadPoints / maxPossibleDailyLoad * 100.0).coerceIn(0.0, 100.0)
    }

    return DailyLoad(
        date = date,
        grossLoadPoints = grossLoadPoints,
        recoveryPointsRaw = recoveryPointsRaw,
        appliedRecoveryPoints = appliedRecoveryPoints,
        netLoadPoints = netLoadPoints,
        maxPossibleDailyLoad = maxPossibleDailyLoad,
        normalizedLoadPercent = normalizedLoadPercent,
        calculationVersion = calculationVersion
    )
}

/**
 * Averages load across the last three consecutive calendar days.
 *
 * [lastThreeDays] must contain exactly three entries ordered oldest to newest, ending on
 * the day the rolling load is being computed for. A `null` entry means "no log exists for
 * that date" — a genuinely unknown day — and is never treated as a zero. A day that was
 * actively logged as "nothing notable" produces a real [DailyLoad] with zero net points
 * and *does* count toward the window.
 *
 * The mean is normalized against [maxPossibleDailyLoad] as configured *now*, not as it
 * was on each historical day — the rolling percentage describes current load against the
 * current tracking configuration.
 */
fun calculate72HourLoad(
    lastThreeDays: List<DailyLoad?>,
    maxPossibleDailyLoad: Double
): RollingLoad {
    require(lastThreeDays.size == REQUIRED_WINDOW_DAYS) {
        "calculate72HourLoad requires exactly $REQUIRED_WINDOW_DAYS calendar days, got ${lastThreeDays.size}"
    }
    require(maxPossibleDailyLoad >= 0.0) { "maxPossibleDailyLoad must be non-negative" }

    val validDays = lastThreeDays.filterNotNull()
    if (validDays.size < REQUIRED_WINDOW_DAYS) {
        return RollingLoad.Building(daysLogged = validDays.size)
    }

    val meanLoadPoints = validDays.sumOf { it.netLoadPoints } / validDays.size
    val meanLoadPercent = if (maxPossibleDailyLoad <= 0.0) {
        0.0
    } else {
        (meanLoadPoints / maxPossibleDailyLoad * 100.0).coerceIn(0.0, 100.0)
    }

    return RollingLoad.Established(
        meanLoadPoints = meanLoadPoints,
        meanLoadPercent = meanLoadPercent,
        daysIncluded = REQUIRED_WINDOW_DAYS
    )
}

/**
 * The absolute gauge band for a normalized load percentage. This is independent of the
 * user's personal target — a load can be "above your target" while still reading GREEN.
 */
fun loadTierFor(normalizedLoadPercent: Double): LoadTier = when {
    normalizedLoadPercent < 50.0 -> LoadTier.GREEN
    normalizedLoadPercent < 75.0 -> LoadTier.YELLOW
    else -> LoadTier.RED
}
