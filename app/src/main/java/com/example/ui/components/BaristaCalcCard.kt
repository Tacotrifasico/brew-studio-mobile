package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.BaristaCalcState
import com.example.ui.viewmodel.BaristaPreset
import com.example.ui.viewmodel.RatioCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaristaCalcCard(
    state: BaristaCalcState,
    presets: List<BaristaPreset>,
    onCoffeeChanged: (String) -> Unit,
    onRatioChanged: (String) -> Unit,
    onWaterChanged: (String) -> Unit,
    onMethodSelected: (String) -> Unit,
    onPresetSelected: (BaristaPreset) -> Unit,
    onAdjustCoffee: (Float) -> Unit,
    onAdjustRatio: (Float) -> Unit,
    onAdjustWater: (Int) -> Unit,
    onResetRatio: () -> Unit,
    onCoffeeFocusLost: () -> Unit,
    onRatioFocusLost: () -> Unit,
    onWaterFocusLost: () -> Unit,
    onPrepare: () -> Unit,
    onLab: () -> Unit,
    onFavorite: () -> Unit,
    onToggleMethodPinned: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    var showManageMethodsDialog by remember { mutableStateOf(false) }

    val isCurrentRatioSaved = remember(state.method, state.coffee, state.ratio, state.savedRatioPresets) {
        state.savedRatioPresets.any {
            it.methodName == state.method &&
            Math.abs(it.coffeeGrams - state.coffee) < 0.2f &&
            Math.abs(it.ratio - state.ratio) < 0.2f
        }
    }

    // Sensory Profile Color Themes based on state
    val sensorTheme = remember(state.ratioCategory, isDarkThemeGlobal) {
        when (state.ratioCategory) {
            RatioCategory.ESPRESSO -> SensorTheme(
                cardBackground = SurfaceCard,
                accentColor = EspressoPrimary,
                label = "Espresso • Corto & Intenso",
                labelColor = EspressoPrimary,
                glowColor = EspressoPrimary.copy(alpha = 0.08f)
            )
            RatioCategory.INTENSO -> SensorTheme(
                cardBackground = SurfaceCard,
                accentColor = IntensoPrimary,
                label = "Intenso • Dulce & Marcado",
                labelColor = IntensoPrimary,
                glowColor = IntensoPrimary.copy(alpha = 0.08f)
            )
            RatioCategory.BALANCE -> SensorTheme(
                cardBackground = SurfaceCard,
                accentColor = BalancePrimary,
                label = "Balance • Redondo & Dulce",
                labelColor = BalancePrimary,
                glowColor = BalancePrimary.copy(alpha = 0.08f)
            )
            RatioCategory.CLARIDAD -> SensorTheme(
                cardBackground = SurfaceCard,
                accentColor = ClaridadPrimary,
                label = "Claridad • Té & Notas Limpias",
                labelColor = ClaridadPrimary.copy(alpha = 0.9f),
                glowColor = ClaridadPrimary.copy(alpha = 0.08f)
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = AcentoPrincipal.copy(alpha = if (isDarkThemeGlobal) 0.35f else 0.18f),
                ambientColor = AcentoPrincipal.copy(alpha = if (isDarkThemeGlobal) 0.2f else 0.08f)
            )
            .border(
                width = 1.dp,
                color = BordeSuave,
                shape = RoundedCornerShape(32.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = sensorTheme.cardBackground),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Barc",
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrincipal
                    )
                }

                // Active Method Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(sensorTheme.glowColor)
                        .border(1.dp, sensorTheme.accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${state.method} • 1:${state.ratioInput}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = sensorTheme.accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- ZONA PRINCIPAL: RESULTADO GRANDE (DINÁMICO & ORGÁNICO) ---
            val ratio = state.ratio.coerceIn(2f, 25f)
            val (rawC1, rawC2) = when {
                ratio <= 6f -> {
                    val f = ((ratio - 2f) / 4f).coerceIn(0f, 1f)
                    Pair(
                        lerp(Color(0xFF3D2817), Color(0xFF4A3728), f),
                        lerp(Color(0xFF7A3B2E), Color(0xFFA85D3F), f)
                    )
                }
                ratio <= 12f -> {
                    val f = ((ratio - 6f) / 6f).coerceIn(0f, 1f)
                    Pair(
                        lerp(Color(0xFF4A3728), Color(0xFF2D4A3E), f),
                        lerp(Color(0xFFA85D3F), Color(0xFFB5714A), f)
                    )
                }
                ratio <= 15f -> {
                    val f = ((ratio - 12f) / 3f).coerceIn(0f, 1f)
                    Pair(
                        lerp(Color(0xFF2D4A3E), Color(0xFF3D5E4F), f),
                        lerp(Color(0xFFB5714A), Color(0xFF6B9080), f)
                    )
                }
                ratio <= 18f -> {
                    val f = ((ratio - 15f) / 3f).coerceIn(0f, 1f)
                    Pair(
                        lerp(Color(0xFF3D5E4F), Color(0xFF6B9080), f),
                        lerp(Color(0xFF6B9080), Color(0xFFC9D6C4), f)
                    )
                }
                else -> {
                    Pair(Color(0xFF6B9080), Color(0xFFC9D6C4))
                }
            }

            val animColor1 by animateColorAsState(targetValue = rawC1, animationSpec = tween(500), label = "grad1")
            val animColor2 by animateColorAsState(targetValue = rawC2, animationSpec = tween(500), label = "grad2")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(26.dp),
                        spotColor = animColor1.copy(alpha = 0.45f),
                        ambientColor = animColor2.copy(alpha = 0.2f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.09f), animColor1, animColor2),
                            start = Offset(0f, 0f),
                            end = Offset(700f, 700f) // ~135° diagonal flow with top-left highlight stop
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Organic watercolour blob wash and fine noise texture overlay
                Canvas(modifier = Modifier.matchParentSize()) {
                    // Soft organic wash in top-right
                    drawCircle(
                        color = Color.White.copy(alpha = 0.14f),
                        radius = size.width * 0.48f,
                        center = Offset(size.width * 0.88f, size.height * 0.15f)
                    )
                    // Artisanal paper grain / noise dots (3-4% opacity)
                    val grainColor = Color.White.copy(alpha = 0.045f)
                    for (i in 0 until 50) {
                        val px = (i * 31.7f) % size.width
                        val py = (i * 17.1f) % size.height
                        drawCircle(color = grainColor, radius = 1.3f, center = Offset(px, py))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "AGUA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${state.water} ml",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${state.coffeeInput} g • 1:${state.ratioInput} • ${state.method}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- DIAL O MEDIDOR VISUAL ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sensación esperada",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecundario
                    )
                    Text(
                        text = sensorTheme.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = sensorTheme.labelColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                
                // Continuous sensory bar with selection tick
                RatioGaugeBar(
                    ratio = state.ratio,
                    accentColor = sensorTheme.accentColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- INPUTS EDITABLES CÓMODOS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Input Café (Swipe Up/Down or Tap +/- to adjust)
                Box(modifier = Modifier.weight(1f)) {
                    val accumulatedCoffeeY = remember { mutableStateOf(0f) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 2.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = AcentoPrincipal.copy(alpha = if (isDarkThemeGlobal) 0.25f else 0.08f),
                                ambientColor = AcentoPrincipal.copy(alpha = if (isDarkThemeGlobal) 0.12f else 0.04f)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.verticalGradient(listOf(SurfaceCard, MainBackgroundAlt.copy(alpha = 0.35f))))
                            .border(1.dp, BordeSuave, RoundedCornerShape(20.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { accumulatedCoffeeY.value = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        accumulatedCoffeeY.value += dragAmount.y
                                        if (accumulatedCoffeeY.value < -40f) {
                                            onAdjustCoffee(1.0f)
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                            accumulatedCoffeeY.value = 0f
                                        } else if (accumulatedCoffeeY.value > 40f) {
                                            onAdjustCoffee(-1.0f)
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                            accumulatedCoffeeY.value = 0f
                                        }
                                    }
                                )
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CAFÉ (g)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecundario,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        ResponsiveInputTextField(
                            value = state.coffeeInput,
                            onValueChange = onCoffeeChanged,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            onFocusLost = onCoffeeFocusLost
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MainBackgroundLight)
                                    .clickable { onAdjustCoffee(-1.0f) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", color = TextSecundario, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Text(
                                text = "⇅ Desl.",
                                fontSize = 8.sp,
                                color = AcentoSecundario
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MainBackgroundLight)
                                    .clickable { onAdjustCoffee(1.0f) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = TextSecundario, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Input Ratio (Slider simulation or Tap +/- to adjust)
                Box(modifier = Modifier.weight(1f)) {
                    val accumulatedRatioX = remember { mutableStateOf(0f) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 2.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = AcentoPrincipal.copy(alpha = if (isDarkThemeGlobal) 0.25f else 0.08f),
                                ambientColor = AcentoPrincipal.copy(alpha = if (isDarkThemeGlobal) 0.12f else 0.04f)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.verticalGradient(listOf(SurfaceCard, MainBackgroundLight.copy(alpha = 0.3f))))
                            .border(1.dp, BordeSuave, RoundedCornerShape(20.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { onResetRatio() }
                                )
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { accumulatedRatioX.value = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        accumulatedRatioX.value += dragAmount.x
                                        if (accumulatedRatioX.value > 25f) {
                                            onAdjustRatio(0.1f)
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                            accumulatedRatioX.value = 0f
                                        } else if (accumulatedRatioX.value < -25f) {
                                            onAdjustRatio(-0.1f)
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                            accumulatedRatioX.value = 0f
                                        }
                                    }
                                )
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "RATIO (1:x)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecundario,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))

                        ResponsiveInputTextField(
                            value = state.ratioInput,
                            onValueChange = onRatioChanged,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            onFocusLost = onRatioFocusLost
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MainBackgroundLight)
                                    .clickable { onAdjustRatio(-0.1f) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", color = TextSecundario, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Text(
                                text = "⇆ Desl.",
                                fontSize = 8.sp,
                                color = AcentoSecundario
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MainBackgroundLight)
                                    .clickable { onAdjustRatio(0.1f) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = TextSecundario, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Input Agua (Swipe Up/Down or Tap +/- to adjust (+/- 10ml))
                Box(modifier = Modifier.weight(1f)) {
                    val accumulatedWaterY = remember { mutableStateOf(0f) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 2.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = AcentoPrincipal.copy(alpha = if (isDarkThemeGlobal) 0.25f else 0.08f),
                                ambientColor = AcentoPrincipal.copy(alpha = if (isDarkThemeGlobal) 0.12f else 0.04f)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.verticalGradient(listOf(SurfaceCard, AcentoSuave.copy(alpha = 0.35f))))
                            .border(1.dp, BordeSuave, RoundedCornerShape(20.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { accumulatedWaterY.value = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        accumulatedWaterY.value += dragAmount.y
                                        if (accumulatedWaterY.value < -40f) {
                                            onAdjustWater(10)
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                            accumulatedWaterY.value = 0f
                                        } else if (accumulatedWaterY.value > 40f) {
                                            onAdjustWater(-10)
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                            accumulatedWaterY.value = 0f
                                        }
                                    }
                                )
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AGUA (ml)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecundario,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))

                        ResponsiveInputTextField(
                            value = state.waterInput,
                            onValueChange = onWaterChanged,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            onFocusLost = onWaterFocusLost
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MainBackgroundLight)
                                    .clickable { onAdjustWater(-10) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", color = TextSecundario, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Text(
                                text = "⇅ +/-10",
                                fontSize = 8.sp,
                                color = AcentoSecundario
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MainBackgroundLight)
                                    .clickable { onAdjustWater(10) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = TextSecundario, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- CHIPS DE PRESTATS (LazyRow) ---
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(presets) { preset ->
                    val isActive = state.method == preset.method &&
                            (state.coffee - preset.coffee).let { Math.abs(it) < 0.2f } &&
                            (state.ratio - preset.ratio).let { Math.abs(it) < 0.2f }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isActive) AcentoSuave else SurfaceCard)
                            .border(
                                width = 1.dp,
                                color = if (isActive) sensorTheme.accentColor else BordeSuave,
                                shape = CircleShape
                            )
                            .clickable {
                                onPresetSelected(preset)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (preset.isCustom) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Advertencia,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = preset.label,
                                fontSize = 12.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) sensorTheme.accentColor else TextSecundario
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- SELECCIÓN DE MÉTODO DIRECTO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Métodos rápidos:",
                        fontSize = 12.sp,
                        color = TextSecundario,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(
                        onClick = { showManageMethodsDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Gestionar métodos",
                            tint = AccentGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickMethods = if (state.pinnedMethods.isNotEmpty()) {
                        state.pinnedMethods.map { it.name }
                    } else if (state.userMethods.isNotEmpty()) {
                        state.userMethods.map { it.name }
                    } else {
                        listOf("V60", "AeroPress", "Prensa francesa", "Chemex", "Espresso", "Moka", "Cold brew")
                    }
                    items(quickMethods) { method ->
                        val isSelected = state.method.equals(method, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) sensorTheme.accentColor else SurfaceCard)
                                .border(1.dp, BordeSuave, RoundedCornerShape(8.dp))
                                .clickable { onMethodSelected(method) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = method,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextPrincipal
                            )
                        }
                    }
                }
            }

            if (showManageMethodsDialog && onToggleMethodPinned != null) {
                ManageMethodsDialog(
                    userMethods = if (state.userMethods.isNotEmpty()) state.userMethods else listOf(
                        com.example.data.domain.UserMethodItem("1", "11111111-1111-4000-8000-000000000001", "V60", "v60", isPinnedToCalculator = true),
                        com.example.data.domain.UserMethodItem("2", "11111111-1111-4000-8000-000000000002", "AeroPress", "aeropress", isPinnedToCalculator = true),
                        com.example.data.domain.UserMethodItem("3", "11111111-1111-4000-8000-000000000003", "Espresso", "espresso", isPinnedToCalculator = true),
                        com.example.data.domain.UserMethodItem("4", "11111111-1111-4000-8000-000000000004", "Prensa francesa", "french_press", isPinnedToCalculator = true)
                    ),
                    onTogglePinned = { methodId -> onToggleMethodPinned(methodId) },
                    onDismiss = { showManageMethodsDialog = false }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- MICROCOPY STATUS BOARD ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(sensorTheme.accentColor.copy(alpha = 0.05f))
                    .border(1.dp, BordeSuave, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Status",
                    tint = sensorTheme.accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = state.microcopy,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecundario,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTTOM BUTTON PANEL ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Secondary Action: Laboratorio (Icon-only)
                Button(
                    onClick = onLab,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceCard,
                        contentColor = TextPrincipal
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BordeMedio)
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = "Laboratorio",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Primary Action: Preparar (Icon-only)
                Button(
                    onClick = onPrepare,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = sensorTheme.accentColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Preparar",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Favorito action
                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .background(SurfaceCard, RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = if (isCurrentRatioSaved) Advertencia else BordeMedio,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (isCurrentRatioSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Guardar Ratio en Calculadora",
                        tint = if (isCurrentRatioSaved) Advertencia else TextSecundario
                    )
                }
            }
        }
    }
}

// Continuous premium visual gauge representing ratio scale
@Composable
fun RatioGaugeBar(
    ratio: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        // Draw standard range bounds
        // espresso (1 to 3), intense (4 to 10), balance (11 to 16), clarity (17 to 25)
        // Let's draw contiguous colored lines
        val cut1 = width * (3f / 25f)
        val cut2 = width * (10f / 25f)
        val cut3 = width * (16f / 25f)
        
        // Background track lines with custom round endings and colors
        // Espresso zone (Brown)
        drawLine(
            color = EspressoPrimary.copy(alpha = 0.25f),
            start = Offset(0f, centerY),
            end = Offset(cut1, centerY),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Intenso zone (Caramel)
        drawLine(
            color = IntensoPrimary.copy(alpha = 0.25f),
            start = Offset(cut1, centerY),
            end = Offset(cut2, centerY),
            strokeWidth = 6.dp.toPx()
        )
        // Balance zone (Green)
        drawLine(
            color = BalancePrimary.copy(alpha = 0.25f),
            start = Offset(cut2, centerY),
            end = Offset(cut3, centerY),
            strokeWidth = 6.dp.toPx()
        )
        // Claridad zone (Clear Light Green)
        drawLine(
            color = ClaridadPrimary.copy(alpha = 0.25f),
            start = Offset(cut3, centerY),
            end = Offset(width, centerY),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Draw selection Tick Indicator pin
        val cappedRatio = ratio.coerceIn(1f, 25f)
        val tickX = width * ((cappedRatio - 1f) / 24f)
        
        drawCircle(
            color = accentColor,
            radius = 8.dp.toPx(),
            center = Offset(tickX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = Offset(tickX, centerY)
        )
    }
}

private data class SensorTheme(
    val cardBackground: Color,
    val accentColor: Color,
    val label: String,
    val labelColor: Color,
    val glowColor: Color
)

@Composable
fun ResponsiveInputTextField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onFocusLost: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamically adjust font size for longer values to prevent layout clipping or hiding
    val fontSize = when {
        value.length > 5 -> 14.sp
        value.length > 4 -> 16.sp
        else -> 18.sp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                textAlign = TextAlign.Center,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = TextPrincipal
            ),
            cursorBrush = SolidColor(TextPrincipal),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .onFocusChanged { if (!it.isFocused) onFocusLost() }
        )
    }
}

@Composable
fun ManageMethodsDialog(
    userMethods: List<com.example.data.domain.UserMethodItem>,
    onTogglePinned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Listo", color = AccentGold, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text("Gestionar Métodos en Calculadora", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Selecciona los métodos que quieres ver en el acceso directo de la calculadora del Home:", fontSize = 12.sp, color = TextSecundario)
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(userMethods, key = { it.methodId }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceElevated)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                                Text(item.category, fontSize = 11.sp, color = TextSecundario)
                            }
                            Switch(
                                checked = item.isPinnedToCalculator,
                                onCheckedChange = { onTogglePinned(item.methodId) }
                            )
                        }
                    }
                }
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(18.dp)
    )
}
