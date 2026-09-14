package com.shawkinsrobertson.noguts.ui.components

import com.shawkinsrobertson.noguts.scoring.FactorCategory

/** The user-facing category names, used everywhere factors are grouped (Dashboard,
 * Settings): "Stressors" reads more plainly than the internal LOAD label. */
fun FactorCategory.displayName(): String = when (this) {
    FactorCategory.LOAD -> "Stressors"
    FactorCategory.RECOVERY -> "Recovery"
    FactorCategory.SYMPTOM -> "Symptoms"
}
