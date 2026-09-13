package com.shawkinsrobertson.noguts.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shawkinsrobertson.noguts.data.repository.DailyLogRepository
import com.shawkinsrobertson.noguts.data.repository.FactorRepository
import com.shawkinsrobertson.noguts.scoring.FactorCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val TREND_POINT_COUNT = 7
private const val LOOKBACK_FOR_SNAPSHOTS = 30
private const val COMMON_FACTOR_WINDOW_DAYS = 7L

data class TrendPoint(val date: LocalDate, val rolling72Percent: Double, val targetPercent: Double)

data class MostCommonFactor(val name: String, val selectionCount: Int, val loggedDayCount: Int)

data class StatsUiState(
    val loading: Boolean = true,
    val trendPoints: List<TrendPoint> = emptyList(),
    val mostCommonFactor: MostCommonFactor? = null
)

class StatsViewModel(
    private val dailyLogRepository: DailyLogRepository,
    private val factorRepository: FactorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        dailyLogRepository.observeRecentSnapshots(LOOKBACK_FOR_SNAPSHOTS)
            .onEach { snapshots ->
                val points = snapshots
                    .filter { it.rolling72Percent != null }
                    .sortedBy { it.date }
                    .takeLast(TREND_POINT_COUNT)
                    .map { TrendPoint(it.date, it.rolling72Percent!!, it.targetPercent) }
                _uiState.value = _uiState.value.copy(loading = false, trendPoints = points)
            }
            .launchIn(viewModelScope)

        viewModelScope.launch { loadMostCommonFactor() }
    }

    private suspend fun loadMostCommonFactor() {
        val today = LocalDate.now()
        val since = today.minusDays(COMMON_FACTOR_WINDOW_DAYS - 1)

        val stats = dailyLogRepository.getSelectionStatsSince(since)
        val trackableFactors = factorRepository.getActiveFactors()
            .filter { it.category != FactorCategory.SYMPTOM }
            .associateBy { it.id }

        val top = stats.filter { it.factorId in trackableFactors.keys }.maxByOrNull { it.selectionCount }
        val factorName = top?.let { trackableFactors[it.factorId]?.name }
        if (top == null || factorName == null) return

        val loggedDayCount = dailyLogRepository.countLoggedBetween(since, today)
        _uiState.value = _uiState.value.copy(
            mostCommonFactor = MostCommonFactor(factorName, top.selectionCount, loggedDayCount)
        )
    }
}
