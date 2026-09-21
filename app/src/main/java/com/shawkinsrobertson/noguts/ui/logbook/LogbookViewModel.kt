package com.shawkinsrobertson.noguts.ui.logbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shawkinsrobertson.noguts.data.db.entity.ScoreSnapshotEntity
import com.shawkinsrobertson.noguts.data.repository.DailyLogRepository
import com.shawkinsrobertson.noguts.data.repository.FactorRepository
import com.shawkinsrobertson.noguts.data.repository.LoggedDay
import com.shawkinsrobertson.noguts.scoring.FactorCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val LOGBOOK_PAGE_SIZE = 60

data class LogbookEntry(val loggedDay: LoggedDay, val snapshot: ScoreSnapshotEntity?)

class LogbookViewModel(
    dailyLogRepository: DailyLogRepository,
    factorRepository: FactorRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<LogbookEntry>>(emptyList())
    val entries: StateFlow<List<LogbookEntry>> = _entries.asStateFlow()

    /** Currently active LOAD factor weights, summed - the scale a historical day's stored
     * target percent is converted against for a points display (see DashboardViewModel's
     * same choice for why "current configuration" rather than that day's own max). */
    private val _maxPossibleDailyLoad = MutableStateFlow(0.0)
    val maxPossibleDailyLoad: StateFlow<Double> = _maxPossibleDailyLoad.asStateFlow()

    init {
        combine(
            dailyLogRepository.observeRecentLogs(LOGBOOK_PAGE_SIZE),
            dailyLogRepository.observeRecentSnapshots(LOGBOOK_PAGE_SIZE)
        ) { loggedDays, snapshots ->
            val snapshotsByDate = snapshots.associateBy { it.date }
            loggedDays
                .sortedByDescending { it.date }
                .map { LogbookEntry(it, snapshotsByDate[it.date]) }
        }.onEach { _entries.value = it }.launchIn(viewModelScope)

        factorRepository.activeFactors
            .onEach { factors ->
                _maxPossibleDailyLoad.value = factors
                    .filter { it.category == FactorCategory.LOAD }
                    .sumOf { it.weight }
            }
            .launchIn(viewModelScope)
    }
}
