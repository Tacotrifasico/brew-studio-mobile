package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.BaristaCalcViewModel
import java.util.Locale

@Composable
fun PreparationSettingsDialog(viewModel: BaristaCalcViewModel, onDismiss: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var altitude by remember { mutableStateOf(state.labAltitudeMeters.toString()) }
    val meters = altitude.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuración") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Temperatura")
                Row {
                    FilterChip(selected = !state.useFahrenheit,
                        onClick = { viewModel.setPreparationSettings(state.labAltitudeMeters, false) }, label = { Text("Celsius °C") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = state.useFahrenheit,
                        onClick = { viewModel.setPreparationSettings(state.labAltitudeMeters, true) }, label = { Text("Fahrenheit °F") })
                }
                OutlinedTextField(altitude, { if (it.all(Char::isDigit) && it.length <= 4) altitude = it },
                    label = { Text("Altura (metros)") }, singleLine = true,
                    isError = meters == null || meters !in 0..5000,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                val boil = 100f - (meters ?: state.labAltitudeMeters).coerceIn(0, 5000) * 0.0034f
                val display = if (state.useFahrenheit) boil * 9f / 5f + 32f else boil
                Text("Hervor estimado: ${String.format(Locale.US, "%.1f", display)} ${if (state.useFahrenheit) "°F" else "°C"}")
                Text("Se aplica al laboratorio y a sus recomendaciones. Los cálculos se conservan en Celsius.")
            }
        },
        confirmButton = { TextButton(enabled = meters != null && meters in 0..5000, onClick = {
            viewModel.setPreparationSettings(meters ?: 0, state.useFahrenheit); onDismiss()
        }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
