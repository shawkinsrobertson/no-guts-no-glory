package com.shawkinsrobertson.noguts.data.repository

import com.shawkinsrobertson.noguts.data.datastore.UserPreferencesRepository
import com.shawkinsrobertson.noguts.data.db.dao.DailyLogDao
import com.shawkinsrobertson.noguts.data.db.dao.DailyLogFactorDao
import com.shawkinsrobertson.noguts.data.db.dao.DailyLogWithFactorEntities
import com.shawkinsrobertson.noguts.data.db.dao.FactorSelectionStat
import com.shawkinsrobertson.noguts.data.db.dao.ScoreSnapshotDao
import com.shawkinsrobertson.noguts.data.db.entity.DailyLogEntity
import com.shawkinsrobertson.noguts.data.db.entity.DailyLogFactorEntity
import com.shawkinsrobertson.noguts.data.db.entity.DailyLogStatus
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import com.shawkinsrobertson.noguts.data.db.entity.ScoreSnapshotEntity
import com.shawkinsrobertson.noguts.scoring.DailyLoad
import com.shawkinsrobertson.noguts.scoring.FactorCategory
import com.shawkinsrobertson.noguts.scoring.FactorSelection
import com.shawkinsrobertson.noguts.scoring.IntensityLevel
import com.shawkinsrobertson.noguts.scoring.RollingLoad
import com.shawkinsrobertson.noguts.scoring.calculate72HourLoad
import com.shawkinsrobertson.noguts.scoring.calculateDailyLoad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate

/**
 * The seam between the UI, Room, and the pure scoring engine. Every write here recomputes
 * and persists that day's [ScoreSnapshotEntity] immediately - historical snapshots are
 * never silently recalculated later just because a factor's weight, active state, or the
 * personal target changed (see ScoreSnapshotEntity's kdoc).
 *
 * V1 scope note: only the current day's log is expected to be edited (from the Dashboard
 * or the reminder notification). The Logbook is a read-only history view, so
 * [saveDailyLog] does not cascade a recompute forward through later days' snapshots.
 */
