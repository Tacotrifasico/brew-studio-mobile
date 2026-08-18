package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.example.ui.components.BaristaCalcCard
import com.example.ui.components.V60Icon
import com.example.ui.theme.*
import com.example.ui.viewmodel.BaristaCalcViewModel

data class QuickAccessOptionItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val accentColor: Color,
    val action: () -> Unit
)

@Composable
fun HomeScreen(
    viewModel: BaristaCalcViewModel,
    onNavigateToSection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("app_quick_access_prefs", android.content.Context.MODE_PRIVATE) }

    val allQuickAccessOptions = remember(onNavigateToSection) {
        listOf(
            QuickAccessOptionItem("cata", "Cata", Icons.Default.RateReview, CafeCalidoClaro) { onNavigateToSection("cata") },
            QuickAccessOptionItem("lab", "Laboratorio", Icons.Default.Science, AcentoSecundario) { onNavigateToSection("lab") },
            QuickAccessOptionItem("storage", "Almacén", Icons.Default.Inventory, CafeCalidoOscuro) { onNavigateToSection("storage") },
            QuickAccessOptionItem("social", "Brew Hub", Icons.Default.Groups, Color(0xFFA15A95)) { onNavigateToSection("social") },
            QuickAccessOptionItem("add_coffee", "Agregar Café", Icons.Default.Grass, AcentoPrincipal) { onNavigateToSection("storage") },
            QuickAccessOptionItem("add_recipe", "Nueva Receta", Icons.AutoMirrored.Filled.MenuBook, CafeCalidoOscuro) { onNavigateToSection("storage") },
            QuickAccessOptionItem("add_technique", "Nueva Técnica", V60Icon, AcentoSecundario) { onNavigateToSection("brew") },
            QuickAccessOptionItem("add_grinder", "Registrar Molienda", Icons.Default.Tune, CafeCalidoClaro) { onNavigateToSection("storage") },
            QuickAccessOptionItem("add_equipment", "Nuevo Equipo", Icons.Default.Handyman, TextSecundario) { onNavigateToSection("storage") }
        )
    }

    var selectedAccessIds by remember {
        val saved = prefs.getStringSet("selected_ids", null) ?: setOf("cata", "lab", "storage", "social")
        mutableStateOf(saved)
    }

    var showCustomizeQuickAccessDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MainBackground)
    ) {
        // Atmospheric organic blurred / glowing gradient shapes (depth & atmosphere)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val greenGlow = if (isDarkThemeGlobal) AcentoPrincipal.copy(alpha = 0.22f) else AcentoPrincipal.copy(alpha = 0.12f)
            val terracottaGlow = if (isDarkThemeGlobal) CafeCalidoOscuro.copy(alpha = 0.18f) else CafeCalidoOscuro.copy(alpha = 0.10f)

            // Top right organic circle
            drawCircle(
                color = greenGlow,
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.88f, size.height * 0.10f)
            )
            // Bottom left organic circle
            drawCircle(
                color = terracottaGlow,
                radius = size.width * 0.48f,
                center = Offset(size.width * 0.12f, size.height * 0.78f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp, top = 16.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
        // --- HEADER CÁLIDO ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Utilities Row: User profile avatar, Inbox Bell, and Day/Dark Theme Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile button / avatar (navigates to "social" screen)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BordeSuave, RoundedCornerShape(16.dp))
                            .clickable { onNavigateToSection("social") }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AcentoSuave),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Perfil",
                                tint = AcentoPrincipal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mi Perfil",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrincipal
                        )
                    }

                    // Theme and Inbox utility buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Beautiful Segmented Pill Switch for Dark/Light Mode
                        Row(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(MainBackgroundAlt)
                                .border(1.dp, BordeSuave, RoundedCornerShape(19.dp))
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Light Mode Button
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (!isDarkThemeGlobal) SurfaceCard else Color.Transparent)
                                    .clickable { updateThemeColors(false) }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LightMode,
                                        contentDescription = "Día",
                                        tint = if (!isDarkThemeGlobal) CafeCalidoClaro else TextSecundario,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Día",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isDarkThemeGlobal) TextPrincipal else TextSecundario
                                    )
                                }
                            }
                            
                            // Dark Mode Button
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isDarkThemeGlobal) SurfaceCard else Color.Transparent)
                                    .clickable { updateThemeColors(true) }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = "Noche",
                                        tint = if (isDarkThemeGlobal) AcentoPrincipal else TextSecundario,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Noche",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkThemeGlobal) TextPrincipal else TextSecundario
                                    )
                                }
                            }
                        }

                        // Notifications Bell button
                        var showNotificationsDialog by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(SurfaceCard)
                                .border(1.dp, BordeSuave, RoundedCornerShape(18.dp))
                                .clickable { showNotificationsDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notificaciones",
                                tint = TextSecundario,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (showNotificationsDialog) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { showNotificationsDialog = false },
                                title = { Text("Notificaciones", fontWeight = FontWeight.Bold, color = TextPrincipal) },
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.NotificationsNone,
                                            contentDescription = null,
                                            tint = TextSecundario,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Sin notificaciones nuevas",
                                            fontSize = 14.sp,
                                            color = TextSecundario,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(onClick = { showNotificationsDialog = false }) {
                                        Text("Cerrar", color = AcentoPrincipal, fontWeight = FontWeight.Bold)
                                    }
                                },
                                containerColor = SurfaceCard,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                // Title
                Text(
                    text = "¡Buen día, Brewther!",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrincipal,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        // --- BARISTA CALC CARD ---
        item {
            BaristaCalcCard(
                state = state,
                presets = viewModel.presets,
                onCoffeeChanged = { viewModel.onCoffeeChanged(it) },
                onRatioChanged = { viewModel.onRatioChanged(it) },
                onWaterChanged = { viewModel.onWaterChanged(it) },
                onMethodSelected = { viewModel.onMethodSelected(it) },
                onPresetSelected = { viewModel.applyPreset(it) },
                onAdjustCoffee = { viewModel.adjustCoffee(it) },
                onAdjustRatio = { viewModel.adjustRatio(it) },
                onAdjustWater = { viewModel.adjustWater(it) },
                onResetRatio = { viewModel.resetRatioToMethodBase() },
                onCoffeeFocusLost = { viewModel.onCoffeeFocusLost() },
                onRatioFocusLost = { viewModel.onRatioFocusLost() },
                onWaterFocusLost = { viewModel.onWaterFocusLost() },
                onPrepare = { viewModel.onActionPrepare(); onNavigateToSection("brew") },
                onLab = { viewModel.onActionLab(); onNavigateToSection("lab") },
                onFavorite = { viewModel.onActionFavorite() },
                onToggleMethodPinned = { methodId -> viewModel.toggleMethodPinned(methodId) }
            )
        }

        // --- QUICK ACCESOS ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = AcentoPrincipal.copy(alpha = if (isDarkThemeGlobal) 0.25f else 0.1f)
                    )
                    .border(1.dp, BordeSuave, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACCESOS RÁPIDOS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecundario,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AcentoSuave)
                                .clickable { showCustomizeQuickAccessDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Personalizar",
                                tint = AcentoPrincipal,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Personalizar",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AcentoPrincipal
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Hero / Primary Action Card: Preparar (Organic & Dynamic)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 24.dp),
                                spotColor = AcentoPrincipal.copy(alpha = 0.45f),
                                ambientColor = CafeCalidoOscuro.copy(alpha = 0.2f)
                            )
                            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 24.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(AcentoPrincipal, Color(0xFF16382B)),
                                    start = Offset(0f, 0f),
                                    end = Offset(600f, 600f)
                                )
                            )
                            .clickable { onNavigateToSection("brew") }
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.14f),
                                radius = size.width * 0.45f,
                                center = Offset(size.width * 0.85f, size.height * 0.2f)
                            )
                            val grainColor = Color.White.copy(alpha = 0.045f)
                            for (i in 0 until 40) {
                                val px = (i * 29.3f) % size.width
                                val py = (i * 13.7f) % size.height
                                drawCircle(color = grainColor, radius = 1.3f, center = Offset(px, py))
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(topStart = 16.dp, topEnd = 8.dp, bottomStart = 10.dp, bottomEnd = 16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Preparar Extracción",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Iniciar sesión guiada paso a paso",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic User Selected Quick Access Items
                    val activeItems = allQuickAccessOptions.filter { selectedAccessIds.contains(it.id) }
                    activeItems.chunked(2).forEachIndexed { index, pair ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { item ->
                                QuickAccessPill(
                                    label = item.label,
                                    icon = item.icon,
                                    accentColor = item.accentColor,
                                    onClick = item.action,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Customization Dialog for Quick Access
        if (showCustomizeQuickAccessDialog) {
            item {
                AlertDialog(
                    onDismissRequest = { showCustomizeQuickAccessDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = AcentoPrincipal)
                            Text("Personalizar Accesos Rápidos", fontWeight = FontWeight.Bold, color = TextPrincipal, fontSize = 16.sp)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Selecciona las acciones directas que deseas tener a la mano en la pantalla de inicio:",
                                fontSize = 12.sp,
                                color = TextSecundario,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            allQuickAccessOptions.forEach { option ->
                                val isChecked = selectedAccessIds.contains(option.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isChecked) AcentoSuave else MainBackgroundAlt)
                                        .clickable {
                                            val newSet = selectedAccessIds.toMutableSet()
                                            if (isChecked) {
                                                if (newSet.size > 1) newSet.remove(option.id)
                                            } else {
                                                newSet.add(option.id)
                                            }
                                            selectedAccessIds = newSet
                                            prefs.edit().putStringSet("selected_ids", newSet).apply()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(option.accentColor.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = option.icon,
                                                contentDescription = null,
                                                tint = option.accentColor,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Text(
                                            text = option.label,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrincipal
                                        )
                                    }
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            val newSet = selectedAccessIds.toMutableSet()
                                            if (checked) {
                                                newSet.add(option.id)
                                            } else {
                                                if (newSet.size > 1) newSet.remove(option.id)
                                            }
                                            selectedAccessIds = newSet
                                            prefs.edit().putStringSet("selected_ids", newSet).apply()
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = AcentoPrincipal)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCustomizeQuickAccessDialog = false }) {
                            Text("Guardar", fontWeight = FontWeight.Bold, color = AcentoPrincipal)
                        }
                    },
                    containerColor = SurfaceCard,
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // --- ARTISANAL PROCESS FLOW CHART ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = AcentoPrincipal.copy(alpha = if (isDarkThemeGlobal) 0.25f else 0.1f)
                    )
                    .border(1.dp, BordeSuave, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "FLUJO DEL BARISTA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecundario,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlowStepItem("Brew", Icons.Default.LocalCafe, AcentoPrincipal, "Extrae")
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecundario, modifier = Modifier.size(16.dp))
                        FlowStepItem("Taste", Icons.Default.RestaurantMenu, CafeCalidoClaro, "Cata")
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecundario, modifier = Modifier.size(16.dp))
                        FlowStepItem("Diagnose", Icons.Default.Assessment, Advertencia, "Evalúa")
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecundario, modifier = Modifier.size(16.dp))
                        FlowStepItem("Adjust", Icons.Default.Tune, AcentoSecundario, "Calibra")
                    }
                }
            }
        }

        // --- MINI CARDS DE CONTEO ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = AcentoPrincipal.copy(alpha = if (isDarkThemeGlobal) 0.25f else 0.1f)
                    )
                    .border(1.dp, BordeSuave, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ESTADO DEL TALLER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecundario,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        val stats = listOf(
                            StatItem("Granos", state.beansCount.toString(), Icons.Default.Grass, CafeCalidoOscuro),
                            StatItem("Recetas", state.recipesCount.toString(), Icons.AutoMirrored.Filled.MenuBook, AcentoPrincipal),
                            StatItem("Tazas", state.cupsCount.toString(), Icons.Default.EmojiFoodBeverage, CafeCalidoClaro),
                            StatItem("Técnicas", state.techniquesCount.toString(), V60Icon, AcentoSecundario),
                            StatItem("Equipo", (state.equipmentCount + state.grindersCount).toString(), Icons.Default.Handyman, TextSecundario)
                        )
                        items(stats) { stat ->
                            StatMiniCard(item = stat)
                        }
                    }
                }
            }
        }

        // --- ÚLTIMOS RECURSOS USADOS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Último Grano
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp), spotColor = CafeCalidoOscuro.copy(alpha = 0.2f))
                        .border(1.dp, BordeSuave, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(CafeCalidoOscuro)
                        )
                        Column(modifier = Modifier.padding(14.dp).weight(1f)) {
                            Text("ÚLTIMO GRANO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecundario, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            val lastBean = state.beansList.firstOrNull()
                            if (lastBean != null) {
                                Text(lastBean.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrincipal, maxLines = 1)
                                Text(lastBean.origin, fontSize = 11.sp, color = TextSecundario, maxLines = 1)
                                Text("${lastBean.process} • ${lastBean.stockGrams}g stock", fontSize = 10.sp, color = CafeCalidoOscuro, fontWeight = FontWeight.Medium)
                            } else {
                                Text("No hay granos registrados", fontSize = 12.sp, color = TextSecundario)
                            }
                        }
                    }
                }

                // última Receta
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp), spotColor = AcentoPrincipal.copy(alpha = 0.2f))
                        .border(1.dp, BordeSuave, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(AcentoPrincipal)
                        )
                        Column(modifier = Modifier.padding(14.dp).weight(1f)) {
                            Text("ÚLTIMA RECETA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecundario, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            val lastRecipe = state.recipesList.firstOrNull()
                            if (lastRecipe != null) {
                                Text(lastRecipe.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrincipal, maxLines = 1)
                                Text(lastRecipe.intention.ifBlank { lastRecipe.recipeKind }, fontSize = 11.sp, color = TextSecundario, maxLines = 1)
                                Text(lastRecipe.ingredientsSummary.ifBlank { "Fórmula guardada" }, fontSize = 10.sp, color = AcentoPrincipal, fontWeight = FontWeight.Medium)
                            } else {
                                Text("V60 Filtro Estándar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                                Text("V60 • 15g • 1:16", fontSize = 11.sp, color = TextSecundario)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun FlowStepItem(
    name: String,
    icon: ImageVector,
    color: Color,
    sub: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0.06f)),
                        start = Offset(0f, 0f),
                        end = Offset(40f, 40f)
                    ),
                    RoundedCornerShape(topStart = 14.dp, topEnd = 6.dp, bottomStart = 8.dp, bottomEnd = 14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = name, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
        Text(sub, fontSize = 10.sp, color = TextSecundario)
    }
}

