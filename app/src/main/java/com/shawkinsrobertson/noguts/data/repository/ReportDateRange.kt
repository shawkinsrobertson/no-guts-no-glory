package com.shawkinsrobertson.noguts.data.repository

import java.time.LocalDate

/**
 * Preset ranges offered for the PDF report (plan follow-up: "the user should be able to
 * choose a longer date range" than the CSV's always-everything export). All ranges end
 * today; [ALL_TIME] is capped to a generous 10-year lookback rather than an unbounded
 * query, since a specific origin date isn't tracked anywhere.
 */
enum class ReportDateRange(val label: String) {
    LAST_7_DAYS("Last 7 days"),
    LAST_30_DAYS("Last 30 days"),
    LAST_90_DAYS("Last 90 days"),
    LAST_6_MONTHS("Last 6 months"),
    LAST_YEAR("Last year"),
    ALL_TIME("All time");

    fun startDate(today: LocalDate = LocalDate.now()): LocalDate = when (this) {
        LAST_7_DAYS -> today.minusDays(6)
        LAST_30_DAYS -> today.minusDays(29)
        LAST_90_DAYS -> today.minusDays(89)
        LAST_6_MONTHS -> today.minusMonths(6).plusDays(1)
        LAST_YEAR -> today.minusYears(1).plusDays(1)
        ALL_TIME -> today.minusYears(10)
    }
}
