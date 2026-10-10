package com.example.ui.screens

import androidx.compose.animation.*
import kotlin.math.roundToInt
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.BaristaCalcViewModel

// Calculation model for Heuristic Taste Hypotheses
data class LabFlavorProfile(
    val aroma: Int,
    val acidity: Int,
    val sweetness: Int,
    val body: Int,
    val bitterness: Int,
    val finish: Int,
    val extractionIndex: Float,
    val labels: List<String>,
    val summary: String
)

// Legacy visual heuristic, NOT validated extraction physics or sensory measurement.
fun calculateLabProfile(
    coffeeGrams: Float,
    waterMl: Int,
    ratio: Float,
    temperature: Int,
    grindClicks: Int,
    freshnessState: String,
    altitudeMeters: Int = 0,
    timeSeconds: Int = 180,
    preciseTemperatureC: Double? = null,
    useFahrenheit: Boolean = false,
    method: String = "V60"
): LabFlavorProfile {
    // Effective ratio (water / coffee) fallback to ratio parameter if safe
    val effectiveRatio = if (ratio > 0f) {
        ratio
    } else {
        16.0f
    }.coerceIn(5f, 30f)

    // Boiling point at altitude: T_boil = 100 - (altitude * 0.0034)
    val tBoil = (100.0f - (altitudeMeters.coerceIn(0, 5000) * 0.0034f)).coerceIn(80.0f, 100.0f)

    // Effective water temperature cannot exceed boiling point at atmospheric pressure
    val reference = com.example.data.engine.LabTemperatureGuide(preciseTemperatureC ?: temperature.toDouble(), altitudeMeters, useFahrenheit, method)
    val tempEffective = if (reference.openHotWater) minOf((preciseTemperatureC ?: temperature.toDouble()).toFloat(), tBoil) else (preciseTemperatureC ?: temperature.toDouble()).toFloat()

    // Legacy visual weighting, NOT a validated altitude extraction correction.
    val altitudeFactor = kotlin.math.sqrt(tBoil / 100.0f)

    // Legacy illustration only; numerical grinder references are not universal particle sizes.
    // extRaw = (tiempoActual / tiempoTechnique) × (moliendaTechnique / moliendaActual) × ((temperaturaC - 35) / 55) * altitudeFactor
    val clicksActual = grindClicks.coerceIn(4, 50).toFloat()
    val timeFactor = (timeSeconds.coerceIn(60, 360).toFloat() / 180.0f).coerceIn(0.55f, 1.85f)

    val extRaw = timeFactor * (22.0f / clicksActual) * ((tempEffective - 35.0f) / 55.0f) * altitudeFactor
    val extractionIndex = extRaw.coerceIn(0.45f, 1.65f)

    // Seis puntuaciones enteras, limitadas entre 8 y 96:
    // aroma = 58 + (temperatura - 88) × 1.4 - max(0, extracción - 1.22) × 16 + max(0, 22 - clicks) × 0.9
    val aromaRaw = 58.0f + (tempEffective - 88.0f) * 1.4f - maxOf(0.0f, extractionIndex - 1.22f) * 16.0f + maxOf(0.0f, 22.0f - clicksActual) * 0.9f

    // acidez = 54 + (1 - extracción) × 42 + (89 - temperatura) × 0.5 + max(0, ratio - 15.5) × 0.8
    val acidityRaw = 54.0f + (1.0f - extractionIndex) * 42.0f + (89.0f - tempEffective) * 0.5f + maxOf(0.0f, effectiveRatio - 15.5f) * 0.8f

    // dulzor = 92 - abs(1 - extracción) × 82 - abs(temperatura - 91) × 0.9
    val sweetnessRaw = 92.0f - kotlin.math.abs(1.0f - extractionIndex) * 82.0f - kotlin.math.abs(tempEffective - 91.0f) * 0.9f

    // cuerpo = 40 + 150 / ratio + max(0, 24 - clicks) × 0.9 + max(0, extracción - 1) × 10
    val bodyRaw = 40.0f + (150.0f / effectiveRatio) + maxOf(0.0f, 24.0f - clicksActual) * 0.9f + maxOf(0.0f, extractionIndex - 1.0f) * 10.0f

    // amargor = 32 + max(0, extracción - 1) × 44 + max(0, temperatura - 92) × 2 + max(0, 18 - clicks) × 1.1
    val bitternessRaw = 32.0f + maxOf(0.0f, extractionIndex - 1.0f) * 44.0f + maxOf(0.0f, tempEffective - 92.0f) * 2.0f + maxOf(0.0f, 18.0f - clicksActual) * 1.1f

    val finalAroma = Math.round(aromaRaw).coerceIn(8, 96)
    val finalAcidity = Math.round(acidityRaw).coerceIn(8, 96)
    val finalSweetness = Math.round(sweetnessRaw).coerceIn(8, 96)
    val finalBody = Math.round(bodyRaw).coerceIn(8, 96)
    val finalBitterness = Math.round(bitternessRaw).coerceIn(8, 96)

    // final = 48 + (dulzor - 50) × 0.25 + (cuerpo - 50) × 0.18 - max(0, amargor - 58) × 0.22
    val finishRaw = 48.0f + (finalSweetness - 50.0f) * 0.25f + (finalBody - 50.0f) * 0.18f - maxOf(0.0f, finalBitterness - 58.0f) * 0.22f
    val finalFinish = Math.round(finishRaw).coerceIn(8, 96)

    // Never present the legacy illustration as measured flavor or guaranteed balance.
    val supported = reference.kind in listOf("filter", "chemex", "press", "aero")
    val activeLabels = listOf(reference.headline, "Hipótesis visual · no medición")
    val summary = reference.detail

    return LabFlavorProfile(
        aroma = if (supported) finalAroma else 0,
        acidity = if (supported) finalAcidity else 0,
        sweetness = if (supported) finalSweetness else 0,
        body = if (supported) finalBody else 0,
        bitterness = if (supported) finalBitterness else 0,
        finish = if (supported) finalFinish else 0,
        extractionIndex = extractionIndex,
        labels = activeLabels.distinct().take(3),
        summary = summary
    )
}

