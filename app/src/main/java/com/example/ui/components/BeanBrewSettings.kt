package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.database.*
import com.example.ui.viewmodel.BaristaCalcViewModel
import com.example.ui.theme.MainBackgroundAlt

@Composable
fun CalculatorBeanBack(viewModel: BaristaCalcViewModel) {
    val state by viewModel.state.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val bean = state.beansList.firstOrNull { it.id == state.calculatorBeanId }
    Text("Método · ${state.method}", style = MaterialTheme.typography.labelLarge)
    Text("Grano", style = MaterialTheme.typography.labelMedium)
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(bean?.name ?: "Elegir grano del Almacén")
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Sin grano seleccionado") }, onClick = { viewModel.selectCalculatorBean(null); expanded = false })
            state.beansList.forEach { item ->
                DropdownMenuItem(text = { Text(item.name) }, onClick = { viewModel.selectCalculatorBean(item.id); expanded = false })
            }
        }
    }
    if (bean != null) BeanMethodSettings(bean, state.method, state.useFahrenheit, viewModel)
    else Text(if (state.beansList.isEmpty()) "Agrega un grano en Almacén para guardar sus ajustes." else "Cada grano recuerda sus ajustes por método.", style = MaterialTheme.typography.bodySmall)
}

@Composable
fun BeanMethodSettings(bean: Bean, method: String, fahrenheit: Boolean, viewModel: BaristaCalcViewModel) {
    val profile = BeanBrewProfiles.read(bean.brewProfilesJSON, method) ?: BeanBrewProfile(method)
    var saving by remember(bean.id, method) { mutableStateOf(false) }
    var error by remember(bean.id, method) { mutableStateOf(false) }
    fun save(clicks: Int, temperature: Int) {
        saving = true
        viewModel.saveBeanBrewProfile(bean.id, method, clicks, temperature) { success -> saving = false; error = !success }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()
        .background(MainBackgroundAlt, RoundedCornerShape(14.dp)).padding(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Clics de molino", style = MaterialTheme.typography.labelMedium)
            Text("${profile.clicks}", style = MaterialTheme.typography.titleMedium)
            Row {
                TextButton(modifier = Modifier.weight(1f), onClick = { save(profile.clicks - 1, profile.temperatureC) }, enabled = !saving && profile.clicks > 1) { Text("−") }
                TextButton(modifier = Modifier.weight(1f), onClick = { save(profile.clicks + 1, profile.temperatureC) }, enabled = !saving && profile.clicks < 200) { Text("+") }
            }
        }
        Column(Modifier.weight(1f)) {
            Text("Temperatura", style = MaterialTheme.typography.labelMedium)
            val display = if (fahrenheit) "${kotlin.math.round(profile.temperatureC * 1.8 + 32).toInt()} °F" else "${profile.temperatureC} °C"
            Text(display, style = MaterialTheme.typography.titleMedium)
            Row {
                TextButton(modifier = Modifier.weight(1f), onClick = { save(profile.clicks, profile.temperatureC - 1) }, enabled = !saving && profile.temperatureC > 1) { Text("−") }
                TextButton(modifier = Modifier.weight(1f), onClick = { save(profile.clicks, profile.temperatureC + 1) }, enabled = !saving && profile.temperatureC < 100) { Text("+") }
            }
        }
    }
    Text(when { error -> "No se guardó. Vuelve a ajustar para reintentar."; saving -> "Guardando…"; BeanBrewProfiles.read(bean.brewProfilesJSON, method) != null -> "Guardado para este grano · $method"; else -> "Ajusta para guardar tu punto favorito." }, style = MaterialTheme.typography.bodySmall)
}

@Composable
fun InventoryBeanBrewSettings(bean: Bean, viewModel: BaristaCalcViewModel) {
    val state by viewModel.state.collectAsState()
    // Resolve the live entity so editing in Almacén and BARC always reads the same data.
    val current = state.beansList.firstOrNull { it.id == bean.id } ?: bean
    var method by remember(bean.id) { mutableStateOf(state.method) }
    var expanded by remember { mutableStateOf(false) }
    Text("Cómo lo preparo", style = MaterialTheme.typography.titleSmall)
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(method) }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            (viewModel.presets.map { it.method } + state.userMethods.filter { it.isActive }.map { it.name } + state.method).distinct().forEach { name ->
                DropdownMenuItem(text = { Text(name) }, onClick = { method = name; expanded = false })
            }
        }
    }
    BeanMethodSettings(current, method, state.useFahrenheit, viewModel)
}
