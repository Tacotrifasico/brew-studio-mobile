package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
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

// Continuous extraction physics and sensory equalizer calculation
fun calculateLabProfile(
    coffeeGrams: Float,
    waterMl: Int,
    ratio: Float,
    temperature: Int,
    grindClicks: Int,
    freshnessState: String,
    altitudeMeters: Int = 0,
    timeSeconds: Int = 180
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
    val tempEffective = minOf(temperature.toFloat(), tBoil)

    // Altitude efficiency factor for extraction
    val altitudeFactor = kotlin.math.sqrt(tBoil / 100.0f)

    // Extraction physics calculation
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

    // Freshness & qualitative badge tags
    val activeLabels = mutableListOf<String>()
    when {
        extractionIndex > 1.20f -> activeLabels.add("Alta Extracción")
        extractionIndex < 0.85f -> activeLabels.add("Sub-Extracción")
        else -> activeLabels.add("Ventana Óptima")
    }

    if (effectiveRatio < 13.0f) activeLabels.add("Cuerpo Denso")
    else if (effectiveRatio > 17.0f) activeLabels.add("Alta Claridad")

    if (tempEffective < 88.0f) activeLabels.add("Acidez Brillante")
    else if (tempEffective > 94.0f) activeLabels.add("Tono Tostado")

    if (timeSeconds < 105) activeLabels.add("Paso Rápido")
    else if (timeSeconds > 270) activeLabels.add("Contacto Prolongado")

    if (temperature > tBoil) {
        activeLabels.add("Hervor ${String.format(java.util.Locale.US, "%.1f", tBoil)}°C")
    } else if (altitudeMeters >= 1800) {
        activeLabels.add("Altitud ${altitudeMeters}m")
    }

    when (freshnessState) {
        "muy fresco" -> activeLabels.add("Bloom Largo")
        "en ventana", "punto ideal" -> activeLabels.add("Grano en Punto")
        "viejo" -> activeLabels.add("Desgasificado")
    }

    val summary = when {
        temperature > tBoil -> "A ${altitudeMeters} msnm el agua hierve a ${String.format(java.util.Locale.US, "%.1f", tBoil)}°C. La temperatura está acotada al hervor; muele más fino para potenciar extracción."
        finalBitterness >= 60 -> "Extracción intensa con perfil seco/amargo pronunciado; disminuye temperatura o engruesa la molienda."
        finalAcidity >= 68 && finalSweetness < 50 -> "Acidez dominante con sub-extracción; aumenta temperatura o afina la molienda."
        finalSweetness >= 65 && finalBitterness < 45 -> "Taza balanceada con dulzor redondo y acidez perfectamente integrada."
        finalBody >= 65 -> "Sensación táctil densa y untuosa con postgusto prolongado."
        finalBody <= 35 -> "Taza ligera y cristalina con marcada separación aromática."
        else -> "Perfil armónico y equilibrado con desarrollo limpio de sabores."
    }

    return LabFlavorProfile(
        aroma = finalAroma,
        acidity = finalAcidity,
        sweetness = finalSweetness,
        body = finalBody,
        bitterness = finalBitterness,
        finish = finalFinish,
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
    var isFahrenheit by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }

    var showRecipeDialog by remember { mutableStateOf(false) }
    var showTechniqueDialog by remember { mutableStateOf(false) }
    var inputRecipeName by remember { mutableStateOf("") }
    var inputTechniqueName by remember { mutableStateOf("") }

    var isAltitudePanelExpanded by remember { mutableStateOf(false) }
    var showCustomCityDialog by remember { mutableStateOf(false) }
    var customCityNameInput by remember { mutableStateOf("") }
    var customCityAltitudeInput by remember { mutableStateOf("") }

    val currentProfile = remember(
        state.labCoffee,
        state.labWater,
        state.labRatio,
        state.labTemp,
        state.labClicks,
        state.labBeanFreshness,
        state.labAltitudeMeters,
        state.labEstTimeSeconds
    ) {
        calculateLabProfile(
            coffeeGrams = state.labCoffee,
            waterMl = state.labWater,
            ratio = state.labRatio,
            temperature = state.labTemp,
            grindClicks = state.labClicks,
            freshnessState = state.labBeanFreshness,
            altitudeMeters = state.labAltitudeMeters,
            timeSeconds = state.labEstTimeSeconds
        )
    }

    if (showRecipeDialog) {
        AlertDialog(
            onDismissRequest = { showRecipeDialog = false },
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
                        val nameStr = inputRecipeName.ifBlank { "Receta Lab ${state.labMethod}" }
                        viewModel.saveLabAsRecipe(nameStr)
                        showRecipeDialog = false
                        inputRecipeName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal),
                    modifier = Modifier.testTag("recipe_submit_btn")
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecipeDialog = false }) {
                    Text("Cancelar", color = TextSecundario)
                }
            }
        )
    }

    if (showTechniqueDialog) {
        AlertDialog(
            onDismissRequest = { showTechniqueDialog = false },
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
                        val nameStr = inputTechniqueName.ifBlank { "Técnica Lab ${state.labMethod}" }
                        viewModel.saveLabAsTechnique(nameStr)
                        showTechniqueDialog = false
                        inputTechniqueName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal),
                    modifier = Modifier.testTag("technique_submit_btn")
                ) {
                    Text("Registrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTechniqueDialog = false }) {
                    Text("Cancelar", color = TextSecundario)
                }
            }
        )
    }

    if (showCustomCityDialog) {
        AlertDialog(
            onDismissRequest = { showCustomCityDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.LocationCity,
                        contentDescription = null,
                        tint = AcentoPrincipal,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Tu Ciudad y Altura", fontWeight = FontWeight.Bold, color = TextPrincipal, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Ingresa el nombre de tu ciudad y su elevación sobre el nivel del mar para calibrar el punto de ebullición exacto.",
                        fontSize = 12.5.sp,
                        color = TextSecundario
                    )
                    OutlinedTextField(
                        value = customCityNameInput,
                        onValueChange = { customCityNameInput = it },
                        label = { Text("Nombre de la ciudad") },
                        placeholder = { Text("Ej: Cusco, Denver, Manizales...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customCityAltitudeInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 5) {
                                customCityAltitudeInput = input
                            }
                        },
                        label = { Text("Altitud (msnm / metros)") },
                        placeholder = { Text("Ej: 2150") },
                        trailingIcon = { Text("msnm", fontSize = 12.sp, color = TextSecundario) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val altVal = customCityAltitudeInput.toIntOrNull()?.coerceIn(0, 5000) ?: 0
                        val nameVal = customCityNameInput.ifBlank { "Mi Ciudad" }
                        val displayStr = "$nameVal (${altVal}m)"
                        viewModel.updateLabVariables(altitudeMeters = altVal, cityName = displayStr)
                        showCustomCityDialog = false
                        isAltitudePanelExpanded = false
                        customCityNameInput = ""
                        customCityAltitudeInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal)
                ) {
                    Text("Guardar y Calibrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCityDialog = false }) {
                    Text("Cancelar", color = TextSecundario)
                }
            }
        )
    }

    if (showInfoSheet) {
        LabInfoSheet(onDismissRequest = { showInfoSheet = false })
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MainBackground)
    ) {
        // Atmospheric organic background blobs (depth & atmosphere)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val greenGlow = if (isDarkThemeGlobal) AcentoPrincipal.copy(alpha = 0.20f) else AcentoPrincipal.copy(alpha = 0.10f)
            val terracottaGlow = if (isDarkThemeGlobal) CafeCalidoOscuro.copy(alpha = 0.18f) else CafeCalidoOscuro.copy(alpha = 0.08f)

            drawCircle(
                color = greenGlow,
                radius = size.width * 0.50f,
                center = Offset(size.width * 0.82f, size.height * 0.08f)
            )
            drawCircle(
                color = terracottaGlow,
                radius = size.width * 0.45f,
                center = Offset(size.width * 0.18f, size.height * 0.85f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. AJUSTE DE ELEVACIÓN / ALTITUD & PUNTO DE EBULLICIÓN (PRIMER ELEMENTO DE LA PANTALLA)
            com.example.ui.screens.components.LabAltitudeHeaderCard(
                state = state,
                viewModel = viewModel,
                isFahrenheit = isFahrenheit,
                isExpanded = isAltitudePanelExpanded,
                onToggleExpand = { isAltitudePanelExpanded = !isAltitudePanelExpanded },
                onOpenCustomCityDialog = {
                    customCityNameInput = ""
                    customCityAltitudeInput = if (state.labAltitudeMeters > 0) state.labAltitudeMeters.toString() else ""
                    showCustomCityDialog = true
                }
            )

            // 2. SCREEN HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Laboratorio",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrincipal
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AcentoSuave)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = state.labMethod.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AcentoPrincipal
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Simulación y calibración sensorial.",
                        fontSize = 11.sp,
                        color = TextSecundario,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard)
                            .border(1.dp, BordeSuave, CircleShape)
                            .clickable { showInfoSheet = true }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Información",
                            tint = AcentoPrincipal,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 2. TARJETA SUPERIOR DE HIPÓTESIS (LEVEL 3 HERO)
            LabHypothesisCard(profile = currentProfile, state = state)

            // 3. BLOQUE CENTRAL — SENSORY MIXER (LEVEL 2 CARD WITH GRAIN)
            SensoryMixerCard(profile = currentProfile)

            // 4. CONTROL DOCK CAT TABS
            LabVariableGroupTabs(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            // 5. VARIABLE DOCK CONTROLS
            LabVariableDock(
                category = selectedCategory,
                state = state,
                viewModel = viewModel,
                isFahrenheit = isFahrenheit,
                onToggleFahrenheit = { isFahrenheit = it },
                onNavigateToSection = onNavigateToSection
            )
        }

        // --- 6. FIXED BOTTOM ACTION BAR DOCK ---
        LabActionBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onPrepareClick = {
                viewModel.playLabIdeaAsPrep()
                onNavigateToSection("brew")
            },
            onSaveExperimentClick = {
                viewModel.saveLabExperiment()
            },
            onSaveRecipeClick = {
                showRecipeDialog = true
            },
            onSaveTechniqueClick = {
                showTechniqueDialog = true
            }
        )
    }
}