enum class LabCategory {
    Proporcion, Extraccion, Grano
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabScreen(
    viewModel: BaristaCalcViewModel,
    onNavigateToSection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    var selectedCategory by remember { mutableStateOf(LabCategory.Extraccion) }
    val isFahrenheit = state.useFahrenheit
    var showInfoSheet by remember { mutableStateOf(false) }
    var showContextSheet by remember { mutableStateOf(false) }
    var methodMenuExpanded by remember { mutableStateOf(false) }

    var showRecipeDialog by remember { mutableStateOf(false) }
    var showTechniqueDialog by remember { mutableStateOf(false) }
    var inputRecipeName by remember { mutableStateOf("") }
    var inputTechniqueName by remember { mutableStateOf("") }
    var isSavingRecipe by remember { mutableStateOf(false) }
    var isSavingTechnique by remember { mutableStateOf(false) }


    val currentProfile = remember(
        state.labCoffee,
        state.labWater,
        state.labRatio,
        state.labTemp,
        state.labPreciseTemp,
        state.useFahrenheit,
        state.labClicks,
        state.labBeanFreshness,
        state.labAltitudeMeters,
        state.labEstTimeSeconds,
        state.labMethod
    ) {
        calculateLabProfile(
            coffeeGrams = state.labCoffee,
            waterMl = state.labWater,
            ratio = state.labRatio,
            temperature = state.labTemp,
            preciseTemperatureC = state.labPreciseTemp,
            grindClicks = state.labClicks,
            freshnessState = state.labBeanFreshness,
            altitudeMeters = state.labAltitudeMeters,
            timeSeconds = state.labEstTimeSeconds,
            useFahrenheit = state.useFahrenheit,
            method = state.labMethod
        )
    }

    if (showRecipeDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingRecipe) showRecipeDialog = false },
            title = { Text("Guardar Receta Base", fontWeight = FontWeight.Bold, color = TextPrincipal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Guarda esta hipótesis como una receta rápida en tus favoritos.", fontSize = 13.sp, color = TextSecundario)
                    OutlinedTextField(
                        value = inputRecipeName,
                        onValueChange = { inputRecipeName = it },
                        label = { Text("Nombre de la receta") },
                        placeholder = { Text("Fórmula Lab ${state.labMethod}") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("recipe_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isSavingRecipe) return@Button
                        val nameStr = inputRecipeName.ifBlank { "Receta Lab ${state.labMethod}" }
                        isSavingRecipe = true
                        viewModel.saveLabAsRecipe(nameStr) { success ->
                            isSavingRecipe = false
                            if (success) {
                                showRecipeDialog = false
                                inputRecipeName = ""
                            }
                        }
                    },
                    enabled = !isSavingRecipe,
                    colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal),
                    modifier = Modifier.testTag("recipe_submit_btn")
                ) {
                    Text(if (isSavingRecipe) "Guardando…" else "Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecipeDialog = false }, enabled = !isSavingRecipe) {
                    Text("Cancelar", color = TextSecundario)
                }
            }
        )
    }

