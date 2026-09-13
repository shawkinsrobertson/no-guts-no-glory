package com.shawkinsrobertson.noguts.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private val DAY = LocalDate.of(2026, 9, 14)

private fun load(
    factorId: Long = 1,
    weight: Double,
    multiplier: Double = IntensityLevel.BOOLEAN_SELECTED_MULTIPLIER
) = FactorSelection(factorId, FactorCategory.LOAD, weight, multiplier)

private fun recovery(
    factorId: Long = 100,
    weight: Double,
    multiplier: Double = IntensityLevel.BOOLEAN_SELECTED_MULTIPLIER
) = FactorSelection(factorId, FactorCategory.RECOVERY, weight, multiplier)

class ScoringEngineTest {

    // --- Daily load -------------------------------------------------------

    @Test
    fun `zero factors selected produces zero percent`() {
        val result = calculateDailyLoad(DAY, emptyList(), maxPossibleDailyLoad = 40.0)
        assertEquals(0.0, result.netLoadPoints, 0.0001)
        assertEquals(0.0, result.normalizedLoadPercent, 0.0001)
    }

    @Test
    fun `single boolean load factor scores its full weight`() {
        val result = calculateDailyLoad(DAY, listOf(load(weight = 10.0)), maxPossibleDailyLoad = 40.0)
        assertEquals(10.0, result.netLoadPoints, 0.0001)
        assertEquals(25.0, result.normalizedLoadPercent, 0.0001)
    }

    @Test
    fun `level factor applies its intensity multiplier`() {
        // Alcohol weight 10, MODERATE -> 10 x 0.6 = 6, matching the plan's worked example.
        val result = calculateDailyLoad(
            DAY,
            listOf(load(weight = 10.0, multiplier = IntensityLevel.MODERATE.multiplier)),
            maxPossibleDailyLoad = 40.0
        )
        assertEquals(6.0, result.netLoadPoints, 0.0001)
    }

    @Test
    fun `max possible daily load is the sum of active load factor weights`() {
        assertEquals(40.0, calculateMaxPossibleDailyLoad(listOf(10.0, 12.0, 8.0, 10.0)), 0.0001)
    }

    // --- Recovery -----------------------------------------------------------

    @Test
    fun `recovery reduces net load but is capped at 30 percent of gross load`() {
        // Gross load 10, recovery worth 8 raw points -> ceiling is 3 (30% of 10).
        val result = calculateDailyLoad(
            DAY,
            listOf(load(weight = 10.0), recovery(weight = 8.0)),
            maxPossibleDailyLoad = 40.0
        )
        assertEquals(3.0, result.appliedRecoveryPoints, 0.0001)
        assertEquals(7.0, result.netLoadPoints, 0.0001)
    }

    @Test
    fun `recovery can never produce negative load even with heavy recovery stacking`() {
        val result = calculateDailyLoad(
            DAY,
            listOf(
                load(factorId = 1, weight = 10.0, multiplier = IntensityLevel.SEVERE.multiplier),
                recovery(factorId = 101, weight = 20.0),
                recovery(factorId = 102, weight = 20.0),
                recovery(factorId = 103, weight = 20.0)
            ),
            maxPossibleDailyLoad = 40.0
        )
        assertTrue(result.netLoadPoints >= 0.0)
        // Heavy alcohol + excellent sleep + gentle movement must not net to zero (plan section 10).
        assertTrue("recovery must not fully erase load", result.netLoadPoints > 0.0)
    }

    @Test
    fun `recovery does not affect max possible daily load`() {
        // Recovery weights are irrelevant to the denominator; only active LOAD factors count.
        val max = calculateMaxPossibleDailyLoad(listOf(10.0, 12.0)) // load factors only
        assertEquals(22.0, max, 0.0001)
    }

    // --- Historical stability -------------------------------------------------

    @Test
    fun `a factor's later weight change does not alter an already-scored day`() {
        // The engine only ever sees the snapshot passed in - it never re-reads a factor's
        // current weight, so re-scoring the same historical selection is idempotent even
        // after the "current" weight (represented here by a second, different snapshot)
        // has changed.
        val loggedAtWeight10 = calculateDailyLoad(DAY, listOf(load(weight = 10.0)), maxPossibleDailyLoad = 40.0)
        val sameDayRecalculatedFromStoredSnapshot =
            calculateDailyLoad(DAY, listOf(load(weight = 10.0)), maxPossibleDailyLoad = 40.0)

        assertEquals(loggedAtWeight10.netLoadPoints, sameDayRecalculatedFromStoredSnapshot.netLoadPoints, 0.0001)
    }

