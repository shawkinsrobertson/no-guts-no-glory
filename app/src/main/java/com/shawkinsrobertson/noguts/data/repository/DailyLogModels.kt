package com.shawkinsrobertson.noguts.data.repository

import com.shawkinsrobertson.noguts.data.db.entity.DailyLogStatus
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import com.shawkinsrobertson.noguts.scoring.IntensityLevel
import java.time.LocalDate

/** UI-facing input: what the user selected for one factor when submitting a day's log. */
data class FactorLogInput(
    val factorId: Long,
    /** Required (and not NONE) for LEVEL-input factors; ignored for BOOLEAN factors. */
    val level: IntensityLevel? = null
)

data class LoggedFactorDetail(
    val factor: FactorEntity,
    val level: IntensityLevel?,
    val calculatedPoints: Double
)

/** A fully resolved day, joined with the factor details needed to display it. */
data class LoggedDay(
    val date: LocalDate,
    val status: DailyLogStatus,
    val notes: String?,
    val factors: List<LoggedFactorDetail>
)
