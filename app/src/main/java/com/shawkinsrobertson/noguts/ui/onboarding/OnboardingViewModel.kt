package com.shawkinsrobertson.noguts.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import com.shawkinsrobertson.noguts.data.datastore.UserPreferencesRepository
import com.shawkinsrobertson.noguts.data.repository.FactorRepository
import com.shawkinsrobertson.noguts.scoring.FactorCategory
import com.shawkinsrobertson.noguts.scoring.InputType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

const val ONBOARDING_STEP_COUNT = 6

/** Factors with sortOrder below this are considered "common" and preselected (plan section 31). */
private const val COMMON_FACTOR_SORT_ORDER_CEILING = 10

data class OnboardingUiState(
    val step: Int = 0,
    val loading: Boolean = true,
    val trackableFactors: List<FactorEntity> = emptyList(),
    val selectedFactorIds: Set<Long> = emptySet(),
    val customFactorDraftName: String = "",
    val weights: Map<Long, Int> = emptyMap(),
    val suggestedTargetPercent: Double = 50.0,
    val chosenTargetPercent: Double = 50.0,
    val useCustomTarget: Boolean = false,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val isSaving: Boolean = false,
    val finished: Boolean = false
) {
    val selectedFactors: List<FactorEntity> get() = trackableFactors.filter { it.id in selectedFactorIds }

    /** Symptoms are tracked but never carry a weight (they don't contribute to the load
     * score), so the weight step only asks about Stressors/Recovery. */
    val selectedWeighableFactors: List<FactorEntity>
        get() = selectedFactors.filter { it.category != FactorCategory.SYMPTOM }

    /** The max possible daily load implied by what's selected so far, using each
     * factor's in-progress weight from [weights] rather than its stored default - the
     * scale the target percent (last step) is shown against as points. */
    val maxPossibleDailyLoad: Double
        get() = selectedFactors
            .filter { it.category == FactorCategory.LOAD }
            .sumOf { (weights[it.id] ?: it.weight.toInt()).toDouble() }
}

class OnboardingViewModel(
    private val factorRepository: FactorRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            factorRepository.seedDefaultsIfEmpty()
            // Every category is now trackable from onboarding (Stressors/Recovery/Symptoms
            // each get their own selection step) - see finish() for why a symptom's weight
            // is never touched even though this associates one into [weights] below.
            val trackable = factorRepository.allFactors.first()
            val preselected = trackable
                .filter { it.sortOrder < COMMON_FACTOR_SORT_ORDER_CEILING }
                .map { it.id }
                .toSet()
            _uiState.value = _uiState.value.copy(
                loading = false,
                trackableFactors = trackable,
                selectedFactorIds = preselected,
                weights = trackable.associate { it.id to it.weight.toInt().coerceIn(1, 10) }
            )
        }
    }

    fun goToStep(step: Int) {
        _uiState.value = _uiState.value.copy(step = step.coerceIn(0, ONBOARDING_STEP_COUNT - 1))
    }

    fun nextStep() = goToStep(_uiState.value.step + 1)
    fun previousStep() = goToStep(_uiState.value.step - 1)

    fun toggleFactor(factorId: Long) {
        val current = _uiState.value.selectedFactorIds
        _uiState.value = _uiState.value.copy(
            selectedFactorIds = if (factorId in current) current - factorId else current + factorId
        )
    }

    fun updateCustomFactorDraftName(name: String) {
        _uiState.value = _uiState.value.copy(customFactorDraftName = name)
    }

    fun addCustomFactor(category: FactorCategory) {
        val name = _uiState.value.customFactorDraftName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            // Matches Settings' AddFactorSection: a symptom is a level input with no
            // weight (it's tracked, not scored); everything else is a boolean at a
            // reasonable starting weight the user can adjust in the weight step.
            val (inputType, weight) = if (category == FactorCategory.SYMPTOM) {
                InputType.LEVEL to 0.0
            } else {
                InputType.BOOLEAN to 5.0
            }
            val id = factorRepository.addCustomFactor(name, category, inputType, weight)
            val updatedFactors = factorRepository.allFactors.first()
            _uiState.value = _uiState.value.copy(
                trackableFactors = updatedFactors,
                selectedFactorIds = _uiState.value.selectedFactorIds + id,
                weights = _uiState.value.weights + (id to weight.toInt().coerceIn(1, 10)),
                customFactorDraftName = ""
            )
        }
    }

    fun updateWeight(factorId: Long, weight: Int) {
        _uiState.value = _uiState.value.copy(weights = _uiState.value.weights + (factorId to weight.coerceIn(1, 10)))
    }

    fun chooseSuggestedTarget() {
        _uiState.value = _uiState.value.copy(
            useCustomTarget = false,
            chosenTargetPercent = _uiState.value.suggestedTargetPercent
        )
    }

    fun chooseCustomTarget(percent: Double) {
        _uiState.value = _uiState.value.copy(useCustomTarget = true, chosenTargetPercent = percent)
    }

    fun setReminderEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(reminderEnabled = enabled)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(reminderHour = hour, reminderMinute = minute)
    }

    /** Persists every onboarding choice. Reminder alarm scheduling is the caller's job
     * (it needs a Context, which this ViewModel deliberately doesn't hold). */
    fun finish() {
        val state = _uiState.value
        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            for (factor in state.trackableFactors) {
                val isSelected = factor.id in state.selectedFactorIds
                // A symptom's weight is never touched here - it never went through the
                // weight step (see selectedWeighableFactors), so state.weights holds only
                // a meaningless placeholder for it, not a real user choice.
                val weight = when {
                    factor.category == FactorCategory.SYMPTOM -> factor.weight
                    isSelected -> state.weights[factor.id]?.toDouble() ?: factor.weight
                    else -> factor.weight
                }
                if (factor.active != isSelected || weight != factor.weight) {
                    factorRepository.updateFactor(factor.copy(active = isSelected, weight = weight))
                }
            }
            userPreferencesRepository.updateTargetPercent(state.chosenTargetPercent)
            userPreferencesRepository.updateReminder(state.reminderEnabled, state.reminderHour, state.reminderMinute)
            userPreferencesRepository.setOnboardingComplete(true)
            _uiState.value = _uiState.value.copy(isSaving = false, finished = true)
        }
    }
}