    if (showTechniqueDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingTechnique) showTechniqueDialog = false },
            title = { Text("Guardar Técnica", fontWeight = FontWeight.Bold, color = TextPrincipal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Guarda esta configuración como técnica estructurada.", fontSize = 13.sp, color = TextSecundario)
                    OutlinedTextField(
                        value = inputTechniqueName,
                        onValueChange = { inputTechniqueName = it },
                        label = { Text("Nombre de la técnica") },
                        placeholder = { Text("Técnica Lab ${state.labMethod}") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("technique_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isSavingTechnique) return@Button
                        val nameStr = inputTechniqueName.ifBlank { "Técnica Lab ${state.labMethod}" }
                        isSavingTechnique = true
                        viewModel.saveLabAsTechnique(nameStr) { success ->
                            isSavingTechnique = false
                            if (success) {
                                showTechniqueDialog = false
                                inputTechniqueName = ""
                            }
                        }
                    },
                    enabled = !isSavingTechnique,
                    colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal),
                    modifier = Modifier.testTag("technique_submit_btn")
                ) {
                    Text(if (isSavingTechnique) "Guardando…" else "Registrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTechniqueDialog = false }, enabled = !isSavingTechnique) {
                    Text("Cancelar", color = TextSecundario)
                }
            }
        )
    }


    if (showInfoSheet) {
        LabInfoSheet(onDismissRequest = { showInfoSheet = false }, guide = com.example.data.engine.LabTemperatureGuide(state.labPreciseTemp ?: state.labTemp.toDouble(), state.labAltitudeMeters, state.useFahrenheit, state.labMethod))
    }
    if (showContextSheet) {
        LabContextSheet(state = state, viewModel = viewModel, onDismissRequest = { showContextSheet = false })
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MainBackground)
    ) {
        com.example.ui.components.BrewOrganicBackground(warmTop = false)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 88.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // 2. SCREEN HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Laboratorio",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrincipal
                        )
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AcentoSuave)
                                    .clickable { methodMenuExpanded = true }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(state.labMethod.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AcentoPrincipal, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Cambiar método de extracción", tint = AcentoPrincipal, modifier = Modifier.size(14.dp))
                            }
                            DropdownMenu(expanded = methodMenuExpanded, onDismissRequest = { methodMenuExpanded = false }) {
                                state.allBrewMethods.forEach { method ->
                                    val methodName = when (method.code.lowercase()) {
                                        "v60" -> "V60"; "aeropress" -> "AeroPress"; "espresso" -> "Espresso"
                                        "french_press" -> "Prensa francesa"; "chemex" -> "Chemex"; "moka" -> "Moka"; "cold_brew" -> "Cold brew"
                                        else -> method.nameKey.removePrefix("brew_method.").removeSuffix(".name")
                                    }
                                    DropdownMenuItem(
                                        text = { Text(methodName) },
                                        onClick = {
                                            viewModel.selectMethodForLab(method.id, methodName)
                                            methodMenuExpanded = false
                                        },
                                        leadingIcon = { if (method.id == state.labMethodId) Icon(Icons.Default.Check, contentDescription = null) }
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { showContextSheet = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Contexto", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard)
                            .border(1.dp, BordeSuave, CircleShape)
                            .clickable { viewModel.resetLabVariables() }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Resetear Lab",
                            tint = TextPrincipal,
                            modifier = Modifier.size(16.dp)
                        )
                    }


                }
            }

            // 4. CONTROL DOCK CAT TABS
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SensoryMixerCard(profile = currentProfile, onInformation = { showInfoSheet = true })
                LabVariableGroupTabs(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }

            // 5. VARIABLE DOCK CONTROLS
            LabVariableDock(
                category = selectedCategory,
                state = state,
                viewModel = viewModel,
                isFahrenheit = isFahrenheit,
                onNavigateToSection = onNavigateToSection
            )
        }

        // --- 6. FIXED BOTTOM ACTION BAR DOCK ---
        LabActionBar(
            modifier = Modifier.align(Alignment.BottomCenter).testTag("lab_actions"),
            onPrepareClick = {
                if (viewModel.playLabIdeaAsPrep()) onNavigateToSection("brew")
            },
            onSaveExperimentClick = {
                viewModel.saveLabExperiment()
            }
        )
    }
}

@Composable
fun LabHypothesisCard(
    profile: LabFlavorProfile,
    state: com.example.ui.viewmodel.BaristaCalcState
) {
    var expanded by remember { mutableStateOf(false) }
    val ratio = state.labRatio.coerceIn(2f, 25f)
    val (rawC1, rawC2) = when {
        ratio <= 6f -> Pair(Color(0xFF3D2817), Color(0xFF7A3B2E))
        ratio <= 12f -> Pair(Color(0xFF4A3728), Color(0xFFA85D3F))
        ratio <= 15f -> Pair(Color(0xFF2D4A3E), Color(0xFFB5714A))
        ratio <= 18f -> Pair(Color(0xFF3D5E4F), Color(0xFF6B9080))
        else -> Pair(Color(0xFF6B9080), Color(0xFFC9D6C4))
    }

    val animC1 by animateColorAsState(targetValue = rawC1, animationSpec = tween(500), label = "labH1")
    val animC2 by animateColorAsState(targetValue = rawC2, animationSpec = tween(500), label = "labH2")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .semantics { stateDescription = if (expanded) "Perfil expandido" else "Perfil resumido" }
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = animC1.copy(alpha = 0.4f),
                ambientColor = animC2.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.1f), animC1, animC2),
                    start = Offset(0f, 0f),
                    end = Offset(700f, 700f)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = size.width * 0.45f,
                center = Offset(size.width * 0.85f, size.height * 0.2f)
            )
            val grainColor = Color.White.copy(alpha = 0.045f)
            for (i in 0 until 40) {
                val px = (i * 31.3f) % size.width
                val py = (i * 17.7f) % size.height
                drawCircle(color = grainColor, radius = 1.3f, center = Offset(px, py))
            }
        }

        Column(
            modifier = Modifier.padding(if (expanded) 18.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (expanded) 68.dp else 40.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    LabCupPreview(
                        ratio = state.labRatio,
                        temperature = state.labTemp,
                        grindClicks = state.labClicks,
                        profile = profile
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "GUÍA DEL MÉTODO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 0.8.sp
                    )

                    val primaryOutcomeLabel = "Estimación de sabor · ver detalle"
                    Text(
                        text = primaryOutcomeLabel,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontSize = if (expanded) 18.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        // Let the advice wrap instead of hiding its meaning.
                    )

                    if (expanded) LabHypothesisChips(labels = profile.labels.take(2))
                }
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Ocultar consejos" else "Ver consejos", tint = Color.White)
            }

            if (expanded) Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = profile.summary,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 15.sp,
                    // The complete recommendation must stay readable.
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabHypothesisChips(labels: List<String>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (labels.isEmpty()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("CALIBRANDO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            labels.forEach { label ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = label.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SensoryMixerCard(profile: LabFlavorProfile, onInformation: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(26.dp), spotColor = CafeCalidoOscuro.copy(alpha = 0.12f))
            .border(1.dp, BordeSuave, RoundedCornerShape(26.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ECUALIZADOR · 0–100",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecundario,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = onInformation, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "Acerca del ecualizador", tint = AcentoPrincipal, modifier = Modifier.size(18.dp))
                }
            }

            SensoryEqualizerBars(profile = profile)
        }
    }
}

