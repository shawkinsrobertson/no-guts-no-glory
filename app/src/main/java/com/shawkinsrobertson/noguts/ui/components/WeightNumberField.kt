package com.shawkinsrobertson.noguts.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val MIN_WEIGHT = 1
private const val MAX_WEIGHT = 10

/** A 1-10 number input for a factor's weight, replacing the slider. Clamps on every
 * keystroke rather than allowing an out-of-range value to sit uncommitted. */
@Composable
fun WeightNumberField(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Weight (1-10)"
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val digitsOnly = raw.filter { it.isDigit() }.take(2)
            if (digitsOnly.isEmpty()) {
                text = ""
                return@OutlinedTextField
            }
            val clamped = digitsOnly.toInt().coerceIn(MIN_WEIGHT, MAX_WEIGHT)
            text = clamped.toString()
            onValueChange(clamped)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier.width(120.dp)
    )
}