@Composable
fun LabHypothesisCard(
    profile: LabFlavorProfile,
    state: com.example.ui.viewmodel.BaristaCalcState
) {
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
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
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
                        text = "PERFIL ESTIMADO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 0.8.sp
                    )

                    val primaryOutcomeLabel = when {
                        profile.bitterness > 65 -> "Intensa y con cuerpo"
                        profile.body < 38 -> "Estilo té, alta claridad"
                        profile.sweetness > 68 && profile.bitterness < 42 -> "Taza dorada y balanceada"
                        profile.acidity > 68 -> "Acidez brillante y frutal"
                        else -> "Taza equilibrada clásica"
                    }
                    Text(
                        text = primaryOutcomeLabel,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    LabHypothesisChips(labels = profile.labels.take(2))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = profile.summary,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LabHypothesisChips(labels: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
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
fun SensoryMixerCard(profile: LabFlavorProfile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(26.dp), spotColor = CafeCalidoOscuro.copy(alpha = 0.12f))
            .border(1.dp, BordeSuave, RoundedCornerShape(26.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ECUALIZADOR SENSORIAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecundario,
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = TextSecundario,
                    modifier = Modifier.size(16.dp)
                )
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
            .height(180.dp)
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
            text = "$value%",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )

        Box(
            modifier = Modifier
                .width(14.dp)
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MainBackgroundAlt.copy(alpha = 0.6f))
                .border(1.dp, BordeSuave, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedPercent)
                    .clip(RoundedCornerShape(8.dp))
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
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecundario,
            letterSpacing = 0.5.sp
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
                LabCategory.Proporcion -> "Ratio"
                LabCategory.Extraccion -> "Calor"
                LabCategory.Grano -> "Grano"
            }
            val icon = when (category) {
                LabCategory.Proporcion -> Icons.Default.Scale
                LabCategory.Extraccion -> Icons.Default.Thermostat
                LabCategory.Grano -> Icons.Default.Grass
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) AcentoPrincipal else Color.Transparent)
                    .clickable { onCategorySelected(category) }
                    .padding(vertical = 10.dp),
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
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
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

