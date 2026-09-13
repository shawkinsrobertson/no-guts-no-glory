package com.shawkinsrobertson.noguts.data.db.dao

import java.time.LocalDate

/** How often and how recently a factor was selected, for the notification's personalized ranking. */
data class FactorSelectionStat(
    val factorId: Long,
    val selectionCount: Int,
    val lastSelectedDate: LocalDate?
)
