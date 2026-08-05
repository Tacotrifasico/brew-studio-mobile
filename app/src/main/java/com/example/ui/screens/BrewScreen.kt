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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Technique
import com.example.ui.theme.*
import com.example.ui.viewmodel.BaristaCalcViewModel
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BrewScreen(
    viewModel: BaristaCalcViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var isCreatingCustom by remember { mutableStateOf(false) }

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

            if (!state.timerRunning) {
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
            targetState = state.timerRunning,
            transitionSpec = {
                fadeIn() + slideInVertically() with fadeOut() + slideOutVertically()
            },
            label = "timerState"
        ) { timerRunning ->
            if (timerRunning) {
                // ACTIVE EXTRACTOR TIMER DISPLAY
                ActiveBrewTimerView(viewModel = viewModel, state = state)
            } else if (isCreatingCustom) {
                // CREATION FORM VIEW
                CreateTechniqueFormView(
                    viewModel = viewModel,
                    onDone = { isCreatingCustom = false }
                )
            } else {
                // DEFAULT SETUP SCREEN WITH LIBRARY PICKERS
                BrewSetupView(viewModel = viewModel, state = state)
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
                        imageVector = Icons.Default.LocalCafe,
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
                            text = "${state.activePrepCoffee}g",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Box(modifier = Modifier.size(1.dp, 20.dp).background(Color.White.copy(alpha = 0.3f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("RATIO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
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
                            text = "${state.activePrepWater}ml",
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

        // Techniques library section
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "CARGAR TÉCNICA DE LA BIBLIOTECA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecundario,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (state.techniquesList.isEmpty()) {
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
                                imageVector = Icons.Default.Coffee,
                                contentDescription = null,
                                tint = AcentoPrincipal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Biblioteca en vacío",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrincipal
                        )
                        Text(
                            text = "Los presets rápidos se cargan automáticamente al iniciar extracciones.",
                            fontSize = 12.sp,
                            color = TextSecundario,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                state.techniquesList.forEach { tech ->
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
                                Text("${tech.doseG}g café • ${tech.waterMl}ml agua", fontSize = 11.sp, color = TextSecundario)
                            }
                            IconButton(onClick = { viewModel.deleteTechnique(tech.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Borrar", tint = Advertencia.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveBrewTimerView(
    viewModel: BaristaCalcViewModel,
    state: com.example.ui.viewmodel.BaristaCalcState
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
                        text = "Vaso actual: ${activeStep?.title ?: "Derrame de equilibrio"}",
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
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF2F7F3))
                        ) {
                            Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Atras", tint = TextPrincipal)
                        }

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

                        IconButton(
                            onClick = { viewModel.advanceStep() },
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) TextPrincipal else if (isPast) TextSecundario else TextPrincipal.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${step.durationSeconds}s • Agrega +${step.waterAddedMl}ml (Total: ${step.waterAccumulatedMl}ml)",
                            fontSize = 11.sp,
                            color = TextSecundario
                        )
                    }
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AcentoPrincipal.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("ACTIVO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AcentoPrincipal)
                        }
                    }
                }
            }
        }

        // Finish action button
        item {
            Button(
                onClick = { viewModel.stopTimer() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Advertencia),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Completado")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Completar Extracción e ir a Cata", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CreateTechniqueFormView(
    viewModel: BaristaCalcViewModel,
    onDone: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("V60") }
    var coffee by remember { mutableStateOf("15.0") }
    var water by remember { mutableStateOf("240") }
    var temp by remember { mutableStateOf("93") }
    var grinder by remember { mutableStateOf("Comandante") }
    var clicks by remember { mutableStateOf("24") }
    var notes by remember { mutableStateOf("") }

    // Step lists
    var stepTitle1 by remember { mutableStateOf("Preinfusión Bloom") }
    var stepTime1 by remember { mutableStateOf("35") }
    var stepWater1 by remember { mutableStateOf("50") }

    var stepTitle2 by remember { mutableStateOf("Primer Vertido") }
    var stepTime2 by remember { mutableStateOf("45") }
    var stepWater2 by remember { mutableStateOf("90") }

    var stepTitle3 by remember { mutableStateOf("Segundo Vertido Seguro") }
    var stepTime3 by remember { mutableStateOf("40") }
    var stepWater3 by remember { mutableStateOf("100") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BordeSuave, RoundedCornerShape(22.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Datos Generales de la Técnica", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la técnica") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = method,
                        onValueChange = { method = it },
                        label = { Text("Método") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = temp,
                        onValueChange = { temp = it },
                        label = { Text("Temp °C") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = coffee,
                        onValueChange = { coffee = it },
                        label = { Text("Café g") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = water,
                        onValueChange = { water = it },
                        label = { Text("Agua ml") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = grinder,
                        onValueChange = { grinder = it },
                        label = { Text("Molino") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f)
                    )
                    OutlinedTextField(
                        value = clicks,
                        onValueChange = { clicks = it },
                        label = { Text("Clicks") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas o instrucciones de calibración") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Steps definitions card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BordeSuave, RoundedCornerShape(22.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Estructura de Pasos (3 Pasos)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)

                // Paso 1
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Paso 1 (Bloom Inicial)", fontSize = 11.sp, color = AcentoPrincipal, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = stepTitle1,
                        onValueChange = { stepTitle1 = it },
                        label = { Text("Título") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stepTime1,
                            onValueChange = { stepTime1 = it },
                            label = { Text("Segundos") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = stepWater1,
                            onValueChange = { stepWater1 = it },
                            label = { Text("Agua ml") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Paso 2
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Paso 2 (Extracción)", fontSize = 11.sp, color = AcentoPrincipal, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = stepTitle2,
                        onValueChange = { stepTitle2 = it },
                        label = { Text("Título") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stepTime2,
                            onValueChange = { stepTime2 = it },
                            label = { Text("Segundos") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = stepWater2,
                            onValueChange = { stepWater2 = it },
                            label = { Text("Agua ml") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Paso 3
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Paso 3 (Decantación final)", fontSize = 11.sp, color = AcentoPrincipal, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = stepTitle3,
                        onValueChange = { stepTitle3 = it },
                        label = { Text("Título") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stepTime3,
                            onValueChange = { stepTime3 = it },
                            label = { Text("Segundos") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = stepWater3,
                            onValueChange = { stepWater3 = it },
                            label = { Text("Agua ml") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Save & Done Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val parsedCoffee = coffee.toFloatOrNull() ?: 15.0f
                        val parsedWater = water.toIntOrNull() ?: 240
                        val parsedRatio = parsedWater / parsedCoffee
                        val parsedTemp = temp.toIntOrNull() ?: 93
                        val parsedClicks = clicks.toIntOrNull() ?: 24
                        
                        val titles = listOf(stepTitle1, stepTitle2, stepTitle3)
                        val times = listOf(
                            stepTime1.toIntOrNull() ?: 35,
                            stepTime2.toIntOrNull() ?: 45,
                            stepTime3.toIntOrNull() ?: 40
                        )
                        val waters = listOf(
                            stepWater1.toIntOrNull() ?: 50,
                            stepWater2.toIntOrNull() ?: 90,
                            stepWater3.toIntOrNull() ?: 100
                        )

                        viewModel.createAndSaveTechnique(
                            name = name,
                            method = method,
                            coffee = parsedCoffee,
                            water = parsedWater,
                            ratio = parsedRatio,
                            temp = parsedTemp,
                            grinder = grinder,
                            clicks = parsedClicks,
                            notes = notes,
                            stepTitles = titles,
                            stepTimes = times,
                            stepWaters = waters
                        )
                        onDone()
                    }
                },
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Guardar Técnica", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onDone,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard, contentColor = TextPrincipal),
                shape = RoundedCornerShape(16.dp),
                border = borderStrokeSuave
            ) {
                Text("Descartar", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

val borderStrokeSuave = androidx.compose.foundation.BorderStroke(1.dp, BordeMedio)
