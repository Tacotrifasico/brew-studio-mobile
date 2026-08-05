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
import androidx.compose.ui.graphics.lerp
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
    val labels: List<String>,
    val summary: String
)

// Heuristic engine to simulate coffee attributes dynamically
fun calculateLabProfile(
    coffeeGrams: Float,
    waterMl: Int,
    ratio: Float,
    temperature: Int,
    grindClicks: Int,
    freshnessState: String
): LabFlavorProfile {
    var aromaBase = 68
    var acidityBase = 62
    var sweetnessBase = 65
    var bodyBase = 58
    var bitternessBase = 32
    var finishBase = 64

    val activeLabels = mutableListOf<String>()

    // Ratio influence
    if (ratio < 12.0f) {
        bodyBase += 25
        bitternessBase += 12
        acidityBase -= 12
        sweetnessBase -= 8
        activeLabels.add("Más cuerpo")
        activeLabels.add("Alta intensidad")
    } else if (ratio in 14.0f..16.5f) {
        sweetnessBase += 15
        acidityBase += 8
        bodyBase += 5
        activeLabels.add("Ventana dorada")
    } else { // ratio > 16.5
        bodyBase -= 20
        acidityBase += 15
        sweetnessBase -= 5
        finishBase += 10
        activeLabels.add("Más ligera")
        activeLabels.add("Alta claridad")
    }

    // Temperature influence
    if (temperature > 94) {
        bitternessBase += 24
        bodyBase += 8
        acidityBase -= 14
        aromaBase += 8
        activeLabels.add("Mayor extracción")
    } else if (temperature in 90..94) {
        sweetnessBase += 10
        acidityBase += 10
    } else { // temperature < 88
        acidityBase += 18
        sweetnessBase -= 15
        bodyBase -= 15
        bitternessBase -= 18
        activeLabels.add("Sub-extracción")
    }

    // Grind clicks influence
    if (grindClicks < 14) {
        bodyBase += 18
        bitternessBase += 18
        sweetnessBase += 4
        acidityBase -= 8
        activeLabels.add("Molienda fina")
    } else if (grindClicks in 14..26) {
        sweetnessBase += 10
        finishBase += 8
    } else {
        bodyBase -= 22
        acidityBase += 12
        bitternessBase -= 12
        activeLabels.add("Molienda gruesa")
    }

    // Freshness influence
    when (freshnessState) {
        "muy fresco" -> {
            aromaBase += 14
            sweetnessBase -= 10
            activeLabels.add("Bloom largo")
        }
        "en ventana", "punto ideal" -> {
            sweetnessBase += 16
            aromaBase += 12
            activeLabels.add("Taza balanceada")
        }
        "bajando" -> {
            aromaBase -= 8
            sweetnessBase -= 4
        }
        "viejo" -> {
            aromaBase -= 25
            sweetnessBase -= 22
            bodyBase -= 6
            activeLabels.add("Aroma bajo")
        }
    }

    val finalAroma = aromaBase.coerceIn(8, 98)
    val finalAcidity = acidityBase.coerceIn(8, 98)
    val finalSweetness = sweetnessBase.coerceIn(8, 98)
    val finalBody = bodyBase.coerceIn(8, 98)
    val finalBitterness = bitternessBase.coerceIn(8, 98)
    val finalFinish = finishBase.coerceIn(8, 98)

    val summary = when {
        finalBitterness > 65 -> "Infusión robusta e intensa, con riesgo de amargor marcado."
        finalBody < 35 -> "Taza ligera estilo té y alta claridad, con riesgo de sub-extracción."
        finalSweetness > 70 && finalBitterness < 45 -> "Taza redonda, dulce y con cuerpo aterciopelado óptimo."
        finalAcidity > 70 && finalSweetness < 55 -> "Acidez chispeante y nítida."
        else -> "Infusión equilibrada de cuerpo medio y excelente armonía."
    }

    return LabFlavorProfile(
        aroma = finalAroma,
        acidity = finalAcidity,
        sweetness = finalSweetness,
        body = finalBody,
        bitterness = finalBitterness,
        finish = finalFinish,
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

    var selectedCategory by remember { mutableStateOf(LabCategory.Proporcion) }
    var showInfoSheet by remember { mutableStateOf(false) }

    var showRecipeDialog by remember { mutableStateOf(false) }
    var showTechniqueDialog by remember { mutableStateOf(false) }
    var inputRecipeName by remember { mutableStateOf("") }
    var inputTechniqueName by remember { mutableStateOf("") }

    val currentProfile = remember(
        state.labCoffee,
        state.labWater,
        state.labRatio,
        state.labTemp,
        state.labClicks,
        state.labBeanFreshness
    ) {
        calculateLabProfile(
            coffeeGrams = state.labCoffee,
            waterMl = state.labWater,
            ratio = state.labRatio,
            temperature = state.labTemp,
            grindClicks = state.labClicks,
            freshnessState = state.labBeanFreshness
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
            // 1. SCREEN HEADER
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

            // --- LECTURA ACTIVA DE CATA BANNER (LEVEL 2 CARD) ---
            if (state.selectedFoundNotes.isNotBlank() || state.cataTexture.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp), spotColor = CafeCalidoOscuro.copy(alpha = 0.12f))
                        .border(1.dp, BordeSuave, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalCafe,
                                contentDescription = null,
                                tint = CafeCalidoOscuro,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lectura activa de Cata",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CafeCalidoOscuro
                            )
                        }
                        Text(
                            text = "Notas: ${state.selectedFoundNotes.ifEmpty { "vacío" }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrincipal
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

@Composable
fun LabVariableDock(
    category: LabCategory,
    state: com.example.ui.viewmodel.BaristaCalcState,
    viewModel: BaristaCalcViewModel,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (category) {
                LabCategory.Proporcion -> {
                    Text("CONTROL DE PROPORCIÓN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecundario, letterSpacing = 1.sp)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dosis de Café", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                            Text("${String.format("%.1f", state.labCoffee)} g", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AcentoPrincipal)
                        }
                        Slider(
                            value = state.labCoffee,
                            onValueChange = { viewModel.updateLabVariables(coffee = it) },
                            valueRange = 10f..35f,
                            colors = SliderDefaults.colors(thumbColor = AcentoPrincipal, activeTrackColor = AcentoPrincipal)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ratio de Extracción", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                            Text("1:${String.format("%.1f", state.labRatio)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AcentoPrincipal)
                        }
                        Slider(
                            value = state.labRatio,
                            onValueChange = { viewModel.updateLabVariables(ratio = it) },
                            valueRange = 8f..22f,
                            colors = SliderDefaults.colors(thumbColor = AcentoPrincipal, activeTrackColor = AcentoPrincipal)
                        )
                    }
                }
                LabCategory.Extraccion -> {
                    Text("CONTROL DE CALOR Y MOLIENDA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecundario, letterSpacing = 1.sp)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Temperatura del Agua", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                            Text("${state.labTemp} °C", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CafeCalidoClaro)
                        }
                        Slider(
                            value = state.labTemp.toFloat(),
                            onValueChange = { viewModel.updateLabVariables(temperature = it.toInt()) },
                            valueRange = 80f..98f,
                            colors = SliderDefaults.colors(thumbColor = CafeCalidoClaro, activeTrackColor = CafeCalidoClaro)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Clicks de Molienda", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                            Text("${state.labClicks} clicks", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CafeCalidoClaro)
                        }
                        Slider(
                            value = state.labClicks.toFloat(),
                            onValueChange = { viewModel.updateLabVariables(clicks = it.toInt()) },
                            valueRange = 6f..36f,
                            colors = SliderDefaults.colors(thumbColor = CafeCalidoClaro, activeTrackColor = CafeCalidoClaro)
                        )
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

            Divider(color = BordeSuave, thickness = 1.dp)

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
