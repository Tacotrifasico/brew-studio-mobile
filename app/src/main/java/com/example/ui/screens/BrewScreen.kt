package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Technique
import com.example.data.database.TechniqueStep
import com.example.data.validation.BrewInputRules
import com.example.ui.theme.*
import com.example.ui.components.*
import com.example.ui.viewmodel.BaristaCalcViewModel
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BrewScreen(
    viewModel: BaristaCalcViewModel,
    onNavigateToCata: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var isCreatingCustom by remember { mutableStateOf(false) }
    LaunchedEffect(state.ownerScopeKey) { isCreatingCustom = false }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MainBackground)
    ) {
        // Atmospheric organic background blobs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val greenGlow = if (isDarkThemeGlobal) AcentoPrincipal.copy(alpha = 0.20f) else AcentoPrincipal.copy(alpha = 0.10f)
            val terracottaGlow = if (isDarkThemeGlobal) CafeCalidoOscuro.copy(alpha = 0.18f) else CafeCalidoOscuro.copy(alpha = 0.08f)

            drawCircle(
                color = terracottaGlow,
                radius = size.width * 0.52f,
                center = Offset(size.width * 0.85f, size.height * 0.08f)
            )
            drawCircle(
                color = greenGlow,
                radius = size.width * 0.45f,
                center = Offset(size.width * 0.15f, size.height * 0.82f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // --- SCREEN TITLE ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SECUENCIA DE EXTRACCIÓN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = TextSecundario
                )
                Text(
                    text = "Preparar Café",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrincipal
                )
            }

            if (!state.timerRunning && !state.preparationCompleted) {
                Button(
                    onClick = { isCreatingCustom = !isCreatingCustom },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isCreatingCustom) Advertencia else AcentoPrincipal),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isCreatingCustom) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Crear",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCreatingCustom) "Cancelar" else "Nueva Técnica",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AnimatedContent(
            targetState = state.timerRunning || state.preparationCompleted,
            transitionSpec = {
                fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
            },
            label = "timerState"
        ) { showingExecution ->
            if (showingExecution) {
                // ACTIVE EXTRACTOR TIMER DISPLAY
                ActiveBrewTimerView(viewModel = viewModel, state = state, onNavigateToCata = onNavigateToCata)
            } else if (isCreatingCustom) {
                // CREATION FORM VIEW
                key(state.ownerScopeKey) {
                    CreateTechniqueFormView(
                        viewModel = viewModel,
                        onDone = { isCreatingCustom = false }
                    )
                }
            } else {
                // DEFAULT SETUP SCREEN WITH LIBRARY PICKERS
                key(state.ownerScopeKey) { BrewSetupView(viewModel = viewModel, state = state) }
            }
        }
      }
    }
}

