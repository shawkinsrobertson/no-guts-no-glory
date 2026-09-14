package com.shawkinsrobertson.noguts.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shawkinsrobertson.noguts.data.datastore.UserPreferencesRepository
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
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
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val loading: Boolean = true,
    val greetingName: String = "",
    val latestSnapshot: ScoreSnapshotEntity? = null,
    /** Only non-null once today itself has a saved snapshot - a stale prior day's
     * percentage is never shown as "today's load". */
    val todaysLoadPercent: Double? = null,
    val stressorFactors: List<FactorEntity> = emptyList(),
    val recoveryFactors: List<FactorEntity> = emptyList(),
    val symptomFactors: List<FactorEntity> = emptyList(),
    val selections: Map<Long, FactorSelectionState> = emptyMap(),
    val notes: String = "",
    val todayAlreadyLogged: Boolean = false,
    val isEditingToday: Boolean = true,
    val isSaving: Boolean = false,
    val justSaved: Boolean = false
)

class DashboardViewModel(
    private val dailyLogRepository: DailyLogRepository,
    private val factorRepository: FactorRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        combine(
            factorRepository.activeFactors,
            dailyLogRepository.observeLogForDate(today),
            dailyLogRepository.observeLatestSnapshot(),
            userPreferencesRepository.profile
        ) { activeFactors, todayLog, latestSnapshot, profile ->
            val byCategory = activeFactors.groupBy { it.category }
            val current = _uiState.value

            DashboardUiState(
                loading = false,
                greetingName = profile.name,
                latestSnapshot = latestSnapshot,
                todaysLoadPercent = latestSnapshot?.takeIf { it.date == today }?.normalizedDailyPercent,
                stressorFactors = byCategory[FactorCategory.LOAD].orEmpty().sortedBy { it.sortOrder },
                recoveryFactors = byCategory[FactorCategory.RECOVERY].orEmpty().sortedBy { it.sortOrder },
                symptomFactors = byCategory[FactorCategory.SYMPTOM].orEmpty().sortedBy { it.sortOrder },
                selections = if (current.loading || !current.isEditingToday) selectionsFrom(todayLog) else current.selections,
                notes = if (current.loading) todayLog?.notes ?: "" else current.notes,
                todayAlreadyLogged = todayLog != null,
                isEditingToday = current.loading || todayLog == null || current.isEditingToday,
                isSaving = false,
                justSaved = current.justSaved
            )
        }.onEach { _uiState.value = it }.launchIn(viewModelScope)
    }

    private fun selectionsFrom(loggedDay: LoggedDay?): Map<Long, FactorSelectionState> {
        if (loggedDay == null) return emptyMap()
        return loggedDay.factors.associate { detail ->
            detail.factor.id to (
                detail.level?.let { FactorSelectionState.LevelSelected(it) } ?: FactorSelectionState.BooleanSelected
                )
        }
    }

    fun toggleBooleanFactor(factorId: Long) {
        updateSelection(factorId) { (it ?: FactorSelectionState.NotSelected).toggledForBooleanFactor() }
    }

    fun cycleLevelFactor(factorId: Long) {
        updateSelection(factorId) { (it ?: FactorSelectionState.NotSelected).cycledForLevelFactor() }
    }

    private fun updateSelection(factorId: Long, transform: (FactorSelectionState?) -> FactorSelectionState) {
        val current = _uiState.value.selections
        _uiState.value = _uiState.value.copy(
            selections = current + (factorId to transform(current[factorId])),
            justSaved = false
        )
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes, justSaved = false)
    }

    fun startEditingToday() {
        _uiState.value = _uiState.value.copy(isEditingToday = true, justSaved = false)
    }

    fun saveToday() {
        val state = _uiState.value
        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            dailyLogRepository.saveDailyLog(
                today,
                state.selections.toFactorLogInputs(),
                notes = state.notes.ifBlank { null }
            )
            _uiState.value = _uiState.value.copy(isSaving = false, isEditingToday = false, justSaved = true)
        }
    }

    /** One-tap "nothing notable" (plan section 21) - clears any in-progress selections too. */
    fun logNothingNotable() {
        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            dailyLogRepository.saveZeroDay(today)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                isEditingToday = false,
                justSaved = true,
                selections = emptyMap(),
                notes = ""
            )
        }
    }
}
