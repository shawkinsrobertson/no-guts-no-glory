package com.shawkinsrobertson.noguts.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shawkinsrobertson.noguts.data.datastore.ThemeMode
import com.shawkinsrobertson.noguts.data.datastore.UserPreferencesRepository
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import com.shawkinsrobertson.noguts.data.repository.CsvExportRepository
import com.shawkinsrobertson.noguts.data.repository.FactorRepository
import com.shawkinsrobertson.noguts.scoring.FactorCategory
import com.shawkinsrobertson.noguts.scoring.InputType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loading: Boolean = true,
    val name: String = "",
    val targetPercent: Double = 40.0,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val factors: List<FactorEntity> = emptyList(),
    val isExporting: Boolean = false,
    val pendingShareUris: List<Uri>? = null,
    val message: String? = null
)

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val factorRepository: FactorRepository,
    private val csvExportRepository: CsvExportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        combine(userPreferencesRepository.profile, factorRepository.allFactors) { profile, factors ->
            SettingsUiState(
                loading = false,
                name = profile.name,
                targetPercent = profile.targetPercent,
                reminderEnabled = profile.reminderEnabled,
                reminderHour = profile.reminderHour,
                reminderMinute = profile.reminderMinute,
                theme = profile.theme,
                factors = factors.sortedWith(compareBy({ it.category }, { it.sortOrder }, { it.name })),
                isExporting = _uiState.value.isExporting,
                pendingShareUris = _uiState.value.pendingShareUris,
                message = _uiState.value.message
            )
        }.onEach { _uiState.value = it }.launchIn(viewModelScope)
    }

    fun updateName(name: String) {
        viewModelScope.launch { userPreferencesRepository.updateName(name) }
    }

    fun updateTargetPercent(percent: Double) {
        viewModelScope.launch { userPreferencesRepository.updateTargetPercent(percent) }
    }

    /** Persists the reminder preference; scheduling the alarm itself is the caller's job
     * (it needs a Context, which this ViewModel deliberately doesn't hold). */
    fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch { userPreferencesRepository.updateReminder(enabled, hour, minute) }
    }

    fun updateTheme(theme: ThemeMode) {
        viewModelScope.launch { userPreferencesRepository.updateTheme(theme) }
    }

    fun toggleFactorActive(factor: FactorEntity) {
        viewModelScope.launch { factorRepository.setActive(factor, !factor.active) }
    }

    fun updateFactorWeight(factor: FactorEntity, weight: Double) {
        viewModelScope.launch { factorRepository.updateFactor(factor.copy(weight = weight)) }
    }

    fun deleteFactor(factor: FactorEntity) {
        viewModelScope.launch {
            val deleted = factorRepository.deleteFactor(factor)
            if (!deleted) {
                _uiState.value = _uiState.value.copy(
                    message = "${factor.name} has logged history, so it was turned off instead of deleted."
                )
            }
        }
    }

    fun messageShown() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun addFactor(name: String, category: FactorCategory, inputType: InputType, weight: Double) {
        if (name.isBlank()) return
        viewModelScope.launch { factorRepository.addCustomFactor(name, category, inputType, weight) }
    }

    fun exportCsv() {
        _uiState.value = _uiState.value.copy(isExporting = true)
        viewModelScope.launch {
            val uris = csvExportRepository.exportToShareableUris()
            _uiState.value = _uiState.value.copy(isExporting = false, pendingShareUris = uris)
        }
    }

    fun shareHandled() {
        _uiState.value = _uiState.value.copy(pendingShareUris = null)
    }
}