/**
 * Custom interactive calibrated slider with:
 * - Highlighted optimal/recommended zone with rounded pill indicator and micro-ticks
 * - Non-linear visual track expansion around the recommended range for surgical precision
 * - Material 3 tactile thumb with inner contrast core
 * - Built-in Haptic Feedback on value stepping and boundary crossing
 */
@Composable
fun LabCalibratedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    recommendedRange: ClosedFloatingPointRange<Float>?,
    step: Float = 1f,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var lastValueRef by remember { mutableStateOf(value) }
    var wasInRecommendedRef by remember { mutableStateOf(recommendedRange?.let { value in it } ?: false) }

    // Helper functions for non-linear compression/expansion
    fun valueToFraction(v: Float): Float {
        val clamped = v.coerceIn(range.start, range.endInclusive)
        if (recommendedRange == null) {
            return ((clamped - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
        }
        val rStart = recommendedRange.start
        val rEnd = recommendedRange.endInclusive
        val totalSpan = range.endInclusive - range.start
        val recSpan = rEnd - rStart

        // Give 50% of the visual track width to the recommended range
        val recTrackShare = 0.50f
        val leftTrackShare = 0.25f
        val rightTrackShare = 0.25f

        return when {
            clamped <= rStart -> {
                val subProg = if (rStart > range.start) (clamped - range.start) / (rStart - range.start) else 0f
                (subProg * leftTrackShare).coerceIn(0f, leftTrackShare)
            }
            clamped >= rEnd -> {
                val subProg = if (range.endInclusive > rEnd) (clamped - rEnd) / (range.endInclusive - rEnd) else 0f
                (leftTrackShare + recTrackShare + subProg * rightTrackShare).coerceIn(0f, 1f)
            }
            else -> {
                val subProg = if (recSpan > 0f) (clamped - rStart) / recSpan else 0f
                (leftTrackShare + subProg * recTrackShare).coerceIn(0f, 1f)
            }
        }
    }

    fun fractionToValue(f: Float): Float {
        val clampedF = f.coerceIn(0f, 1f)
        if (recommendedRange == null) {
            val raw = range.start + clampedF * (range.endInclusive - range.start)
            return if (step > 0f) Math.round(raw / step) * step else raw
        }
        val rStart = recommendedRange.start
        val rEnd = recommendedRange.endInclusive
        val recTrackShare = 0.50f
        val leftTrackShare = 0.25f
        val rightTrackShare = 0.25f

        val raw = when {
            clampedF <= leftTrackShare -> {
                val p = clampedF / leftTrackShare
                range.start + p * (rStart - range.start)
            }
            clampedF >= (leftTrackShare + recTrackShare) -> {
                val p = (clampedF - (leftTrackShare + recTrackShare)) / rightTrackShare
                rEnd + p * (range.endInclusive - rEnd)
            }
            else -> {
                val p = (clampedF - leftTrackShare) / recTrackShare
                rStart + p * (rEnd - rStart)
            }
        }
        val stepped = if (step > 0f) Math.round(raw / step) * step else raw
        return stepped.coerceIn(range.start, range.endInclusive)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(range, recommendedRange, step) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val newValue = fractionToValue(frac)
                    if (newValue != lastValueRef) {
                        lastValueRef = newValue
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onValueChange(newValue)
                    }
                }
            }
            .pointerInput(range, recommendedRange, step) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val newValue = fractionToValue(frac)
                        if (newValue != lastValueRef) {
                            lastValueRef = newValue
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onValueChange(newValue)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val newValue = fractionToValue(frac)
                        if (newValue != lastValueRef) {
                            lastValueRef = newValue
                            val inRec = recommendedRange?.let { newValue in it } ?: false
                            if (inRec != wasInRecommendedRef) {
                                wasInRecommendedRef = inRec
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            } else {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            }
                            onValueChange(newValue)
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val thumbFraction = valueToFraction(value)
        val thumbX = widthPx * thumbFraction

        // Canvas drawing background track, recommended range highlight, and ticks
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = 8.dp.toPx()
            val centerY = size.height / 2f
            val cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f, trackHeight / 2f)

            // 1. Inactive full base track
            drawRoundRect(
                color = if (isDarkThemeGlobal) Color(0xFF2B322E) else Color(0xFFE2DDD2),
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = androidx.compose.ui.geometry.Size(size.width, trackHeight),
                cornerRadius = cornerRadius
            )

            // 2. Highlight recommended optimal range if present
            if (recommendedRange != null) {
                val startFrac = valueToFraction(recommendedRange.start)
                val endFrac = valueToFraction(recommendedRange.endInclusive)
                val recLeft = size.width * startFrac
                val recRight = size.width * endFrac
                val recWidth = (recRight - recLeft).coerceAtLeast(4f)

                // Recommended range glowing pill background
                val recGlowColor = activeColor.copy(alpha = if (isDarkThemeGlobal) 0.35f else 0.22f)
                drawRoundRect(
                    color = recGlowColor,
                    topLeft = Offset(recLeft, centerY - (trackHeight + 6.dp.toPx()) / 2f),
                    size = androidx.compose.ui.geometry.Size(recWidth, trackHeight + 6.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )

                // Subdued border on recommended range
                drawRoundRect(
                    color = activeColor.copy(alpha = 0.55f),
                    topLeft = Offset(recLeft, centerY - (trackHeight + 6.dp.toPx()) / 2f),
                    size = androidx.compose.ui.geometry.Size(recWidth, trackHeight + 6.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )

                // Subtle calibration tick marks inside recommended range
                val tickSteps = 4
                for (i in 0..tickSteps) {
                    val tickFrac = startFrac + (endFrac - startFrac) * (i.toFloat() / tickSteps)
                    val tickX = size.width * tickFrac
                    drawLine(
                        color = activeColor.copy(alpha = 0.60f),
                        start = Offset(tickX, centerY - 8.dp.toPx()),
                        end = Offset(tickX, centerY + 8.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. Active progress track up to thumb position
            if (thumbX > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2f),
                    size = androidx.compose.ui.geometry.Size(thumbX.coerceIn(0f, size.width), trackHeight),
                    cornerRadius = cornerRadius
                )
            }
        }

        // 4. Custom Thumb Indicator with shadow and tactile inner ring
        Box(
            modifier = Modifier
                .offset(
                    x = with(androidx.compose.ui.platform.LocalDensity.current) {
                        (thumbX - 14.dp.toPx()).coerceIn(0f, widthPx - 28.dp.toPx()).toDp()
                    }
                )
                .size(28.dp)
                .shadow(elevation = 4.dp, shape = CircleShape, spotColor = activeColor.copy(alpha = 0.5f))
                .clip(CircleShape)
                .background(SurfaceCard)
                .border(3.dp, activeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Inner core dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(activeColor)
            )
        }
    }
}

@Composable
fun LabVariableDock(
    category: LabCategory,
    state: com.example.ui.viewmodel.BaristaCalcState,
    viewModel: BaristaCalcViewModel,
    isFahrenheit: Boolean,
    onToggleFahrenheit: (Boolean) -> Unit,
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
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (category) {
                LabCategory.Proporcion -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "RATIO & TIEMPO DE EXTRACCIÓN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecundario,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AcentoSuave)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "EQUILIBRIO SENSORIAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AcentoPrincipal
                            )
                        }
                    }
                    
                    // Slider 1: Ratio de Extracción (Proporción)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ratio de Proporción", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
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
                            range = 8f..22f,
                            recommendedRange = 15f..17f,
                            step = 0.5f,
                            activeColor = AcentoPrincipal
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1:8 (Intenso / Denso)", fontSize = 10.sp, color = TextSecundario)
                            Text("1:16 (Áureo)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AcentoPrincipal)
                            Text("1:22 (Ligero / Claridad)", fontSize = 10.sp, color = TextSecundario)
                        }
                    }

                    // Slider 2: Tiempo de Extracción
                    val timeSec = state.labEstTimeSeconds
                    val minutes = timeSec / 60
                    val seconds = timeSec % 60
                    val formattedTime = String.format(java.util.Locale.US, "%d:%02d min", minutes, seconds)

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tiempo de Extracción", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
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
                            range = 60f..360f,
                            recommendedRange = 135f..210f, // 2:15 - 3:30 min
                            step = 5f,
                            activeColor = AcentoPrincipal
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1:00 min (Rápido)", fontSize = 10.sp, color = TextSecundario)
                            Text("3:00 min (Estándar)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AcentoPrincipal)
                            Text("6:00 min (Lento)", fontSize = 10.sp, color = TextSecundario)
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecundario,
                            letterSpacing = 1.sp
                        )

                        // °C / °F Segmented Toggle Control
                        Row(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MainBackgroundAlt)
                                .border(1.dp, BordeSuave, RoundedCornerShape(14.dp))
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // °C button
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (!isFahrenheit) CafeCalidoClaro else Color.Transparent)
                                    .clickable { onToggleFahrenheit(false) }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "°C",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isFahrenheit) Color.White else TextSecundario
                                )
                            }

                            // °F button
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isFahrenheit) CafeCalidoClaro else Color.Transparent)
                                    .clickable { onToggleFahrenheit(true) }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "°F",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFahrenheit) Color.White else TextSecundario
                                )
                            }
                        }
                    }

                    // Slider 1: Temperatura del Agua
                    val isTempInOptimum = state.labTemp in 90..96
                    val displayTemp = if (isFahrenheit) {
                        val fVal = Math.round(state.labTemp * 9f / 5f + 32f)
                        "$fVal °F"
                    } else {
                        "${state.labTemp} °C"
                    }
                    val recRangeTempText = if (isFahrenheit) "194°F - 205°F" else "90°C - 96°C"

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Temperatura del Agua", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                                if (isTempInOptimum) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AcentoSuave)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text("ZONA ÓPTIMA", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AcentoPrincipal)
                                    }
                                }
                            }
                            Text(
                                text = displayTemp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CafeCalidoClaro
                            )
                        }

                        LabCalibratedSlider(
                            value = state.labTemp.toFloat(),
                            onValueChange = { viewModel.updateLabVariables(temperature = it.toInt()) },
                            range = 80f..98f,
                            recommendedRange = 90f..96f,
                            step = 1f,
                            activeColor = CafeCalidoClaro
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isFahrenheit) "176°F" else "80°C", fontSize = 10.sp, color = TextSecundario)
                            Text("Recomendado: $recRangeTempText", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CafeCalidoOscuro)
                            Text(if (isFahrenheit) "208°F" else "98°C", fontSize = 10.sp, color = TextSecundario)
                        }
                    }

                    // Slider 2: Clicks de Molienda
                    val isClicksInOptimum = state.labClicks in 18..26
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Clicks de Molienda", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                                if (isClicksInOptimum) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AcentoSuave)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text("FILTRADOS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AcentoPrincipal)
                                    }
                                }
                            }
                            Text(
                                text = "${state.labClicks} clicks",
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
                            recommendedRange = 18f..26f,
                            step = 1f,
                            activeColor = CafeCalidoClaro
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("6 (Espresso/Fino)", fontSize = 10.sp, color = TextSecundario)
                            Text("Recomendado: 18 - 26", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CafeCalidoOscuro)
                            Text("36 (Prensa/Grueso)", fontSize = 10.sp, color = TextSecundario)
                        }
                    }

                    // Live Educational Recommendation banner (connecting state.labRecommendationText)
                    if (state.labRecommendationText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AcentoSuave)
                                .border(1.dp, AcentoPrincipal.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AcentoPrincipal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = state.labRecommendationText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AcentoPrincipal,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
                LabCategory.Grano -> {
                    Text("ESTADO DEL GRANO Y FRESCURA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecundario, letterSpacing = 1.sp)
                    
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
    onSaveExperimentClick: () -> Unit,
    onSaveRecipeClick: () -> Unit,
    onSaveTechniqueClick: () -> Unit
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
                Text("Archivar", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
                    text = "Preparar Idea",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Box {
                IconButton(
                    onClick = { expandedMenu = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MainBackgroundAlt.copy(alpha = 0.5f))
                        .border(1.dp, BordeSuave, RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Más",
                        tint = TextPrincipal
                    )
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Guardar como Receta") },
                        onClick = {
                            expandedMenu = false
                            onSaveRecipeClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Guardar como Técnica") },
                        onClick = {
                            expandedMenu = false
                            onSaveTechniqueClick()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabInfoSheet(onDismissRequest: () -> Unit) {
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
                text = "CIENCIA DEL FILTRADO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AcentoPrincipal,
                letterSpacing = 1.sp
            )
            Text(
                text = "Guía Interactiva",
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrincipal
            )

            HorizontalDivider(color = BordeSuave, thickness = 1.dp)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("⚖️ Ratio y Proporción", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CafeCalidoOscuro)
                Text(
                    text = "El ratio define la intensidad. Ratios cortos (1:11) aportan cuerpo y potencia; ratios largos (1:18) aportan ligereza y claridad.",
                    fontSize = 12.sp,
                    color = TextSecundario,
                    lineHeight = 16.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🌡️ Temperatura y Molienda", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CafeCalidoOscuro)
                Text(
                    text = "Temperaturas altas y molienda fina aumentan la extracción y el riesgo de amargor. Molienda gruesa y agua tibia otorgan claridad.",
                    fontSize = 12.sp,
                    color = TextSecundario,
                    lineHeight = 16.sp
                )
            }

            Button(
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Entendido", fontWeight = FontWeight.Bold)
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
