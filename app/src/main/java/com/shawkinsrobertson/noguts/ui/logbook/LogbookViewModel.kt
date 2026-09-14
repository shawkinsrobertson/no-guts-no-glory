package com.shawkinsrobertson.noguts.ui.logbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shawkinsrobertson.noguts.data.db.entity.ScoreSnapshotEntity
import com.shawkinsrobertson.noguts.data.repository.DailyLogRepository
import com.shawkinsrobertson.noguts.data.repository.LoggedDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val LOGBOOK_PAGE_SIZE = 60

data class LogbookEntry(val loggedDay: LoggedDay, val snapshot: ScoreSnapshotEntity?)

class LogbookViewModel(dailyLogRepository: DailyLogRepository) : ViewModel() {

    private val _entries = MutableStateFlow<List<LogbookEntry>>(emptyList())
    val entries: StateFlow<List<LogbookEntry>> = _entries.asStateFlow()

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
    }
}