@Composable
fun QuickAccessPill(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(18.dp), spotColor = accentColor.copy(alpha = 0.15f))
            .clip(RoundedCornerShape(18.dp))
            .background(MainBackgroundAlt.copy(alpha = 0.5f))
            .border(1.dp, BordeSuave, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(accentColor.copy(alpha = 0.2f), accentColor.copy(alpha = 0.05f)),
                        start = Offset(0f, 0f),
                        end = Offset(40f, 40f)
                    ),
                    RoundedCornerShape(topStart = 12.dp, topEnd = 6.dp, bottomStart = 8.dp, bottomEnd = 12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrincipal
        )
    }
}

data class StatItem(
    val title: String,
    val count: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun StatMiniCard(
    item: StatItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(105.dp)
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(22.dp), spotColor = item.color.copy(alpha = 0.18f))
            .border(1.dp, BordeSuave, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = MainBackgroundAlt.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(item.color.copy(alpha = 0.2f), item.color.copy(alpha = 0.06f)),
                            start = Offset(0f, 0f),
                            end = Offset(50f, 50f)
                        ),
                        RoundedCornerShape(topStart = 14.dp, topEnd = 6.dp, bottomStart = 8.dp, bottomEnd = 14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.count,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextPrincipal
            )
            Text(
                text = item.title,
                fontSize = 11.sp,
                color = TextSecundario,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