@Composable
fun SensoryEqualizerBars(profile: LabFlavorProfile) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(vertical = 4.dp)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val color = Color(0x1260756A)
                val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.30f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.30f),
                    strokeWidth = strokeWidth,
                    pathEffect = pathEffect
                )
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.55f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.55f),
                    strokeWidth = strokeWidth,
                    pathEffect = pathEffect
                )
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.80f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.80f),
                    strokeWidth = strokeWidth,
                    pathEffect = pathEffect
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val barData = listOf(
                Triple("Aroma", profile.aroma, Color(0xFFC59A5A)),
                Triple("Acidez", profile.acidity, Color(0xFFF2C14E)),
                Triple("Dulzor", profile.sweetness, Color(0xFFD98BB3)),
                Triple("Cuerpo", profile.body, Color(0xFF8B6B5C)),
                Triple("Amargor", profile.bitterness, Color(0xFF5C5641)),
                Triple("Final", profile.finish, Color(0xFF74BFE0))
            )

            barData.forEach { (label, value, color) ->
                SensoryEqualizerBarItem(
                    label = label,
                    value = value,
                    color = color,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SensoryEqualizerBarItem(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedPercent by animateFloatAsState(
        targetValue = value.toFloat() / 100f,
        animationSpec = tween(durationMillis = 220),
        label = "Eq_$label"
    )

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (value == 0) "—" else "$value",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            modifier = Modifier.height(18.dp)
        )

        Box(
            modifier = Modifier
                .width(12.dp)
                .weight(1f)
                .clip(RoundedCornerShape(2.dp))
                .background(MainBackgroundAlt.copy(alpha = 0.6f))
                .border(1.dp, BordeSuave, RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedPercent)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = 0.7f), color)
                        )
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.95f))
                )
            }
        }

        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecundario,
            modifier = Modifier.fillMaxWidth().height(16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun LabVariableGroupTabs(
    selectedCategory: LabCategory,
    onCategorySelected: (LabCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, BordeSuave, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LabCategory.values().forEach { category ->
            val isSelected = selectedCategory == category
            val label = when (category) {
                LabCategory.Proporcion -> "Proporción"
                LabCategory.Extraccion -> "Calor"
                LabCategory.Grano -> "Grano"
            }
            val icon = when (category) {
                LabCategory.Proporcion -> Icons.Default.Scale
                LabCategory.Extraccion -> Icons.Default.Thermostat
                LabCategory.Grano -> Icons.Default.Grass
            }
            Box(
                modifier = (if (isSelected) Modifier.weight(1f) else Modifier.width(44.dp))
                    .height(44.dp)
                    .animateContentSize()
                    .semantics { contentDescription = label; stateDescription = if (isSelected) "Seleccionado" else "No seleccionado" }
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) AcentoPrincipal else Color.Transparent)
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else TextSecundario,
                        modifier = Modifier.size(18.dp)
                    )
                    if (isSelected) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else TextSecundario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    }
                }
            }
        }
    }
}