@Composable
fun BrewSetupView(
    viewModel: BaristaCalcViewModel,
    state: com.example.ui.viewmodel.BaristaCalcState
) {
    val scrollState = rememberScrollState()
    var showAddMethodDialog by remember { mutableStateOf(false) }
    var newMethodName by remember { mutableStateOf("") }
    var newMethodRatio by remember { mutableStateOf("16") }
    val activeMethodId = viewModel.methodIdForName(state.activePrepMethod)
    val matchingTechniques = state.techniquesList.filter { it.methodId == activeMethodId }

    if (showAddMethodDialog) {
        AlertDialog(
            onDismissRequest = { showAddMethodDialog = false },
            title = { Text("Agregar método", fontWeight = FontWeight.Bold, color = TextPrincipal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("El método aparecerá en la calculadora y en Preparar café con tres técnicas iniciales.", fontSize = 12.sp, color = TextSecundario)
                    StyledOutlinedTextField(value = newMethodName, onValueChange = { newMethodName = it }, label = "Nombre", placeholder = "Ej. Kalita Wave")
                    StyledOutlinedTextField(value = newMethodRatio, onValueChange = { newMethodRatio = it }, label = "Proporción inicial 1:", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            },
            confirmButton = {
                StyledPrimaryButton(
                    text = "Agregar y seleccionar",
                    onClick = {
                        viewModel.addCustomMethod(newMethodName, newMethodRatio.replace(',', '.').toFloatOrNull() ?: 16f) { _ ->
                            showAddMethodDialog = false
                            newMethodName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = { TextButton(onClick = { showAddMethodDialog = false }) { Text("Cancelar", color = TextSecundario) } },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(22.dp)
        )
    }

    val (c1, c2) = when {
        state.activePrepRatio <= 6f -> Pair(Color(0xFF3D2817), Color(0xFF7A3B2E))
        state.activePrepRatio <= 12f -> Pair(Color(0xFF4A3728), Color(0xFFA85D3F))
        state.activePrepRatio <= 15f -> Pair(Color(0xFF2D4A3E), Color(0xFFB5714A))
        else -> Pair(Color(0xFF3D5E4F), Color(0xFF6B9080))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active profile configuration card (Hero level 3)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = c1.copy(alpha = 0.45f),
                    ambientColor = c2.copy(alpha = 0.2f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.12f), c1, c2),
                        start = Offset(0f, 0f),
                        end = Offset(700f, 700f)
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = size.width * 0.48f,
                    center = Offset(size.width * 0.88f, size.height * 0.15f)
                )
                val grainColor = Color.White.copy(alpha = 0.045f)
                for (i in 0 until 50) {
                    val px = (i * 31.7f) % size.width
                    val py = (i * 17.1f) % size.height
                    drawCircle(color = grainColor, radius = 1.3f, center = Offset(px, py))
                }
            }

            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = V60Icon,
                        contentDescription = "Timer",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "CONFIGURACIÓN ACTIVA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Text(
                    text = state.activePrepTechniqueName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Parameters summary row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CAFÉ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                        Text(
                            text = "${state.activePrepCoffee} g",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Box(modifier = Modifier.size(1.dp, 20.dp).background(Color.White.copy(alpha = 0.3f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PROPORCIÓN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                        Text(
                            text = "1:${state.activePrepRatio}",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Box(modifier = Modifier.size(1.dp, 20.dp).background(Color.White.copy(alpha = 0.3f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AGUA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                        Text(
                            text = "${state.activePrepWater} ml",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // Play Button
                Button(
                    onClick = { viewModel.startTimer() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Iniciar", tint = c1)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Iniciar Extracción", fontSize = 14.sp, fontWeight = FontWeight.Black, color = c1)
                }
            }
        }

        if (state.activePrepSteps.isNotEmpty()) {
            TechniqueStepsOverview(
                techniqueName = state.activePrepTechniqueName,
                steps = state.activePrepSteps
            )
        }

        // Techniques library section
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "TÉCNICAS PARA ${state.activePrepMethod.uppercase(Locale.getDefault())}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecundario,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = { showAddMethodDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Método", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (matchingTechniques.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BordeSuave, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AcentoSuave),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = V60Icon,
                                contentDescription = null,
                                tint = AcentoPrincipal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Aún no hay técnicas para ${state.activePrepMethod}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrincipal
                        )
                        Text(
                            text = "Crea una técnica propia o agrega otro método.",
                            fontSize = 12.sp,
                            color = TextSecundario,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                matchingTechniques.forEach { tech ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .shadow(3.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.04f))
                            .border(1.dp, BordeSuave, RoundedCornerShape(20.dp))
                            .clickable { viewModel.loadPrepTechnique(tech.id) },
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CafeCalidoClaro.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = CafeCalidoClaro,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tech.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                                Text("Usará ${state.activePrepCoffee} g • ${state.activePrepWater} ml • 1:${state.activePrepRatio}", fontSize = 11.sp, color = TextSecundario)
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Seleccionar técnica ${tech.name}",
                                tint = TextSecundario,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TechniqueStepsOverview(
    techniqueName: String,
    steps: List<TechniqueStep>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BordeSuave, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "TÉCNICA COMPLETA · ${steps.size} PASOS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = AcentoPrincipal
                )
                Text(techniqueName, fontSize = 17.sp, fontWeight = FontWeight.Black, color = TextPrincipal)
                Text(
                    "Revísala antes de iniciar. “Meta en báscula” es el total que debe marcar al terminar cada paso.",
                    fontSize = 11.sp,
                    color = TextSecundario,
                    lineHeight = 15.sp
                )
            }
            steps.forEachIndexed { index, step ->
                if (index > 0) HorizontalDivider(color = BordeSuave.copy(alpha = 0.65f))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(AcentoPrincipal),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(step.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                    }
                    PreparationStepMetrics(step = step)
                    if (step.stepNote.isNotBlank()) {
                        Text(step.stepNote, fontSize = 11.sp, color = TextSecundario, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreparationStepMetrics(step: TechniqueStep) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    if (largeText) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            PreparationMetric("AGREGA AHORA", "+${step.waterAddedMl} ml", CafeCalidoOscuro, Modifier.fillMaxWidth())
            PreparationMetric("META EN BÁSCULA", "${step.waterAccumulatedMl} ml", AcentoPrincipal, Modifier.fillMaxWidth())
            PreparationMetric("TIEMPO", formatStepDuration(step.durationSeconds), AccentGold, Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            PreparationMetric("AGREGA AHORA", "+${step.waterAddedMl} ml", CafeCalidoOscuro, Modifier.weight(1f))
            PreparationMetric("META EN BÁSCULA", "${step.waterAccumulatedMl} ml", AcentoPrincipal, Modifier.weight(1f))
            PreparationMetric("TIEMPO", formatStepDuration(step.durationSeconds), AccentGold, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PreparationMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" }
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .padding(horizontal = 7.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = TextPrincipal, maxLines = 1)
    }
}

private fun formatStepDuration(seconds: Int): String =
    if (seconds < 60) "${seconds} s" else String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)

@Composable
fun ActiveBrewTimerView(
    viewModel: BaristaCalcViewModel,
    state: com.example.ui.viewmodel.BaristaCalcState,
    onNavigateToCata: () -> Unit
) {
    val steps = state.activePrepSteps
    val currentIndex = state.activeStepIndex
    val activeStep = steps.getOrNull(currentIndex)
    
    // Calculate values for current step duration and total progress
    val elapsedTotal = state.elapsedSeconds
    val previousStepsDurationSum = steps.take(currentIndex).sumOf { it.durationSeconds }
    val elapsedCurrentStep = elapsedTotal - previousStepsDurationSum
    val currentStepDuration = activeStep?.durationSeconds ?: 45
    val remainingCurrentStep = (currentStepDuration - elapsedCurrentStep).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Big Time Board Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BordeSuave, RoundedCornerShape(32.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TIEMPO RESTANTE PASO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecundario
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d", remainingCurrentStep / 60, remainingCurrentStep % 60),
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        color = AcentoPrincipal
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "AHORA · ${activeStep?.title ?: "Derrame de equilibrio"}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrincipal
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = activeStep?.stepNote ?: "Vierte agua suavemente para completar.",
                        fontSize = 13.sp,
                        color = TextSecundario,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    activeStep?.let { PreparationStepMetrics(step = it) }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Time total progress indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cronómetro Total",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecundario
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", elapsedTotal / 60, elapsedTotal % 60),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrincipal
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Player controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousStep() },
                            enabled = !state.preparationCompleted && currentIndex > 0,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF2F7F3))
                        ) {
                            Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Atras", tint = TextPrincipal)
                        }

                        if (state.preparationCompleted) {
                            Row(
                                modifier = Modifier
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AcentoPrincipal.copy(alpha = 0.14f))
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AcentoPrincipal)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Finalizada", color = AcentoPrincipal, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (state.timerPaused) viewModel.resumeTimer() else viewModel.pauseTimer()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.timerPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (state.timerPaused) "Reanudar" else "Pausar"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (state.timerPaused) "Reanudar" else "Pausar", fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = { viewModel.advanceStep() },
                            enabled = !state.preparationCompleted,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF2F7F3))
                        ) {
                            Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Siguiente", tint = TextPrincipal)
                        }
                    }
                }
            }
        }

        // Active/Pending Sequence List
        item {
            Text(
                text = "SECUENCIA DE PASOS EXTRACCIÓN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecundario,
                letterSpacing = 1.sp
            )
        }

        itemsIndexed(steps) { idx, step ->
            val isCurrent = idx == currentIndex
            val isPast = idx < currentIndex
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isCurrent) 1.5.dp else 1.dp,
                        color = if (isCurrent) AcentoPrincipal else if (isPast) Color.Transparent else BordeSuave.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) Color(0xFFF0FAF5) else if (isPast) Color(0x33BFCFC6) else SurfaceCard
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrent) AcentoPrincipal else if (isPast) AcentoSecundario else TextSecundario.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = step.stepNumber.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent || isPast) Color.White else TextSecundario
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = step.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) TextPrincipal else if (isPast) TextSecundario else TextPrincipal.copy(alpha = 0.8f),
                                modifier = Modifier.weight(1f)
                            )
                            if (isCurrent) {
                                Text(
                                    "ACTIVO",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AcentoPrincipal,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AcentoPrincipal.copy(alpha = 0.15f))
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        PreparationStepMetrics(step = step)
                        if (step.stepNote.isNotBlank()) {
                            Text(step.stepNote, fontSize = 10.sp, color = TextSecundario, lineHeight = 14.sp)
                        }
                    }
                }
            }
        }

        // Finish action button
        item {
            Button(
                onClick = {
                    viewModel.stopTimer()
                    onNavigateToCata()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Advertencia),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Completado")
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (state.preparationCompleted) "Continuar a Cata" else "Completar extracción e ir a Cata", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CreateTechniqueFormView(
    viewModel: BaristaCalcViewModel,
    onDone: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    var name by rememberSaveable { mutableStateOf("") }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    
    // Method Picker state
    val availableMethods = if (state.userMethods.isNotEmpty()) state.userMethods else listOf(
        com.example.data.domain.UserMethodItem("1", "11111111-1111-4000-8000-000000000001", "V60", "v60"),
        com.example.data.domain.UserMethodItem("2", "11111111-1111-4000-8000-000000000002", "AeroPress", "aeropress"),
        com.example.data.domain.UserMethodItem("3", "11111111-1111-4000-8000-000000000003", "Espresso", "espresso"),
        com.example.data.domain.UserMethodItem("4", "11111111-1111-4000-8000-000000000004", "Prensa francesa", "french_press"),
        com.example.data.domain.UserMethodItem("5", "11111111-1111-4000-8000-000000000006", "Chemex", "chemex"),
        com.example.data.domain.UserMethodItem("6", "11111111-1111-4000-8000-000000000007", "Moka", "moka"),
        com.example.data.domain.UserMethodItem("7", "11111111-1111-4000-8000-000000000008", "Cold brew", "cold_brew")
    )
    var selectedMethodId by rememberSaveable { mutableStateOf(availableMethods.firstOrNull()?.methodId ?: "11111111-1111-4000-8000-000000000001") }
    var methodDropdownExpanded by remember { mutableStateOf(false) }
    var showAddMethodDialog by remember { mutableStateOf(false) }
    var newMethodInputName by remember { mutableStateOf("") }

    var coffee by rememberSaveable { mutableStateOf("15.0") }
    var water by rememberSaveable { mutableStateOf("240") }
    var temp by rememberSaveable { mutableStateOf("93") }

    // Grinder Picker state
    val grinders = state.grindersList.ifEmpty { state.equipmentList.filter { it.type == "GRINDER" } }
    var selectedGrinderId by remember { mutableStateOf<String?>(grinders.firstOrNull()?.id) }
    var selectedGrinderName by remember { mutableStateOf(grinders.firstOrNull()?.name ?: "Molino Manual") }
    var grinderDropdownExpanded by remember { mutableStateOf(false) }
    var showAddGrinderDialog by remember { mutableStateOf(false) }

    var clicks by rememberSaveable { mutableStateOf("24") }
    var notes by rememberSaveable { mutableStateOf("") }

    // Step lists
    var stepTitle1 by rememberSaveable { mutableStateOf("Preinfusión Bloom") }
    var stepTime1 by rememberSaveable { mutableStateOf("35") }
    var stepWater1 by rememberSaveable { mutableStateOf("50") }

    var stepTitle2 by rememberSaveable { mutableStateOf("Primer Vertido") }
    var stepTime2 by rememberSaveable { mutableStateOf("45") }
    var stepWater2 by rememberSaveable { mutableStateOf("90") }

    var stepTitle3 by rememberSaveable { mutableStateOf("Segundo Vertido") }
    var stepTime3 by rememberSaveable { mutableStateOf("40") }
    var stepWater3 by rememberSaveable { mutableStateOf("100") }

    val scrollState = rememberScrollState()

    // Calculated summary preview
    val parsedCoffee = coffee.replace(',', '.').trim().toFloatOrNull() ?: 15.0f
    val parsedStepW1 = stepWater1.replace(',', '.').trim().toIntOrNull() ?: 0
    val parsedStepW2 = stepWater2.replace(',', '.').trim().toIntOrNull() ?: 0
    val parsedStepW3 = stepWater3.replace(',', '.').trim().toIntOrNull() ?: 0
    val stepWaterSum = parsedStepW1 + parsedStepW2 + parsedStepW3
    val parsedWater = if (stepWaterSum > 0) stepWaterSum else (water.replace(',', '.').trim().toIntOrNull() ?: 240)
    
    val rawRatio = if (parsedCoffee > 0f) parsedWater / parsedCoffee else 16.0f
    val calculatedRatio = if (rawRatio % 1.0f == 0.0f) {
        String.format(Locale.US, "%.0f", rawRatio)
    } else {
        String.format(Locale.US, "%.1f", rawRatio)
    }
    val stepTitles = listOf(stepTitle1, stepTitle2, stepTitle3)
    val stepTimes = listOf(stepTime1, stepTime2, stepTime3).map { it.replace(',', '.').trim().toIntOrNull() }
    val stepWaters = listOf(stepWater1, stepWater2, stepWater3).map { it.replace(',', '.').trim().toIntOrNull() }
    val validationError = BrewInputRules.techniqueError(
        name = name,
        coffee = coffee.replace(',', '.').trim().toFloatOrNull(),
        temperature = temp.replace(',', '.').trim().toIntOrNull(),
        stepTitles = stepTitles,
        stepDurations = stepTimes,
        stepWaters = stepWaters
    )

    if (showAddGrinderDialog) {
        var newBrand by remember { mutableStateOf("") }
        var newModel by remember { mutableStateOf("") }
        var newClicks by remember { mutableStateOf("20") }
        var newNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddGrinderDialog = false },
            title = { Text("Agregar Molino Nuevo", fontWeight = FontWeight.Bold, color = TextPrincipal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StyledOutlinedTextField(
                        value = newBrand,
                        onValueChange = { newBrand = it },
                        label = "Marca (ej. Comandante, Timemore)"
                    )
                    StyledOutlinedTextField(
                        value = newModel,
                        onValueChange = { newModel = it },
                        label = "Modelo (ej. C40, Chestnut C3)"
                    )
                    StyledOutlinedTextField(
                        value = newNotes,
                        onValueChange = { newNotes = it },
                        label = "Notas de calibración"
                    )
                }
            },
            confirmButton = {
                StyledPrimaryButton(
                    text = "Guardar y Seleccionar",
                    onClick = {
                        if (newModel.isNotBlank() || newBrand.isNotBlank()) {
                            viewModel.addGrinder(newBrand, newModel, newClicks, newNotes, onCreated = { createdInst ->
                                selectedGrinderId = createdInst.id
                                selectedGrinderName = createdInst.name
                                showAddGrinderDialog = false
                            })
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddGrinderDialog = false }) {
                    Text("Cancelar", color = TextSecundario)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(22.dp)
        )
    }

    FormAtmosphereBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with ambient glow blob
            FormHeaderWithBlob(
                title = "Preparar Café",
                subtitle = "Crear nueva técnica de extracción con temporizador guiado",
                icon = V60Icon,
                onClose = onDone
            )

            // Hero Card Level 3: Ratio & Water Summary Preview
            FormHeroCard(
                title = "Relación de Extracción Calculada",
                primaryValue = "1:$calculatedRatio",
                secondaryValue = "$parsedWater ml total",
                details = listOf(
                    "Dosis" to "$parsedCoffee g",
                    "Temperatura" to "$temp °C",
                    "Molino" to selectedGrinderName
                )
            )

            // Group 1: General Info Sub-Card Level 2
            FormSubCard(
                title = "Datos Generales de la Técnica",
                titleIcon = Icons.Default.Tune
            ) {
                StyledOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre de la técnica",
                    placeholder = "ej. V60 Receta de Competencia 2026",
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Method Dropdown Selector
                    Box(modifier = Modifier.weight(1.4f)) {
                        val currentMethod = availableMethods.find { it.methodId == selectedMethodId }
                        StyledOutlinedTextField(
                            value = currentMethod?.name ?: "V60",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = "Método",
                            trailingIcon = {
                                IconButton(onClick = { methodDropdownExpanded = !methodDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleccionar método")
                                }
                            },
                            modifier = Modifier.clickable { methodDropdownExpanded = !methodDropdownExpanded }
                        )
                        DropdownMenu(
                            expanded = methodDropdownExpanded,
                            onDismissRequest = { methodDropdownExpanded = false },
                            modifier = Modifier.heightIn(max = 260.dp)
                        ) {
                            availableMethods.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.name, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedMethodId = m.methodId
                                        methodDropdownExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = AcentoPrincipal, modifier = Modifier.size(16.dp))
                                        Text("Nuevo Método...", fontWeight = FontWeight.Bold, color = AcentoPrincipal)
                                    }
                                },
                                onClick = {
                                    methodDropdownExpanded = false
                                    showAddMethodDialog = true
                                }
                            )
                        }
                    }

                    if (showAddMethodDialog) {
                        AlertDialog(
                            onDismissRequest = { showAddMethodDialog = false },
                            title = { Text("Nuevo Método de Extracción", fontWeight = FontWeight.Bold, color = TextPrincipal) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Ingresa el nombre de tu nuevo extractor o cafetera. Se guardará en tu Almacén y estará disponible en toda la app:", fontSize = 12.sp, color = TextSecundario)
                                    StyledOutlinedTextField(
                                        value = newMethodInputName,
                                        onValueChange = { newMethodInputName = it },
                                        label = "Nombre del método",
                                        placeholder = "ej. Kalita Wave, V60 Switch, Tricolate"
                                    )
                                }
                            },
                            confirmButton = {
                                StyledPrimaryButton(
                                    text = "Guardar en Almacén",
                                    onClick = {
                                        if (newMethodInputName.isNotBlank()) {
                                            viewModel.addCustomMethod(newMethodInputName) { createdMethod ->
                                                selectedMethodId = createdMethod.id
                                                showAddMethodDialog = false
                                                newMethodInputName = ""
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddMethodDialog = false }) {
                                    Text("Cancelar", color = TextSecundario)
                                }
                            },
                            containerColor = SurfaceCard,
                            shape = RoundedCornerShape(22.dp)
                        )
                    }

                    StyledOutlinedTextField(
                        value = temp,
                        onValueChange = { temp = it },
                        label = "Temperatura (°C)",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Group 2: Dose, Water & Grinder Sub-Card Level 2
            FormSubCard(
                title = "Dosis, Agua y Molienda",
                titleIcon = Icons.Default.Scale
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StyledOutlinedTextField(
                        value = coffee,
                        onValueChange = { coffee = it },
                        label = "Café (g)",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    StyledOutlinedTextField(
                        value = water,
                        onValueChange = {
                            water = it
                            val totalW = it.replace(',', '.').trim().toIntOrNull()
                            if (totalW != null && totalW > 0) {
                                val w1 = stepWater1.replace(',', '.').trim().toIntOrNull() ?: 50
                                val remaining = (totalW - w1).coerceAtLeast(0)
                                val w2 = remaining / 2
                                val w3 = remaining - w2
                                stepWater2 = w2.toString()
                                stepWater3 = w3.toString()
                            }
                        },
                        label = "Agua Total (ml)",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Grinder Dropdown Selector
                    Box(modifier = Modifier.weight(1.5f)) {
                        StyledOutlinedTextField(
                            value = selectedGrinderName,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = "Molino de Café",
                            trailingIcon = {
                                IconButton(onClick = { grinderDropdownExpanded = !grinderDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleccionar molino")
                                }
                            },
                            modifier = Modifier.clickable { grinderDropdownExpanded = !grinderDropdownExpanded }
                        )
                        DropdownMenu(
                            expanded = grinderDropdownExpanded,
                            onDismissRequest = { grinderDropdownExpanded = false },
                            modifier = Modifier.heightIn(max = 260.dp)
                        ) {
                            grinders.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g.name, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        selectedGrinderId = g.id
                                        selectedGrinderName = g.name
                                        grinderDropdownExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("+ Agregar molino nuevo", color = AccentGold, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    grinderDropdownExpanded = false
                                    showAddGrinderDialog = true
                                }
                            )
                        }
                    }

                    StyledOutlinedTextField(
                        value = clicks,
                        onValueChange = { clicks = it },
                        label = "Clics",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                StyledOutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Notas de Calibración / Molido",
                    placeholder = "ej. Molienda media fina, verter suavemente",
                    maxLines = 2
                )
            }

            // Group 3: Steps Structure Sub-Card Level 2
            FormSubCard(
                title = "Estructura de Pasos Guiados",
                titleIcon = Icons.Default.Timer
            ) {
                // Step 1
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Paso 1 · Preinfusión (Bloom)", fontSize = 12.sp, color = AcentoPrincipal, fontWeight = FontWeight.Bold)
                    StyledOutlinedTextField(
                        value = stepTitle1,
                        onValueChange = { stepTitle1 = it },
                        label = "Título del Paso 1",
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyledOutlinedTextField(
                            value = stepTime1,
                            onValueChange = { stepTime1 = it },
                            label = "Segundos",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        StyledOutlinedTextField(
                            value = stepWater1,
                            onValueChange = {
                                stepWater1 = it
                                val s1 = it.replace(',', '.').trim().toIntOrNull() ?: 0
                                val s2 = stepWater2.replace(',', '.').trim().toIntOrNull() ?: 0
                                val s3 = stepWater3.replace(',', '.').trim().toIntOrNull() ?: 0
                                if (s1 + s2 + s3 > 0) water = (s1 + s2 + s3).toString()
                            },
                            label = "Agua (ml)",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = BordeSuave)

                // Step 2
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Paso 2 · Extracción Principal", fontSize = 12.sp, color = AcentoPrincipal, fontWeight = FontWeight.Bold)
                    StyledOutlinedTextField(
                        value = stepTitle2,
                        onValueChange = { stepTitle2 = it },
                        label = "Título del Paso 2",
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyledOutlinedTextField(
                            value = stepTime2,
                            onValueChange = { stepTime2 = it },
                            label = "Segundos",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        StyledOutlinedTextField(
                            value = stepWater2,
                            onValueChange = {
                                stepWater2 = it
                                val s1 = stepWater1.replace(',', '.').trim().toIntOrNull() ?: 0
                                val s2 = it.replace(',', '.').trim().toIntOrNull() ?: 0
                                val s3 = stepWater3.replace(',', '.').trim().toIntOrNull() ?: 0
                                if (s1 + s2 + s3 > 0) water = (s1 + s2 + s3).toString()
                            },
                            label = "Agua (ml)",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = BordeSuave)

                // Step 3
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Paso 3 · Decantación Final", fontSize = 12.sp, color = AcentoPrincipal, fontWeight = FontWeight.Bold)
                    StyledOutlinedTextField(
                        value = stepTitle3,
                        onValueChange = { stepTitle3 = it },
                        label = "Título del Paso 3",
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyledOutlinedTextField(
                            value = stepTime3,
                            onValueChange = { stepTime3 = it },
                            label = "Segundos",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        StyledOutlinedTextField(
                            value = stepWater3,
                            onValueChange = {
                                stepWater3 = it
                                val s1 = stepWater1.replace(',', '.').trim().toIntOrNull() ?: 0
                                val s2 = stepWater2.replace(',', '.').trim().toIntOrNull() ?: 0
                                val s3 = it.replace(',', '.').trim().toIntOrNull() ?: 0
                                if (s1 + s2 + s3 > 0) water = (s1 + s2 + s3).toString()
                            },
                            label = "Agua (ml)",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (validationError != null) {
                    Text(validationError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                StyledPrimaryButton(
                    text = if (isSaving) "Guardando…" else "Guardar y lista para extraer",
                    icon = Icons.Default.Check,
                    enabled = validationError == null && !isSaving,
                    onClick = {
                        if (validationError == null && !isSaving) {
                            isSaving = true
                            val pCoffee = coffee.replace(',', '.').trim().toFloatOrNull() ?: 15.0f
                            val pTemp = temp.replace(',', '.').trim().toIntOrNull() ?: 93
                            val pClicks = clicks.replace(',', '.').trim().toIntOrNull() ?: 24
                            
                            val titles = stepTitles
                            val times = stepTimes.filterNotNull()
                            val waters = stepWaters.filterNotNull()

                            viewModel.createAndSaveTechnique(
                                name = name,
                                methodId = selectedMethodId,
                                coffee = pCoffee,
                                temp = pTemp,
                                grinderId = selectedGrinderId,
                                grinderName = selectedGrinderName,
                                clicks = pClicks,
                                notes = notes,
                                stepTitles = titles,
                                stepTimes = times,
                                stepWaters = waters,
                                onCompleted = { success ->
                                    isSaving = false
                                    if (success) onDone()
                                }
                            )
                        }
                    }
                )

                StyledSecondaryButton(
                    text = "Descartar Cambios",
                    onClick = onDone
                )
            }
        }
    }
}

val borderStrokeSuave = androidx.compose.foundation.BorderStroke(1.dp, BordeMedio)