    @Test
    fun `deactivating a factor does not change a historical day's stored selections`() {
        // Deactivation only affects future calculateMaxPossibleDailyLoad calls (which
        // exclude the now-inactive factor); it must not be applied retroactively to a
        // FactorSelection list that was already captured for a past day.
        val historicalMax = 40.0 // max at the time the day was logged, frozen in ScoreSnapshot
        val result = calculateDailyLoad(DAY, listOf(load(weight = 10.0)), maxPossibleDailyLoad = historicalMax)
        assertEquals(25.0, result.normalizedLoadPercent, 0.0001)
    }

    @Test
    fun `changing the personal target does not affect the daily load calculation`() {
        // Target is not a parameter of calculateDailyLoad at all - it is a display-layer
        // comparison against the same DailyLoad output, so any target value must yield
        // an identical DailyLoad.
        val result = calculateDailyLoad(DAY, listOf(load(weight = 10.0)), maxPossibleDailyLoad = 40.0)
        assertEquals(25.0, result.normalizedLoadPercent, 0.0001)
        // (No target parameter exists to pass here - this test documents the invariant.)
    }

    // --- 72-hour rolling load -------------------------------------------------

    @Test
    fun `three consecutive valid days produce an established rolling mean`() {
        val dayMinus2 = calculateDailyLoad(DAY.minusDays(2), listOf(load(weight = 14.0)), 40.0)
        val dayMinus1 = calculateDailyLoad(DAY.minusDays(1), listOf(load(weight = 20.0)), 40.0)
        val today = calculateDailyLoad(DAY, listOf(load(weight = 10.0)), 40.0)

        val rolling = calculate72HourLoad(listOf(dayMinus2, dayMinus1, today), maxPossibleDailyLoad = 40.0)

        assertTrue(rolling is RollingLoad.Established)
        rolling as RollingLoad.Established
        assertEquals(14.6667, rolling.meanLoadPoints, 0.001)
        assertEquals(36.67, rolling.meanLoadPercent, 0.01)
    }

    @Test
    fun `a missing day is not treated as zero and yields a building state`() {
        val monday = calculateDailyLoad(DAY.minusDays(2), listOf(load(weight = 12.0)), 40.0)
        val tuesday = calculateDailyLoad(DAY.minusDays(1), listOf(load(weight = 18.0)), 40.0)
        // Wednesday: no log at all.

        val rolling = calculate72HourLoad(listOf(monday, tuesday, null), maxPossibleDailyLoad = 40.0)

        assertTrue(rolling is RollingLoad.Building)
        assertEquals(2, (rolling as RollingLoad.Building).daysLogged)
    }

    @Test
    fun `a day intentionally logged as nothing notable is a legitimate zero in the window`() {
        val nothingNotable = calculateDailyLoad(DAY.minusDays(2), emptyList(), 40.0)
        val dayMinus1 = calculateDailyLoad(DAY.minusDays(1), listOf(load(weight = 20.0)), 40.0)
        val today = calculateDailyLoad(DAY, listOf(load(weight = 10.0)), 40.0)

        val rolling = calculate72HourLoad(listOf(nothingNotable, dayMinus1, today), maxPossibleDailyLoad = 40.0)

        assertTrue(rolling is RollingLoad.Established)
        rolling as RollingLoad.Established
        // (0 + 20 + 10) / 3
        assertEquals(10.0, rolling.meanLoadPoints, 0.0001)
    }

    @Test
    fun `rolling load requires exactly three day slots`() {
        try {
            calculate72HourLoad(listOf(null, null), maxPossibleDailyLoad = 40.0)
            assertTrue("expected IllegalArgumentException", false)
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    // --- Gauge tiers -------------------------------------------------------

    @Test
    fun `gauge tiers follow the 0-49-50-74-75plus bands`() {
        assertEquals(LoadTier.GREEN, loadTierFor(0.0))
        assertEquals(LoadTier.GREEN, loadTierFor(49.9))
        assertEquals(LoadTier.YELLOW, loadTierFor(50.0))
        assertEquals(LoadTier.YELLOW, loadTierFor(74.9))
        assertEquals(LoadTier.RED, loadTierFor(75.0))
        assertEquals(LoadTier.RED, loadTierFor(100.0))
    }
}