/** Native horizontal drag arbitration; linear scale and current callback.
 * The former competing tap/omnidirectional drag handlers fought the ScrollView.
 * Rounded values publish once per change; haptics are throttled, never LongPress.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabCalibratedSlider(
    value: Float, onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>, recommendedRange: ClosedFloatingPointRange<Float>?,
    step: Float = 1f, activeColor: Color, accessibilityLabel: String,
    accessibilityValue: String, modifier: Modifier = Modifier,
    referencePoints: List<Float> = emptyList()
) {
    val callback by rememberUpdatedState(onValueChange)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var lastPublished by remember(range, step) { mutableFloatStateOf(value) }
    var lastHapticAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(value) { lastPublished = value }
    fun publish(raw: Float) {
        val next = (if (step > 0) (raw / step).roundToInt() * step else raw).coerceIn(range.start, range.endInclusive)
        if (next == lastPublished) return
        lastPublished = next
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastHapticAt >= 45) {
            lastHapticAt = now
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        }
        callback(next)
    }
    Slider(
        value = value.coerceIn(range.start, range.endInclusive),
        onValueChange = ::publish,
        valueRange = range,
        steps = if (step > 0) ((range.endInclusive - range.start) / step).roundToInt().minus(1).coerceAtLeast(0) else 0,
        modifier = modifier.fillMaxWidth().height(44.dp).semantics {
            contentDescription = accessibilityLabel
            stateDescription = accessibilityValue
        },
        thumb = {
            Box(Modifier.size(22.dp).shadow(2.dp, CircleShape, spotColor = CafeCalidoOscuro.copy(alpha = 0.12f))
                .background(SurfaceCard, CircleShape).border(2.dp, activeColor, CircleShape))
        },
        track = { slider ->
            Canvas(Modifier.fillMaxWidth().height(24.dp)) {
                val mid = size.height / 2
                fun x(v: Float) = size.width * ((v - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
                drawLine(MainBackgroundAlt, Offset(0f, mid), Offset(size.width, mid), 4.dp.toPx(), StrokeCap.Round)
                if (recommendedRange != null) {
                    val left = x(recommendedRange.start); val right = x(recommendedRange.endInclusive)
                    drawLine(activeColor.copy(alpha = 0.18f), Offset(left, mid), Offset(right, mid), 10.dp.toPx(), StrokeCap.Round)
                    // Two clean limits, no dense ruler or oversized shaded pill.
                    listOf(left, right).forEach { px ->
                        drawLine(activeColor.copy(alpha = 0.65f), Offset(px, mid - 6.dp.toPx()), Offset(px, mid + 6.dp.toPx()), 1.5.dp.toPx(), StrokeCap.Round)
                    }
                }
                referencePoints.filter { it in range }.forEach { point ->
                    drawLine(activeColor.copy(alpha = 0.65f), Offset(x(point), mid - 6.dp.toPx()), Offset(x(point), mid + 6.dp.toPx()), 1.5.dp.toPx(), StrokeCap.Round)
                }
                drawLine(activeColor.copy(alpha = 0.75f), Offset(0f, mid), Offset(x(slider.value), mid), 3.dp.toPx(), StrokeCap.Round)
            }
        }
    )
}

@Composable
fun LabVariableDock(
    category: LabCategory,
    state: com.example.ui.viewmodel.BaristaCalcState,
    viewModel: BaristaCalcViewModel,
    isFahrenheit: Boolean,
    onNavigateToSection: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp), spotColor = CafeCalidoOscuro.copy(alpha = 0.12f))
            .border(1.dp, BordeSuave, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (category) {
                LabCategory.Proporcion -> {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${state.labCoffee.toInt()} g", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        IconButton(modifier = Modifier.size(32.dp), onClick = { viewModel.updateLabVariables(coffee = (state.labCoffee - 1).coerceAtLeast(1f)) }) { Icon(Icons.Default.Remove, "Menos 1 g") }
                        IconButton(modifier = Modifier.size(32.dp), onClick = { viewModel.updateLabVariables(coffee = (state.labCoffee + 1).coerceAtMost(100f)) }) { Icon(Icons.Default.Add, "Más 1 g") }
                        Text("${state.labWater} ml", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        IconButton(modifier = Modifier.size(32.dp), onClick = { viewModel.updateLabVariables(water = (state.labWater - 1).coerceAtLeast(10)) }) { Icon(Icons.Default.Remove, "Menos 1 ml") }
                        IconButton(modifier = Modifier.size(32.dp), onClick = { viewModel.updateLabVariables(water = (state.labWater + 1).coerceAtMost(2000)) }) { Icon(Icons.Default.Add, "Más 1 ml") }
                    }
                    // Slider 1: Ratio de Extracción (Proporción)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Proporción", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                            Text(
                                text = "1:${String.format(java.util.Locale.US, "%.1f", state.labRatio)}",
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AcentoPrincipal
                            )
                        }
                        LabCalibratedSlider(
                            value = state.labRatio,
                            onValueChange = { viewModel.updateLabVariables(ratio = it) },
                            range = 1f..40f,
                            recommendedRange = null,
                            step = 1f,
                            activeColor = AcentoPrincipal,
                            accessibilityLabel = "Proporción de café y agua",
                            accessibilityValue = "Uno a ${String.format(java.util.Locale.US, "%.1f", state.labRatio)}"
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Más concentrado", modifier = Modifier.weight(1f), fontSize = 9.sp, lineHeight = 12.sp, color = TextSecundario)
                            Text("Agua / café", modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, color = AcentoPrincipal)
                            Text("Más diluido", modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontSize = 9.sp, lineHeight = 12.sp, color = TextSecundario)
                        }
                    }

                    // Slider 2: Tiempo de Extracción
                    val timeSec = state.labEstTimeSeconds
                    val minutes = timeSec / 60
                    val seconds = timeSec % 60
                    val formattedTime = if (state.labMethod.lowercase() in listOf("cold brew", "coldbrew")) "${timeSec / 3600} h" else String.format(java.util.Locale.US, "%d:%02d min", minutes, seconds)

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tiempo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                            Text(
                                text = formattedTime,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AcentoPrincipal
                            )
                        }
                        LabCalibratedSlider(
                            value = timeSec.toFloat(),
                            onValueChange = { viewModel.updateLabVariables(estTimeSeconds = it.toInt()) },
                            range = when (state.labMethod.lowercase()) { "espresso" -> 5f..90f; "cold brew", "coldbrew" -> 3600f..86400f; else -> 30f..600f },
                            recommendedRange = null, // Timing depends on the selected technique.
                            step = if (state.labMethod.lowercase() in listOf("cold brew", "coldbrew")) 3600f else 1f,
                            activeColor = AcentoPrincipal,
                            accessibilityLabel = "Tiempo de extracción",
                            accessibilityValue = formattedTime
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Menos contacto", fontSize = 10.sp, lineHeight = 12.sp, color = TextSecundario)
                            Text("Según tu técnica", fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, color = AcentoPrincipal)
                            Text("Más contacto", fontSize = 10.sp, lineHeight = 12.sp, color = TextSecundario)
                        }
                    }
                }
                LabCategory.Extraccion -> {
                    // Header with title and °C / °F Unit Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "CONTROL DE CALOR Y MOLIENDA",
                            fontSize = 10.sp, lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecundario,
                            letterSpacing = 1.sp
                        )

                    }

                    // Slider 1: Temperatura del Agua
                    val thermal = com.example.data.engine.LabTemperatureGuide(state.labPreciseTemp ?: state.labTemp.toDouble(), state.labAltitudeMeters, isFahrenheit, state.labMethod)
                    val displayTemp = if (isFahrenheit) {
                        val fVal = Math.round((state.labPreciseTemp ?: state.labTemp.toDouble()) * 1.8 + 32)
                        "$fVal °F"
                    } else {
                        "${state.labTemp} °C"
                    }

                    val degrees = if (isFahrenheit) ((state.labPreciseTemp ?: state.labTemp.toDouble()) * 1.8 + 32).roundToInt() else state.labTemp
                    fun change(delta: Int) { val next = (degrees + delta).coerceIn(thermal.sliderRange.start.toInt(), thermal.sliderRange.endInclusive.toInt()); viewModel.updateLabVariables(preciseTemperature = if (isFahrenheit) (next - 32) / 1.8 else next.toDouble()) }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Temperatura", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)

                            }
                            Text(
                                text = displayTemp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CafeCalidoClaro
                            )
                            IconButton(modifier = Modifier.size(32.dp), onClick = { change(-1) }) { Icon(Icons.Default.Remove, "Menos 1 grado", Modifier.size(16.dp)) }
                            IconButton(modifier = Modifier.size(32.dp), onClick = { change(1) }) { Icon(Icons.Default.Add, "Más 1 grado", Modifier.size(16.dp)) }

                        }

                        LabCalibratedSlider(
                            value = (if (isFahrenheit) (state.labPreciseTemp ?: state.labTemp.toDouble()) * 1.8 + 32 else state.labPreciseTemp ?: state.labTemp.toDouble()).toFloat(),
                            onValueChange = { viewModel.updateLabVariables(preciseTemperature = if (isFahrenheit) (it.roundToInt() - 32) / 1.8 else it.roundToInt().toDouble()) },
                            range = thermal.sliderRange,
                            recommendedRange = if (thermal.hasReachableBand && thermal.kind !in listOf("aero", "chemex")) thermal.recommendedRange else null,
                            referencePoints = if (thermal.hasReachableBand && thermal.kind in listOf("aero", "chemex")) listOf(thermal.degrees(thermal.lowerC).toFloat(), thermal.degrees(thermal.upperC).toFloat()).distinct() else emptyList(),
                            step = 1f,
                            activeColor = CafeCalidoClaro,
                            accessibilityLabel = "Temperatura del agua",
                            accessibilityValue = displayTemp
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${thermal.sliderRange.start.toInt()}${if (isFahrenheit) "°F" else "°C"}", fontSize = 10.sp, lineHeight = 12.sp, color = TextSecundario)
                            Text(thermal.workingRangeText, fontSize = 10.sp, lineHeight = 12.sp, color = CafeCalidoOscuro)
                            Text("${thermal.sliderRange.endInclusive.toInt()}${if (isFahrenheit) "°F" else "°C"}", fontSize = 10.sp, lineHeight = 12.sp, color = TextSecundario)
                        }
                        if (thermal.warning) Text(if (thermal.openHotWater && thermal.temperatureC > thermal.boilingC) "Supera el hervor local" else "Calor fuera de zona · consulta ⓘ", fontSize = 10.sp, lineHeight = 12.sp, color = AcentoPrincipal)

                    }

                    // Slider 2: Clicks de Molienda
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Molienda", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)

                            }
                            Text(
                                text = "Ref. ${state.labClicks}",
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CafeCalidoClaro
                            )
                        }

                        LabCalibratedSlider(
                            value = state.labClicks.toFloat(),
                            onValueChange = { viewModel.updateLabVariables(clicks = it.toInt()) },
                            range = 6f..36f,
                            recommendedRange = null,
                            step = 1f,
                            activeColor = CafeCalidoClaro,
                            accessibilityLabel = "Ajuste de molienda",
                            accessibilityValue = "Referencia ${state.labClicks} de tu molino. Menos gruesa a la izquierda; más gruesa a la derecha."
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("← Menos gruesa", fontSize = 10.sp, lineHeight = 12.sp, color = TextSecundario)
                            Text("Más gruesa →", fontSize = 10.sp, lineHeight = 12.sp, color = TextSecundario)
                        }
                    }

                    // Live Educational Recommendation banner (connecting state.labRecommendationText)

                }
                LabCategory.Grano -> {
                    Text("ESTADO DEL GRANO Y FRESCURA", fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, color = TextSecundario, letterSpacing = 1.sp)
                    
                    val freshnessOptions = listOf("muy fresco", "en ventana", "punto ideal", "bajando", "viejo")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        freshnessOptions.forEach { opt ->
                            val isSelected = state.labBeanFreshness == opt
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) AcentoPrincipal else MainBackgroundAlt.copy(alpha = 0.5f))
                                    .border(1.dp, if (isSelected) AcentoPrincipal else BordeSuave, RoundedCornerShape(12.dp))
                                    .clickable { viewModel.updateLabVariables(freshness = opt) }
                                    .padding(vertical = 10.dp, horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = opt.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextPrincipal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LabActionBar(
    modifier: Modifier = Modifier,
    onPrepareClick: () -> Unit,
    onSaveExperimentClick: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = SurfaceCard,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onSaveExperimentClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrincipal),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BordeSuave),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    tint = AcentoPrincipal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Experimento", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }

            Button(
                onClick = onPrepareClick,
                colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("prepare_idea_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Preparar",
                    maxLines = 1,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabInfoSheet(onDismissRequest: () -> Unit, guide: com.example.data.engine.LabTemperatureGuide = com.example.data.engine.LabTemperatureGuide(92.0, 0)) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = SurfaceCard,
        contentColor = TextPrincipal,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "ECUALIZADOR DIDÁCTICO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AcentoPrincipal,
                letterSpacing = 1.sp
            )
            Text(
                text = "Cómo leer la estimación",
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrincipal
            )

            Text("Los números de 0 a 100 son puntuaciones orientativas, no porcentajes ni mediciones del sabor. Sirven para comparar ajustes; no garantizan el resultado de tu taza.", fontSize = 13.sp, color = TextSecundario)
            Text("El modelo es una hipótesis local, no una fórmula científicamente calibrada. Grano, tueste, molino, agua y técnica cambian el resultado. Registra el sabor real en Cata. Para métodos sin modelo disponible verás —.", fontSize = 12.sp, color = TextSecundario)
            Text(guide.headline, fontWeight = FontWeight.Bold)
            Text(guide.detail, fontSize = 12.sp, color = TextSecundario)
            Text("Puntos orientativos del método: " + guide.rangeText, fontSize = 12.sp)
            if (guide.openHotWater) Text("Hervor estimado: ${guide.degrees(guide.boilingC)} ${if (guide.fahrenheit) "°F" else "°C"}. La presión atmosférica real puede variar.", fontSize = 12.sp)
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            if (guide.sourceURL.isNotEmpty()) TextButton(onClick = { uriHandler.openUri(guide.sourceURL) }) { Text(guide.sourceName + " ↗") }
            Button(
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Listo", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabContextSheet(
    state: com.example.ui.viewmodel.BaristaCalcState,
    viewModel: BaristaCalcViewModel,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MainBackground,
        contentColor = TextPrincipal
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Contexto del experimento", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "Conecta la hipótesis con el método, la receta, la técnica y tu equipo. Estas relaciones viajarán a Preparar, Cata y Almacén.",
                fontSize = 13.sp,
                color = TextSecundario
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, BordeSuave),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabContextDropdown(
                        label = "Método de extracción",
                        emptyLabel = "Selecciona un método",
                        selectedId = state.labMethodId,
                        selectedLabel = state.labMethod,
                        options = state.allBrewMethods.map { method ->
                            method.id to when (method.code.lowercase()) {
                                "v60" -> "V60"; "aeropress" -> "AeroPress"; "espresso" -> "Espresso"
                                "french_press" -> "Prensa francesa"; "chemex" -> "Chemex"; "moka" -> "Moka"; "cold_brew" -> "Cold brew"
                                else -> method.nameKey.removePrefix("brew_method.").removeSuffix(".name")
                            }
                        },
                        allowEmpty = false,
                        onSelect = { id ->
                            state.allBrewMethods.firstOrNull { it.id == id }?.let { method ->
                                val name = when (method.code.lowercase()) {
                                    "v60" -> "V60"; "aeropress" -> "AeroPress"; "espresso" -> "Espresso"
                                    "french_press" -> "Prensa francesa"; "chemex" -> "Chemex"; "moka" -> "Moka"; "cold_brew" -> "Cold brew"
                                    else -> method.nameKey.removePrefix("brew_method.").removeSuffix(".name")
                                }
                                viewModel.selectMethodForLab(method.id, name)
                            }
                        }
                    )
                    LabContextDropdown(
                        label = "Receta base",
                        emptyLabel = "Sin receta base",
                        selectedId = state.labRecipeId,
                        selectedLabel = state.recipesList.firstOrNull { it.id == state.labRecipeId }?.name,
                        options = state.recipesList.map { it.id to it.name },
                        onSelect = viewModel::selectRecipeForLab
                    )
                    LabContextDropdown(
                        label = "Técnica base",
                        emptyLabel = "Modo libre, sin técnica base",
                        selectedId = state.labTechniqueId,
                        selectedLabel = state.techniquesList.firstOrNull { it.id == state.labTechniqueId }?.name,
                        options = state.techniquesList.map { it.id to it.name },
                        onSelect = viewModel::selectTechniqueForLab
                    )
                    LabContextDropdown(
                        label = "Café",
                        emptyLabel = "Sin café seleccionado",
                        selectedId = state.labBeanId,
                        selectedLabel = state.beansList.firstOrNull { it.id == state.labBeanId }?.name,
                        options = state.beansList.map { it.id to it.name },
                        onSelect = { id ->
                            if (id == null) viewModel.clearBeanForLab()
                            else state.beansList.firstOrNull { it.id == id }?.let(viewModel::selectBeanForLab)
                        }
                    )
                    LabContextDropdown(
                        label = "Molino",
                        emptyLabel = "Sin molino seleccionado",
                        selectedId = state.labGrinderId,
                        selectedLabel = state.grindersList.firstOrNull { it.id == state.labGrinderId }?.name,
                        options = state.grindersList.map { it.id to it.name },
                        onSelect = { id -> viewModel.selectGrinderForLab(state.grindersList.firstOrNull { it.id == id }) }
                    )
                }
            }
            Button(onClick = onDismissRequest, modifier = Modifier.fillMaxWidth()) { Text("Listo") }
        }
    }
}

@Composable
private fun LabContextDropdown(
    label: String,
    emptyLabel: String,
    selectedId: String?,
    selectedLabel: String?,
    options: List<Pair<String, String>>,
    allowEmpty: Boolean = true,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecundario)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedLabel ?: emptyLabel, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Cambiar $label")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (allowEmpty) {
                    DropdownMenuItem(
                        text = { Text(emptyLabel) },
                        onClick = { onSelect(null); expanded = false },
                        leadingIcon = { if (selectedId == null) Icon(Icons.Default.Check, contentDescription = null) }
                    )
                }
                options.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = { onSelect(id); expanded = false },
                        leadingIcon = { if (selectedId == id) Icon(Icons.Default.Check, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
fun LabCupPreview(
    ratio: Float,
    temperature: Int,
    grindClicks: Int,
    profile: LabFlavorProfile
) {
    val darknessFactor = remember(ratio, temperature, grindClicks) {
        val rFactor = ((22f - ratio).coerceIn(0f, 17f) / 17f)
        val tFactor = ((temperature - 75f).coerceIn(0f, 24f) / 24f)
        val gFactor = ((40f - grindClicks).coerceIn(0f, 35f) / 35f)
        (rFactor * 0.5f + tFactor * 0.25f + gFactor * 0.25f).coerceIn(0.12f, 0.96f)
    }

    val coffeeLiquidColor = remember(darknessFactor) {
        val startR = 0xD4; val startG = 0x9B; val startB = 0x5D
        val endR = 0x22; val endG = 0x11; val endB = 0x04
        val r = (startR + (endR - startR) * darknessFactor).toInt().coerceIn(0, 255)
        val g = (startG + (endG - startG) * darknessFactor).toInt().coerceIn(0, 255)
        val b = (startB + (endB - startB) * darknessFactor).toInt().coerceIn(0, 255)
        Color(r, g, b)
    }

    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val platePath = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.12f, h * 0.88f)
                lineTo(w * 0.88f, h * 0.88f)
            }
            drawPath(
                path = platePath,
                color = Color.White.copy(alpha = 0.5f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            val cupBodyPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.24f, h * 0.38f)
                lineTo(w * 0.76f, h * 0.38f)
                cubicTo(w * 0.74f, h * 0.72f, w * 0.70f, h * 0.82f, w * 0.60f, h * 0.82f)
                lineTo(w * 0.40f, h * 0.82f)
                cubicTo(w * 0.30f, h * 0.82f, w * 0.26f, h * 0.72f, w * 0.24f, h * 0.38f)
                close()
            }

            drawPath(path = cupBodyPath, color = Color.White)
            drawPath(path = cupBodyPath, color = Color(0xFF3F7A63), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx()))

            val liquidHeightRatio = (ratio.coerceIn(5f, 22f) / 22f)
            val computedLevel = h * (0.80f - (liquidHeightRatio * 0.38f))

            val liquidPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.26f, computedLevel)
                lineTo(w * 0.74f, computedLevel)
                cubicTo(w * 0.72f, h * 0.70f, w * 0.68f, h * 0.80f, w * 0.59f, h * 0.80f)
                lineTo(w * 0.41f, h * 0.80f)
                cubicTo(w * 0.32f, h * 0.80f, w * 0.28f, h * 0.70f, w * 0.26f, computedLevel)
                close()
            }

            drawPath(path = liquidPath, color = coffeeLiquidColor)
        }
    }
}
