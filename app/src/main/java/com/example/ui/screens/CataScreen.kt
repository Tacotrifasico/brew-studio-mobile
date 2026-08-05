package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.BaristaCalcViewModel

@Composable
fun CataScreen(
    viewModel: BaristaCalcViewModel,
    onNavigateToSection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    var rating by remember { mutableStateOf(4.0f) }
    var notesFoundInput by remember { mutableStateOf("") }
    var expectedNotesInput by remember { mutableStateOf("Frutas Rojas, Chocolate, Panela") }
    var commentsInput by remember { mutableStateOf("") }

    // Start simulated cup cooling timer when screen loads
    DisposableEffect(Unit) {
        viewModel.startCataMinutesTimer()
        onDispose {
            viewModel.stopCataTimer()
        }
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- SCREEN HEADER ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "EVALUACIÓN SENSORIAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = TextSecundario
                )
                Text(
                    text = "Cata Artesanal",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrincipal
                )
            }

            // --- COOLING CUP CARD (TAZA VIVA - LEVEL 3 HERO) ---
            val coolingMinutes = state.cataMinutesElapsed.toFloat().coerceIn(0f, 15f)
            val cataGrad1 = lerp(Color(0xFFB85D42), Color(0xFF2D4A3E), coolingMinutes / 15f)
            val cataGrad2 = lerp(Color(0xFF7A3620), Color(0xFF1B3D2F), coolingMinutes / 15f)

            val animCata1 by animateColorAsState(targetValue = cataGrad1, animationSpec = tween(500), label = "cataG1")
            val animCata2 by animateColorAsState(targetValue = cataGrad2, animationSpec = tween(500), label = "cataG2")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(26.dp),
                        spotColor = animCata1.copy(alpha = 0.4f),
                        ambientColor = animCata2.copy(alpha = 0.2f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.1f), animCata1, animCata2),
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
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "LA VIDA DE LA TAZA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 1.2.sp
                        )
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${state.cataMinutesElapsed} min.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiFoodBeverage,
                            contentDescription = "Cup Cooling",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = viewModel.getCupLifeStateLabel(),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Evolución térmica y liberación de azúcares naturales.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.startCataMinutesTimer() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.22f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reiniciar", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reiniciar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // --- NOTES DETECTED SELECTOR (LEVEL 2 CARD) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp), spotColor = CafeCalidoOscuro.copy(alpha = 0.12f))
                    .border(1.dp, BordeSuave, RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "DESCRIPTOR DE NOTAS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecundario,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = expectedNotesInput,
                        onValueChange = { expectedNotesInput = it },
                        label = { Text("Notas esperadas") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notesFoundInput,
                        onValueChange = { notesFoundInput = it; viewModel.updateCataFoundNotes(it) },
                        label = { Text("Notas encontradas") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Sugerencias:", fontSize = 11.sp, color = TextSecundario, fontWeight = FontWeight.SemiBold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Chocolate", "Manzana", "Cacao", "Cítrico", "Mora").forEach { item ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MainBackgroundAlt.copy(alpha = 0.6f))
                                    .border(1.dp, BordeSuave, RoundedCornerShape(10.dp))
                                    .clickable {
                                        val currentStr = notesFoundInput
                                        notesFoundInput = if (currentStr.isBlank()) item else "$currentStr, $item"
                                        viewModel.updateCataFoundNotes(notesFoundInput)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(item, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrincipal)
                            }
                        }
                    }
                }
            }

            // --- TEXTURE, CLEANLINESS & PERSISTENCE (LEVEL 2 CARD) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp), spotColor = CafeCalidoOscuro.copy(alpha = 0.12f))
                    .border(1.dp, BordeSuave, RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "TEXTURA Y ATRIBUTOS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecundario,
                        letterSpacing = 1.sp
                    )

                    // TEXTURE Group
                    Column {
                        Text("Textura en Boca: ${state.cataTexture.uppercase()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("ligera", "sedosa", "jugosa", "densa", "seca").forEach { text ->
                                val isSelected = state.cataTexture == text
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) AcentoPrincipal else MainBackgroundAlt.copy(alpha = 0.6f))
                                        .border(1.dp, if (isSelected) AcentoPrincipal else BordeSuave, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.setCataTexture(text) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = text,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextSecundario
                                    )
                                }
                            }
                        }
                    }

                    // CLEANLINESS Group
                    Column {
                        Text("Claridad / Limpieza: ${state.cataCleanliness.uppercase()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("baja", "media", "alta", "muy alta").forEach { item ->
                                val isSelected = state.cataCleanliness == item
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) CafeCalidoClaro else MainBackgroundAlt.copy(alpha = 0.6f))
                                        .border(1.dp, if (isSelected) CafeCalidoClaro else BordeSuave, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.setCataCleanliness(item) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextSecundario
                                    )
                                }
                            }
                        }
                    }

                    // PERSISTENCE Group
                    Column {
                        Text("Persistencia: ${state.cataPersistence.uppercase()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("corta", "media", "larga").forEach { item ->
                                val isSelected = state.cataPersistence == item
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) AcentoSecundario else MainBackgroundAlt.copy(alpha = 0.6f))
                                        .border(1.dp, if (isSelected) AcentoSecundario else BordeSuave, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.setCataPersistence(item) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextSecundario
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- RATING & FREE COMMENT CARD (LEVEL 2) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp), spotColor = CafeCalidoOscuro.copy(alpha = 0.12f))
                    .border(1.dp, BordeSuave, RoundedCornerShape(22.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "CALIFICACIÓN FINAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecundario,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        (1..5).forEach { star ->
                            Icon(
                                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Rating Star",
                                tint = Advertencia,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { rating = star.toFloat(); viewModel.setCataRating(rating) }
                                    .padding(horizontal = 2.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = commentsInput,
                        onValueChange = { commentsInput = it },
                        label = { Text("Comentarios o balance general...") },
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- CORE ACTION BUTTONS ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveCup(
                            notesFound = notesFoundInput,
                            notesExpected = expectedNotesInput,
                            score = rating,
                            comment = commentsInput
                        )
                        notesFoundInput = ""
                        commentsInput = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Guardar Taza")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar Taza Catada", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        viewModel.pullCataToLab()
                        onNavigateToSection("lab")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard, contentColor = TextPrincipal),
                    shape = RoundedCornerShape(16.dp),
                    border = borderStrokeSuave
                ) {
                    Icon(imageVector = Icons.Default.Science, contentDescription = "Diag", tint = AcentoSecundario)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Llevar al Laboratorio", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
