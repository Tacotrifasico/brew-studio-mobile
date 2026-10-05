package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.example.data.database.*
import com.example.ui.viewmodel.BaristaCalcViewModel
import com.example.ui.theme.*

@Composable
fun CalculatorBeanBack(viewModel: BaristaCalcViewModel) {
    val state by viewModel.state.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val bean = state.beansList.firstOrNull { it.id == state.calculatorBeanId }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Método · ${state.method}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        color = AcentoPrincipal, maxLines = 2)
    Box(Modifier.fillMaxWidth()) {
        Surface(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp), color = SurfaceCard, border = BorderStroke(1.dp, BordeSuave)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Grano", fontSize = 12.sp, color = TextSecundario)
                    Text(bean?.name ?: "Elegir grano del Almacén", fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold, color = TextPrincipal, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Default.ExpandMore, contentDescription = "Elegir grano", tint = AcentoPrincipal)
            }
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Sin grano seleccionado") }, onClick = { viewModel.selectCalculatorBean(null); expanded = false })
            state.beansList.forEach { item ->
                DropdownMenuItem(text = { Text(item.name) }, onClick = { viewModel.selectCalculatorBean(item.id); expanded = false })
            }
        }
    }
    if (bean != null) BeanMethodSettings(bean, state.method, state.useFahrenheit, viewModel)
    else Text(if (state.beansList.isEmpty()) "Agrega un grano en Almacén para guardar sus ajustes." else "Cada grano recuerda sus ajustes por método.", fontSize = 12.sp, color = TextSecundario)
    }
}

@Composable
fun BeanMethodSettings(bean: Bean, method: String, fahrenheit: Boolean, viewModel: BaristaCalcViewModel) {
    val profile = BeanBrewProfiles.resolve(bean.brewProfilesJSON, method)
    var saving by remember(bean.id, method) { mutableStateOf(false) }
    var error by remember(bean.id, method) { mutableStateOf(false) }
    fun save(clicks: Int, temperature: Double) {
        saving = true
        viewModel.saveBeanBrewProfile(bean.id, method, clicks, temperature) { success -> saving = false; error = !success }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    BeanSettingsControls(profile, fahrenheit, saving, onClicks = { save(it, profile.temperatureC) },
        onTemperature = { save(profile.clicks, it) })
    if (BeanBrewProfiles.read(bean.brewProfilesJSON, method) == null && !saving && !error) {
        Text("Punto inicial · calibra según tu molino.", fontSize = 12.sp, color = TextSecundario)
        TextButton(onClick = { save(profile.clicks, profile.temperatureC) }) { Text("Guardar favorito", color = TextPrincipal) }
    } else Text(when { error -> "No se guardó. Vuelve a ajustar para reintentar."; saving -> "Guardando…"; else -> "Guardado para este grano · $method" }, fontSize = 12.sp, color = TextSecundario)
    }
}

/** Equal-width columns and bounded, joined steppers. No default TextButton min-width
 * or text wrapping can squeeze the adjacent metric on a small phone. */
@Composable
internal fun BeanSettingsControls(profile: BeanBrewProfile, fahrenheit: Boolean, saving: Boolean,
    onClicks: (Int) -> Unit, onTemperature: (Double) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()
        .background(MainBackgroundAlt, RoundedCornerShape(14.dp)).padding(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Clics de molino", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecundario)
            Text("${profile.clicks}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
            BeanSettingsStepper("clics de molino", !saving && profile.clicks > 1, !saving && profile.clicks < 200,
                { onClicks(profile.clicks - 1) }, { onClicks(profile.clicks + 1) })
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Temperatura", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecundario)
            val display = profile.temperatureText(fahrenheit)
            Text(display, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
            BeanSettingsStepper("temperatura", !saving && profile.displayDegrees(fahrenheit) > (if (fahrenheit) 34 else 1),
                !saving && profile.displayDegrees(fahrenheit) < (if (fahrenheit) 212 else 100),
                { onTemperature(profile.steppedTemperature(-1, fahrenheit)) }, { onTemperature(profile.steppedTemperature(1, fahrenheit)) })
        }
    }
}

@Composable
private fun BeanSettingsStepper(label: String, canDecrease: Boolean, canIncrease: Boolean,
    decrease: () -> Unit, increase: () -> Unit) {
    Surface(shape = RoundedCornerShape(10.dp), color = SurfaceCard, border = BorderStroke(1.dp, BordeSuave)) {
        Row(Modifier.widthIn(max = 112.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = decrease, enabled = canDecrease, modifier = Modifier.weight(1f).height(48.dp)) {
                Icon(Icons.Default.Remove, "Reducir $label", Modifier.size(18.dp), tint = if (canDecrease) TextPrincipal else TextSecundario.copy(alpha = 0.35f))
            }
            Box(Modifier.width(1.dp).height(24.dp).background(BordeSuave))
            IconButton(onClick = increase, enabled = canIncrease, modifier = Modifier.weight(1f).height(48.dp)) {
                Icon(Icons.Default.Add, "Aumentar $label", Modifier.size(18.dp), tint = if (canIncrease) TextPrincipal else TextSecundario.copy(alpha = 0.35f))
            }
        }
    }
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
