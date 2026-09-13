package com.shawkinsrobertson.noguts.ui.dashboard

import com.shawkinsrobertson.noguts.data.repository.FactorLogInput
import com.shawkinsrobertson.noguts.scoring.IntensityLevel

/** The check-in UI's per-factor selection state, before it's turned into a save request. */
sealed class FactorSelectionState {
    data object NotSelected : FactorSelectionState()
    data object BooleanSelected : FactorSelectionState()
    data class LevelSelected(val level: IntensityLevel) : FactorSelectionState()
}

/** Tapping a card cycles it: unselected -> mild -> moderate -> severe -> unselected. */
fun FactorSelectionState.cycledForLevelFactor(): FactorSelectionState = when (this) {
    is FactorSelectionState.NotSelected -> FactorSelectionState.LevelSelected(IntensityLevel.MILD)
    is FactorSelectionState.LevelSelected -> when (level) {
        IntensityLevel.MILD -> FactorSelectionState.LevelSelected(IntensityLevel.MODERATE)
        IntensityLevel.MODERATE -> FactorSelectionState.LevelSelected(IntensityLevel.SEVERE)
        else -> FactorSelectionState.NotSelected
    }
    is FactorSelectionState.BooleanSelected -> FactorSelectionState.NotSelected // shouldn't happen; defensive
}

fun FactorSelectionState.toggledForBooleanFactor(): FactorSelectionState = when (this) {
    is FactorSelectionState.NotSelected -> FactorSelectionState.BooleanSelected
    else -> FactorSelectionState.NotSelected
}

fun Map<Long, FactorSelectionState>.toFactorLogInputs(): List<FactorLogInput> =
    mapNotNull { (factorId, state) ->
        when (state) {
            is FactorSelectionState.NotSelected -> null
            is FactorSelectionState.BooleanSelected -> FactorLogInput(factorId, level = null)
            is FactorSelectionState.LevelSelected -> FactorLogInput(factorId, level = state.level)
        }
    }