class DailyLogRepository(
    private val dailyLogDao: DailyLogDao,
    private val dailyLogFactorDao: DailyLogFactorDao,
    private val scoreSnapshotDao: ScoreSnapshotDao,
    private val factorRepository: FactorRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    fun observeLogForDate(date: LocalDate): Flow<LoggedDay?> =
        combine(dailyLogDao.observeWithFactorsByDate(date), factorRepository.allFactors) { withFactors, factors ->
            withFactors?.toLoggedDay(factors)
        }

    fun observeRecentLogs(limit: Int): Flow<List<LoggedDay>> =
        combine(dailyLogDao.observeRecentWithFactors(limit), factorRepository.allFactors) { logs, factors ->
            logs.map { it.toLoggedDay(factors) }
        }

    fun observeRecentSnapshots(limit: Int): Flow<List<ScoreSnapshotEntity>> =
        scoreSnapshotDao.observeRecent(limit)

    /** One-shot reads for CSV export - there's no need to stay subscribed for that. */
    suspend fun getAllLoggedDaysOnce(): List<LoggedDay> = observeRecentLogs(Int.MAX_VALUE).first()

    suspend fun getAllSnapshotsOnce(): List<ScoreSnapshotEntity> = observeRecentSnapshots(Int.MAX_VALUE).first()

    fun observeLatestSnapshot(): Flow<ScoreSnapshotEntity?> = scoreSnapshotDao.observeLatest()

    fun observeSnapshotForDate(date: LocalDate): Flow<ScoreSnapshotEntity?> = scoreSnapshotDao.observeByDate(date)

    suspend fun getSelectionStatsSince(date: LocalDate): List<FactorSelectionStat> =
        dailyLogFactorDao.getSelectionStatsSince(date)

    suspend fun getSelectionStatsBetween(start: LocalDate, end: LocalDate): List<FactorSelectionStat> =
        dailyLogFactorDao.getSelectionStatsBetween(start, end)

    suspend fun countLoggedBetween(start: LocalDate, end: LocalDate): Int =
        dailyLogDao.countLoggedBetween(start, end)

    /** One-shot range reads for the PDF report - see [getAllLoggedDaysOnce]/[getAllSnapshotsOnce]
     * for the same idea over the full history instead of a bounded range. */
    suspend fun getLoggedDaysBetween(start: LocalDate, end: LocalDate): List<LoggedDay> {
        val factors = factorRepository.allFactors.first()
        return dailyLogDao.getBetweenWithFactors(start, end).map { it.toLoggedDay(factors) }
    }

    suspend fun getSnapshotsBetween(start: LocalDate, end: LocalDate): List<ScoreSnapshotEntity> =
        scoreSnapshotDao.getBetween(start, end)

    /** Saves (or overwrites) [date]'s log, then recalculates and stores its score snapshot. */
    suspend fun saveDailyLog(date: LocalDate, inputs: List<FactorLogInput>, notes: String?) {
        val now = Instant.now()
        val existing = dailyLogDao.getByDate(date)
        val dailyLogId = if (existing != null) {
            dailyLogDao.update(existing.copy(status = DailyLogStatus.LOGGED, notes = notes, updatedAt = now))
            dailyLogFactorDao.deleteForLog(existing.id)
            existing.id
        } else {
            dailyLogDao.insert(
                DailyLogEntity(
                    date = date,
                    status = DailyLogStatus.LOGGED,
                    notes = notes,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        val factorsById = factorRepository.getByIds(inputs.map { it.factorId }).associateBy { it.id }
        val logFactorEntities = inputs.mapNotNull { input ->
            factorsById[input.factorId]?.let { factor -> input.toEntity(dailyLogId, factor) }
        }
        if (logFactorEntities.isNotEmpty()) {
            dailyLogFactorDao.insertAll(logFactorEntities)
        }

        recalculateAndStoreSnapshot(date, logFactorEntities, factorsById)
    }

    /** Convenience for the "Nothing notable" one-tap zero day (plan section 21). */
    suspend fun saveZeroDay(date: LocalDate) = saveDailyLog(date, emptyList(), notes = null)

    private suspend fun recalculateAndStoreSnapshot(
        date: LocalDate,
        logFactors: List<DailyLogFactorEntity>,
        factorsById: Map<Long, FactorEntity>
    ) {
        val maxPossibleDailyLoad = factorRepository.currentMaxPossibleDailyLoad()
        val selections = logFactors.mapNotNull { it.toFactorSelectionOrNull(factorsById) }
        val todayLoad = calculateDailyLoad(date, selections, maxPossibleDailyLoad)

        val dayMinus1 = scoreSnapshotDao.getByDate(date.minusDays(1))?.toDailyLoad()
        val dayMinus2 = scoreSnapshotDao.getByDate(date.minusDays(2))?.toDailyLoad()
        val rolling = calculate72HourLoad(listOf(dayMinus2, dayMinus1, todayLoad), maxPossibleDailyLoad)

        val targetPercent = userPreferencesRepository.profile.first().targetPercent

        scoreSnapshotDao.upsert(
            ScoreSnapshotEntity(
                date = date,
                dailyLoad = todayLoad.netLoadPoints,
                normalizedDailyPercent = todayLoad.normalizedLoadPercent,
                rolling72Load = (rolling as? RollingLoad.Established)?.meanLoadPoints,
                rolling72Percent = (rolling as? RollingLoad.Established)?.meanLoadPercent,
                daysInRollingWindow = when (rolling) {
                    is RollingLoad.Established -> rolling.daysIncluded
                    is RollingLoad.Building -> rolling.daysLogged
                },
                targetPercent = targetPercent,
                calculationVersion = todayLoad.calculationVersion,
                createdAt = Instant.now()
            )
        )
    }

    private fun DailyLogFactorEntity.toFactorSelectionOrNull(factorsById: Map<Long, FactorEntity>): FactorSelection? {
        // The scoring engine only ever sees LOAD/RECOVERY selections - symptoms are
        // tracked but never contribute points.
        val category = factorsById[factorId]?.category ?: return null
        if (category == FactorCategory.SYMPTOM) return null
        val multiplier = level?.multiplier ?: IntensityLevel.BOOLEAN_SELECTED_MULTIPLIER
        return FactorSelection(factorId, category, weightSnapshot, multiplier)
    }

    private fun FactorLogInput.toEntity(dailyLogId: Long, factor: FactorEntity): DailyLogFactorEntity {
        val multiplier = level?.multiplier ?: IntensityLevel.BOOLEAN_SELECTED_MULTIPLIER
        return DailyLogFactorEntity(
            dailyLogId = dailyLogId,
            factorId = factor.id,
            level = level,
            weightSnapshot = factor.weight,
            calculatedPoints = factor.weight * multiplier
        )
    }

    /**
     * Reconstructs just enough of a [DailyLoad] from a stored snapshot to feed
     * [calculate72HourLoad] - only [DailyLoad.netLoadPoints] is actually read by that
     * function; the rest of these fields are unused placeholders.
     */
    private fun ScoreSnapshotEntity.toDailyLoad(): DailyLoad = DailyLoad(
        date = date,
        grossLoadPoints = dailyLoad,
        recoveryPointsRaw = 0.0,
        appliedRecoveryPoints = 0.0,
        netLoadPoints = dailyLoad,
        maxPossibleDailyLoad = 0.0,
        normalizedLoadPercent = normalizedDailyPercent,
        calculationVersion = calculationVersion
    )

    private fun DailyLogWithFactorEntities.toLoggedDay(allFactors: List<FactorEntity>): LoggedDay {
        val factorsById = allFactors.associateBy { it.id }
        return LoggedDay(
            date = log.date,
            status = log.status,
            notes = log.notes,
            factors = factors.mapNotNull { logFactor ->
                factorsById[logFactor.factorId]?.let { factor ->
                    LoggedFactorDetail(factor, logFactor.level, logFactor.calculatedPoints)
                }
            }
        )
    }
}
