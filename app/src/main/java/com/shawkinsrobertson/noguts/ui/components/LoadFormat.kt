package com.shawkinsrobertson.noguts.ui.components

import kotlin.math.roundToInt

/**
 * Formats a raw load score (points, not the normalized percentage) for display: whole
 * numbers show without a decimal, anything else shows one decimal place. Factor weights
 * are whole numbers 1-10, but intensity multipliers (0.3/0.6/1.0) and multi-factor sums
 * routinely land on a fraction, e.g. a single moderate factor at weight 9 is 5.4.
 */
fun Double.formatPoints(): String {
    val rounded = (this * 10).roundToInt() / 10.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        "%.1f".format(rounded)
    }
}
