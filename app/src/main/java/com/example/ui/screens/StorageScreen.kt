package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.data.database.*
import com.example.data.engine.IngredientSuggestion
import com.example.ui.components.*
import com.example.data.engine.IngredientSuggestionEngine
import com.example.data.engine.RecipeDraft
import com.example.data.engine.RecipeIngredientInput
import com.example.data.engine.RecipeStepInput
import com.example.data.engine.RecipeTextParser
import com.example.ui.theme.*
import com.example.ui.viewmodel.BaristaCalcViewModel
import com.example.ui.viewmodel.FreshnessResult
import com.example.ui.viewmodel.FreshnessState
import com.example.ui.viewmodel.calculateBeanFreshness
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    viewModel: BaristaCalcViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var selectedCategory by remember { mutableStateOf("Café") }
    
    // Bottom Sheet Triggers
    var activeBeanDetail by remember { mutableStateOf<Bean?>(null) }
    var activeBeanEdit by remember { mutableStateOf<Bean?>(null) }
    var isAddingNewBean by remember { mutableStateOf(false) }
    
    // Other categories creation triggers
    var isAddingNewOther by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var recipeFilterMode by remember { mutableStateOf("Todas") }

    // Recipe detail & importer triggers
    var selectedRecipeForDetail by remember { mutableStateOf<Recipe?>(null) }
    var showRecipeImporterDialog by remember { mutableStateOf(false) }
    var importedRecipeDraft by remember { mutableStateOf<RecipeDraft?>(null) }

    if (state.pendingPinDialogInstrument != null) {
        val inst = state.pendingPinDialogInstrument!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissPinPromptDialog() },
            title = { Text("¿Agregar a Calculadora?", fontWeight = FontWeight.Bold, color = TextPrincipal) },
            text = { Text("¿Deseas fijar '${inst.name}' como un acceso rápido en la calculadora del Home?", color = TextSecundario) },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmPinPromptDialog(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Sí, agregar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.confirmPinPromptDialog(false) }
                ) {
                    Text("Solo en almacén", color = TextPrincipal)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(18.dp)
        )
    }

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
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "INVENTARIO COMPLETO Y FRESCO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = TextSecundario
                )
                Text(
                    text = "Almacén Brew Studio",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrincipal
                )
            }

            Button(
                onClick = {
                    if (selectedCategory == "Café") {
                        isAddingNewBean = true
                    } else {
                        isAddingNewOther = !isAddingNewOther
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CafeCalidoOscuro),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Crear",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Ingresar",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- DASHBOARD SUMMARY CARDS FOR COFFEE BEANS ---
        if (selectedCategory == "Café") {
            StorageSummaryCards(beansList = state.beansList)
            Spacer(modifier = Modifier.height(10.dp))
        }

        // --- HORIZONTAL CATEGORY SELECTOR ---
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("Café", "Molinos", "Equipos", "Recetas", "Tazas", "Ciencia")
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CafeCalidoOscuro else SurfaceCard)
                        .border(1.dp, if (isSelected) Color.Transparent else BordeSuave, RoundedCornerShape(12.dp))
                        .clickable { 
                            selectedCategory = cat
                            isAddingNewOther = false 
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSelected) Color.White else TextSecundario
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- TAB CONTENT WORKFLOW ---
        if (isAddingNewOther && selectedCategory != "Café") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, BordeSuave, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AddingFormSelector(
                        category = selectedCategory,
                        viewModel = viewModel,
                        initialDraft = if (selectedCategory == "Recetas") importedRecipeDraft else null,
                        onCompleted = {
                            isAddingNewOther = false
                            importedRecipeDraft = null
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 64.dp)
        ) {
            when (selectedCategory) {
                "Café" -> {
                    val activeBeans = state.beansList.filter { it.status != "terminado" }
                    if (activeBeans.isEmpty()) {
                        item {
                            EmptyStateLayout(
                                text = "No hay granos de café activos en circulación. Haz clic en 'Ingresar' para archivar tu primer lote."
                            )
                        }
                    } else {
                        items(activeBeans) { bean ->
                            BeanItemCard(
                                bean = bean,
                                onDetailRequest = { activeBeanDetail = bean },
                                onBrewSelected = { viewModel.selectBeanForBrewing(bean) },
                                onLabSelected = { viewModel.selectBeanForLab(bean) },
                                onEditSelected = { activeBeanEdit = bean },
                                onDelete = { viewModel.deleteBean(bean) }
                            )
                        }
                    }
                    
                    // Display finished beans at the bottom if any
                    val finishedBeans = state.beansList.filter { it.status == "terminado" }
                    if (finishedBeans.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "LOTES HISTÓRICOS / TERMINADOS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecundario,
                                letterSpacing = 1.sp
                            )
                        }
                        items(finishedBeans) { bean ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BordeSuave.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                    .clickable { activeBeanDetail = bean },
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bean.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecundario
                                        )
                                        Text(
                                            text = "Terminado • Origen: ${bean.origin}",
                                            fontSize = 11.sp,
                                            color = TextSecundario.copy(alpha = 0.7f)
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteBean(bean) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = Advertencia.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                "Molinos" -> {
                    if (state.grindersList.isEmpty()) {
                        item {
                            EmptyStateLayout(
                                text = "No hay calibradores o molinos registrados. Haz clic en 'Ingresar' para añadir uno.",
                                actionText = "Registrar Molino",
                                onActionClick = { isAddingNewOther = true }
                            )
                        }
                    } else {
                        items(state.grindersList) { grinder ->
                            GrinderItemCard(grinder = grinder, onDelete = { viewModel.deleteGrinder(grinder) })
                        }
                    }
                }
                "Equipos" -> {
                    if (state.equipmentList.isEmpty()) {
                        item {
                            EmptyStateLayout(
                                text = "No hay métodos de extracción, teteras ni básculas registradas. Haz clic en 'Ingresar' para añadir tu equipo.",
                                actionText = "Registrar Equipo",
                                onActionClick = { isAddingNewOther = true }
                            )
                        }
                    } else {
                        items(state.equipmentList) { eq ->
                            val isMethodEquipment = eq.type == "BREWER_METHOD" || eq.type == "BREW_METHOD" || eq.type.contains("metodo", ignoreCase = true) || eq.type.contains("método", ignoreCase = true)
                            val methodPref = state.userMethods.find { it.sourceInstrumentId == eq.id }
                            EquipmentItemCard(
                                eq = eq,
                                isPinned = if (isMethodEquipment) (methodPref?.isPinnedToCalculator ?: false) else null,
                                onTogglePinned = if (isMethodEquipment) { { viewModel.toggleMethodPinnedForInstrument(eq.id) } } else null,
                                onDelete = { viewModel.deleteEquipment(eq) }
                            )
                        }
                    }
                }
                "Recetas" -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = { Text("Buscar en recetario...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                                    trailingIcon = {
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { showRecipeImporterDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AcentoPrincipal)
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Importar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val filterChips = listOf("Todas", "Favoritas", "Café Negro", "Leche", "Bebida Fría", "Autor", "Postre", "Otro")
                                items(filterChips) { chip ->
                                    val isSelected = recipeFilterMode == chip
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) CafeCalidoOscuro else SurfaceCard)
                                            .border(1.dp, if (isSelected) Color.Transparent else BordeSuave, RoundedCornerShape(8.dp))
                                            .clickable { recipeFilterMode = chip }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(chip, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else TextSecundario)
                                    }
                                }
                            }
                        }
                    }

                    val filteredRecipes = state.recipesList.filter { recipe ->
                        val matchesSearch = searchQuery.isBlank() ||
                                recipe.name.contains(searchQuery, ignoreCase = true) ||
                                recipe.ingredientsSummary.contains(searchQuery, ignoreCase = true) ||
                                recipe.stepsSummary.contains(searchQuery, ignoreCase = true) ||
                                recipe.intention.contains(searchQuery, ignoreCase = true) ||
                                recipe.tags.contains(searchQuery, ignoreCase = true)
                        val matchesFilter = when (recipeFilterMode) {
                            "Favoritas" -> recipe.isFavorite
                            "Todas" -> true
                            "Café Negro" -> recipe.recipeKind == "BLACK_COFFEE"
                            "Leche" -> recipe.recipeKind == "MILK_DRINK"
                            "Bebida Fría" -> recipe.recipeKind == "COLD_DRINK"
                            "Autor" -> recipe.recipeKind == "SIGNATURE"
                            "Postre" -> recipe.recipeKind == "DESSERT"
                            "Otro" -> recipe.recipeKind == "OTHER"
                            else -> true
                        }
                        matchesSearch && matchesFilter
                    }

                    if (filteredRecipes.isEmpty()) {
                        item {
                            EmptyStateLayout(
                                text = "No has guardado recetas. Crea tu primera receta o importa texto libre.",
                                actionText = "Crear Receta",
                                onActionClick = { isAddingNewOther = true }
                            )
                        }
                    } else {
                        items(filteredRecipes) { recipe ->
                            RecipeItemCard(
                                recipe = recipe,
                                onClick = { selectedRecipeForDetail = recipe },
                                onDelete = { viewModel.deleteRecipe(recipe) },
                                onFavoriteToggle = { viewModel.toggleRecipeFavorite(recipe) },
                                onEdit = {
                                    importedRecipeDraft = recipe.toRecipeDraft(isClone = false)
                                    selectedCategory = "Recetas"
                                    isAddingNewOther = true
                                },
                                onClone = {
                                    importedRecipeDraft = recipe.toRecipeDraft(isClone = true)
                                    selectedCategory = "Recetas"
                                    isAddingNewOther = true
                                }
                            )
                        }
                    }
                }
                "Tazas" -> {
                    if (state.cupsList.isEmpty()) {
                        item {
                            EmptyStateLayout(
                                text = "No hay registros históricos de tazas catadas. Haz clic en 'Ingresar' para evaluar tu primer café.",
                                actionText = "Archivar Taza",
                                onActionClick = { isAddingNewOther = true }
                            )
                        }
                    } else {
                        items(state.cupsList) { cup ->
                            CupItemCard(cup = cup, onDelete = { viewModel.deleteCup(cup) })
                        }
                    }
                }
                "Ciencia" -> {
                    if (state.experimentsList.isEmpty()) {
                        item {
                            EmptyStateLayout(
                                text = "No hay experimentos de laboratorio archivados. Haz clic en 'Ingresar' para registrar tu hipótesis.",
                                actionText = "Registrar Experimento",
                                onActionClick = { isAddingNewOther = true }
                            )
                        }
                    } else {
                        items(state.experimentsList) { exp ->
                            ExperimentItemCard(exp = exp, onDelete = { viewModel.deleteExperiment(exp) })
                        }
                    }
                }
            }
        }
    }
}

    // --- BOTTOM SHEETS TRIGGER HANDLING ---

    // Detail Sheet
    if (activeBeanDetail != null) {
        BeanDetailSheet(
            bean = activeBeanDetail!!,
            viewModel = viewModel,
            onDismiss = { activeBeanDetail = null },
            onEdit = {
                val bean = activeBeanDetail
                activeBeanDetail = null
                activeBeanEdit = bean
            }
        )
    }

    // New/Add Bean Sheet
    if (isAddingNewBean) {
        AddEditBeanSheet(
            beanToEdit = null,
            viewModel = viewModel,
            onDismiss = { isAddingNewBean = false }
        )
    }

    // Edit Bean Sheet
    if (activeBeanEdit != null) {
        AddEditBeanSheet(
            beanToEdit = activeBeanEdit,
            viewModel = viewModel,
            onDismiss = { activeBeanEdit = null }
        )
    }

    // Recipe Detail Dialog
    selectedRecipeForDetail?.let { recipe ->
        SavedRecipeDetailDialog(
            recipe = recipe,
            onDismiss = { selectedRecipeForDetail = null },
            onDelete = {
                viewModel.deleteRecipe(recipe)
                selectedRecipeForDetail = null
            },
            onFavoriteToggle = {
                viewModel.toggleRecipeFavorite(recipe)
                selectedRecipeForDetail = recipe.copy(isFavorite = !recipe.isFavorite)
            },
            onEdit = {
                importedRecipeDraft = recipe.toRecipeDraft(isClone = false)
                selectedCategory = "Recetas"
                isAddingNewOther = true
                selectedRecipeForDetail = null
            },
            onClone = {
                importedRecipeDraft = recipe.toRecipeDraft(isClone = true)
                selectedCategory = "Recetas"
                isAddingNewOther = true
                selectedRecipeForDetail = null
            }
        )
    }

    // Recipe Importer Dialog
    if (showRecipeImporterDialog) {
        RecipeImporterDialog(
            onDismiss = { showRecipeImporterDialog = false },
            onDraftParsed = { draft ->
                importedRecipeDraft = draft
                showRecipeImporterDialog = false
                isAddingNewOther = true
                selectedCategory = "Recetas"
            }
        )
    }
}

// --- STORAGE DASHBOARD METRIC SUMMARY CARDS ---

@Composable
fun StorageSummaryCards(beansList: List<Bean>) {
    val activeBeans = beansList.filter { it.status != "terminado" }
    
    val calculated = activeBeans.map { calculateBeanFreshness(it.roastDate, it.firstUseDate) }
    
    val totalCount = activeBeans.size
    val idealCount = calculated.count { it.freshnessState == FreshnessState.Ideal }
    val decliningCount = calculated.count { it.freshnessState == FreshnessState.Declining }
    val oldCount = calculated.count { it.freshnessState == FreshnessState.Old }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MiniSummaryCard(
            title = "Granos Activos",
            count = totalCount,
            tint = AcentoPrincipal,
            icon = Icons.Default.AllInbox
        )
        MiniSummaryCard(
            title = "Punto Ideal",
            count = idealCount,
            tint = Color(0xFFC28B46),
            icon = Icons.Default.Star
        )
        MiniSummaryCard(
            title = "Bajando",
            count = decliningCount,
            tint = Color(0xFFB76545),
            icon = Icons.Default.TrendingDown
        )
        MiniSummaryCard(
            title = "Lotes Viejos",
            count = oldCount,
            tint = Color(0xFF8C5A2B),
            icon = Icons.Default.HourglassEmpty
        )
    }
}

@Composable
fun MiniSummaryCard(
    title: String,
    count: Int,
    tint: Color,
    icon: ImageVector
) {
    Card(
        modifier = Modifier
            .width(135.dp)
            .border(1.dp, BordeSuave, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(tint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
                }
                Text(
                    text = "$count",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrincipal
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- BEAN FRESHNESS GRAPH (CUSTOM DESIGN CHIP) ---

@Composable
fun BeanFreshnessGraph(
    freshnessResult: FreshnessResult,
    modifier: Modifier = Modifier
) {
    val progress = freshnessResult.freshnessProgress
    val daysLabel = if (freshnessResult.daysFromRoast != null) {
        "Día ${freshnessResult.daysFromRoast} desde tostado"
    } else {
        "Sin datos de tueste"
    }

    val daysOpenLabel = if (freshnessResult.daysFromOpen != null) {
        "• Abierto hace ${freshnessResult.daysFromOpen} d"
    } else {
        ""
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$daysLabel $daysOpenLabel",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AcentoPrincipal
            )
            Text(
                text = "${(progress * 100).toInt()}% est. útil",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecundario
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))

        // Draw horizontal continuous gradient metric bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF84AD92), // Muy fresco
                            Color(0xFF3F7A63), // En ventana
                            Color(0xFFC28B46), // Preferido
                            Color(0xFFB76545), // Bajando
                            Color(0xFF8C5A2B)  // Viejo
                        )
                    )
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = size.width
                val barHeight = size.height

                // Draw pointer marker
                if (freshnessResult.daysFromRoast != null) {
                    val pointerX = barWidth * progress
                    
                    // Outer glow/shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.2f),
                        radius = 11f,
                        center = Offset(pointerX, barHeight / 2)
                    )
                    
                    // White border
                    drawCircle(
                        color = Color.White,
                        radius = 8f,
                        center = Offset(pointerX, barHeight / 2)
                    )
                    
                    // Active color center
                    val centerColor = when (freshnessResult.freshnessState) {
                        FreshnessState.NoDate -> Color(0xFF60756A)
                        FreshnessState.VeryFresh -> Color(0xFF84AD92)
                        FreshnessState.InWindow -> Color(0xFF3F7A63)
                        FreshnessState.Ideal -> Color(0xFFC28B46)
                        FreshnessState.Declining -> Color(0xFFB76545)
                        FreshnessState.Old -> Color(0xFF8C5A2B)
                    }
                    drawCircle(
                        color = centerColor,
                        radius = 5.2f,
                        center = Offset(pointerX, barHeight / 2)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val labelStyle = androidx.compose.ui.text.TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecundario)
            Text("Muy fresco", style = labelStyle)
            Text("En ventana", style = labelStyle)
            Text("PREFERIDO", style = labelStyle, color = CafeCalidoOscuro, fontWeight = FontWeight.ExtraBold)
            Text("Bajando", style = labelStyle)
            Text("Viejo", style = labelStyle)
        }
    }
}

// --- COFFEE BEAN ITEM PREMIUM CARD ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BeanItemCard(
    bean: Bean,
    onDetailRequest: () -> Unit,
    onBrewSelected: () -> Unit,
    onLabSelected: () -> Unit,
    onEditSelected: () -> Unit,
    onDelete: () -> Unit
) {
    val freshnessResult = remember(bean) { calculateBeanFreshness(bean.roastDate, bean.firstUseDate) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.05f))
            .border(1.dp, BordeSuave, RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onDetailRequest,
                onLongClick = onEditSelected
            ),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: brand name and state chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bean.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = bean.roaster.ifBlank { "Tostador Desconocido" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecundario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Chip badge with exact Hex color mapping
                val stateColor = Color(android.graphics.Color.parseColor(freshnessResult.freshnessState.colorHex))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(stateColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = freshnessResult.freshnessState.label.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = stateColor
                    )
                }
            }

            // Specs section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SpecChip(label = "Origen", value = bean.origin)
                SpecChip(label = "Proceso", value = bean.process)
                SpecChip(label = "Estado", value = bean.status)
            }

            // Continuous Degradation Graph
            BeanFreshnessGraph(freshnessResult = freshnessResult)

            // Warning if opened too long
            if (freshnessResult.openWarning != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Advertencia.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Advertencia, modifier = Modifier.size(13.dp))
                    Text(text = freshnessResult.openWarning, fontSize = 10.sp, color = Advertencia, fontWeight = FontWeight.SemiBold)
                }
            }

            // Action Quick Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stock: ${bean.stockGrams}g",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CafeCalidoOscuro
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit bean
                    IconButton(
                        onClick = onEditSelected,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF2F7F3))
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = TextSecundario, modifier = Modifier.size(16.dp))
                    }

                    // Lab
                    IconButton(
                        onClick = onLabSelected,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AcentoSecundario.copy(alpha = 0.12f))
                    ) {
                        Icon(imageVector = Icons.Default.Science, contentDescription = "Usar en Laboratorio", tint = AcentoSecundario, modifier = Modifier.size(16.dp))
                    }

                    // Brew
                    Button(
                        onClick = onBrewSelected,
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.LocalCafe, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preparar", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }

                    // Quick delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Borrar", tint = Advertencia.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SpecChip(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFFAF5))
            .border(1.dp, BordeSuave.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$label: $value",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecundario,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- COFFEE DETAILS BOTTOM SHEET ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeanDetailSheet(
    bean: Bean,
    viewModel: BaristaCalcViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val freshnessResult = remember(bean) { calculateBeanFreshness(bean.roastDate, bean.firstUseDate) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Name and Roaster
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bean.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrincipal
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                Text(
                    text = "Tostador: ${bean.roaster.ifBlank { "N/A" }}",
                    fontSize = 14.sp,
                    color = TextSecundario,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Divider(color = BordeSuave.copy(alpha = 0.4f))
            
            // Freshness Highlights
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("MADURACIÓN ACTUAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecundario, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    val stateColor = Color(android.graphics.Color.parseColor(freshnessResult.freshnessState.colorHex))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(stateColor.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = freshnessResult.freshnessState.label.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = stateColor
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text("ESTADO BOLSA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecundario, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (bean.status == "abierto") AcentoSuave else BordeSuave.copy(alpha = 0.3f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = bean.status.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (bean.status == "abierto") AcentoPrincipal else TextSecundario
                        )
                    }
                }
            }

            // Big Freshness Graph
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BordeSuave, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Curva de Maduración Estimada",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrincipal
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    BeanFreshnessGraph(freshnessResult = freshnessResult)
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "Recomendación:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrincipal
                    )
                    Text(
                        text = freshnessResult.recommendation,
                        fontSize = 12.sp,
                        color = TextSecundario,
                        lineHeight = 16.sp
                    )

                    if (freshnessResult.openWarning != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Advertencia.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Advertencia, modifier = Modifier.size(14.dp))
                            Text(text = freshnessResult.openWarning, fontSize = 11.sp, color = Advertencia, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            
            // Specifications
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BordeSuave, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpecRow(label = "Procedencia/Origen", value = bean.origin)
                    SpecRow(label = "Altura", value = bean.altitude.ifBlank { "No disponible" })
                    SpecRow(label = "Proceso", value = bean.process)
                    SpecRow(label = "Notas Sensoriales", value = bean.notes.ifBlank { "Ninguna" })
                    SpecRow(label = "Fecha Tueste", value = bean.roastDate.ifBlank { "Sin registrar" })
                    SpecRow(
                        label = "Fecha de Apertura", 
                        value = if (bean.firstUseDate.isBlank()) "Cerrado / Hermético" else bean.firstUseDate
                    )
                    SpecRow(label = "Stock", value = "${bean.stockGrams}g")
                }
            }

            // Immediate actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.selectBeanForBrewing(bean)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.LocalCafe, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("En Preparar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        viewModel.selectBeanForLab(bean)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AcentoSecundario),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Llevar a Lab", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Supplementary triggers Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (bean.status != "abierto" && bean.status != "terminado") {
                    OutlinedButton(
                        onClick = {
                            viewModel.markBeanAsOpened(bean)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CafeCalidoOscuro)
                    ) {
                        Text("Abrir bolsa hoy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (bean.status != "terminado") {
                    OutlinedButton(
                        onClick = {
                            viewModel.markBeanAsFinished(bean)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Advertencia)
                    ) {
                        Text("Terminado", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { onEdit() },
                    modifier = Modifier
                        .weight(0.9f)
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecundario, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 12.sp, color = TextPrincipal, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// --- ADD OR EDIT COFFEE BEAN SHEET ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBeanSheet(
    beanToEdit: Bean?,
    viewModel: BaristaCalcViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(beanToEdit?.name ?: "") }
    var roaster by remember { mutableStateOf(beanToEdit?.roaster ?: "") }
    var origin by remember { mutableStateOf(beanToEdit?.origin ?: "") }
    var altitude by remember { mutableStateOf(beanToEdit?.altitude ?: "") }
    var process by remember { mutableStateOf(beanToEdit?.process ?: "") }
    var roastDate by remember { mutableStateOf(beanToEdit?.roastDate ?: "") }
    var firstUseDate by remember { mutableStateOf(beanToEdit?.firstUseDate ?: "") }
    var notes by remember { mutableStateOf(beanToEdit?.notes ?: "") }
    var status by remember { mutableStateOf(beanToEdit?.status ?: "cerrado") }
    var stockGrams by remember { mutableStateOf(beanToEdit?.stockGrams?.toString() ?: "210") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MainBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        FormAtmosphereBackground {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FormHeaderWithBlob(
                    title = if (beanToEdit != null) "Editar Lote de Café" else "Registrar Nuevo Grano",
                    subtitle = "Almacén sensorial y trazabilidad del origen",
                    icon = Icons.Default.Inventory2,
                    onClose = onDismiss
                )

                // Sub-Card 1: General Info
                FormSubCard(
                    title = "Datos del Tostador y Lote",
                    titleIcon = Icons.Default.Coffee
                ) {
                    StyledOutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Nombre del Grano *",
                        placeholder = "ej. Geisha Esmeralda Special Reserve"
                    )

                    StyledOutlinedTextField(
                        value = roaster,
                        onValueChange = { roaster = it },
                        label = "Tostador / Marca *",
                        placeholder = "ej. Brewther Specialty Roasters"
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StyledOutlinedTextField(
                            value = origin,
                            onValueChange = { origin = it },
                            label = "Lugar de Origen",
                            placeholder = "ej. Boquete, Panamá",
                            modifier = Modifier.weight(1f)
                        )
                        StyledOutlinedTextField(
                            value = altitude,
                            onValueChange = { altitude = it },
                            label = "Altura (msnm)",
                            placeholder = "ej. 1750",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Sub-Card 2: Process & Dates
                FormSubCard(
                    title = "Proceso, Fechas y Stock",
                    titleIcon = Icons.Default.DateRange
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StyledOutlinedTextField(
                            value = process,
                            onValueChange = { process = it },
                            label = "Proceso (Beneficio)",
                            placeholder = "ej. Natural Anaeróbico",
                            modifier = Modifier.weight(1.2f)
                        )
                        StyledOutlinedTextField(
                            value = stockGrams,
                            onValueChange = { stockGrams = it },
                            label = "Stock (Gramos)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StyledOutlinedTextField(
                            value = roastDate,
                            onValueChange = { roastDate = it },
                            label = "Fecha Tostado",
                            placeholder = "YYYY-MM-DD",
                            modifier = Modifier.weight(1f)
                        )
                        StyledOutlinedTextField(
                            value = firstUseDate,
                            onValueChange = { firstUseDate = it },
                            label = "Fecha Apertura",
                            placeholder = "YYYY-MM-DD",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "ESTADO DE CONSERVACIÓN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecundario,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("cerrado", "abierto", "terminado").forEach { st ->
                            val isSel = status == st
                            val chipLabel = when(st) {
                                "cerrado" -> "🔒 CERRADO"
                                "abierto" -> "☕ ABIERTO"
                                else -> "🏁 TERMINADO"
                            }
                            StyledCategoryChip(
                                label = chipLabel,
                                isSelected = isSel,
                                onClick = {
                                    status = st
                                    if (st == "abierto" && firstUseDate.isBlank()) {
                                        firstUseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Sub-Card 3: Sensorial profile
                FormSubCard(
                    title = "Perfil Sensorial y Descriptores",
                    titleIcon = Icons.Default.Psychology
                ) {
                    StyledOutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Descriptores / Perfil de Catación",
                        placeholder = "ej. Jazmín, té negro, bergamota, acidez cítrica prolongada",
                        maxLines = 3
                    )
                }

                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StyledPrimaryButton(
                        text = if (beanToEdit != null) "Guardar Cambios" else "Archivar Grano en Almacén",
                        icon = Icons.Default.Check,
                        onClick = {
                            if (name.isNotBlank() && roaster.isNotBlank()) {
                                viewModel.saveBean(
                                    id = beanToEdit?.id,
                                    roaster = roaster,
                                    name = name,
                                    origin = origin,
                                    altitude = altitude,
                                    process = process,
                                    roastDate = roastDate,
                                    firstUseDate = firstUseDate,
                                    notes = notes,
                                    status = status,
                                    stockGrams = stockGrams.toFloatOrNull() ?: 250f
                                )
                                onDismiss()
                            }
                        }
                    )

                    StyledSecondaryButton(
                        text = "Cancelar",
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

// --- STANDARD ITEM CARDS PRESERVATION ---

@Composable
fun GrinderItemCard(grinder: Instrument, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, BordeSuave, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(AcentoSecundario.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Handyman, contentDescription = null, tint = AcentoSecundario, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(grinder.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                if (grinder.notes.isNotBlank()) {
                    Text(grinder.notes, fontSize = 11.sp, color = TextSecundario)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = Advertencia.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun EquipmentItemCard(
    eq: Instrument,
    isPinned: Boolean? = null,
    onTogglePinned: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, BordeSuave, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)).background(CafeCalidoClaro.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Hardware, contentDescription = null, tint = CafeCalidoClaro, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(eq.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                Text("Tipo: ${eq.type.uppercase()}", fontSize = 11.sp, color = CafeCalidoOscuro, fontWeight = FontWeight.Bold)
                if (eq.notes.isNotBlank()) {
                    Text(eq.notes, fontSize = 11.sp, color = TextSecundario)
                }
                if (isPinned != null && onTogglePinned != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPinned) AccentGold.copy(alpha = 0.15f) else SurfaceElevated)
                            .border(1.dp, if (isPinned) AccentGold else BordeSuave, RoundedCornerShape(8.dp))
                            .clickable { onTogglePinned() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = if (isPinned) AccentGold else TextSecundario,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isPinned) "En calculadora" else "Solo en almacén",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPinned) AccentGold else TextSecundario
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = Advertencia.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

fun Recipe.toRecipeDraft(isClone: Boolean = false): RecipeDraft {
    val parsedIngs = IngredientSuggestionEngine.parseIngredientsSummary(ingredientsSummary)
    val ingInputs = if (parsedIngs.isNotEmpty()) {
        parsedIngs.map { p -> RecipeIngredientInput(name = p.name, amount = p.amount, unit = p.unit.ifBlank { "G" }) }
    } else if (ingredientsSummary.isNotBlank()) {
        listOf(RecipeIngredientInput(name = ingredientsSummary, amount = "", unit = "G"))
    } else emptyList()

    val stepInputs = if (stepsSummary.isNotBlank()) {
        stepsSummary.lines().map { line ->
            val cleanInst = line.replace(Regex("""^\d+[\.\)-]\s*"""), "").trim()
            RecipeStepInput(instruction = cleanInst)
        }.filter { it.instruction.isNotBlank() }
    } else emptyList()

    return RecipeDraft(
        id = if (isClone) java.util.UUID.randomUUID().toString() else this.id,
        name = if (isClone) "Copia de ${this.name}" else this.name,
        recipeKind = this.recipeKind,
        intention = this.intention,
        suggestedMethod = this.suggestedMethodId ?: this.legacyMethodName ?: "",
        ingredients = ingInputs,
        steps = stepInputs,
        tags = this.tags,
        isFavorite = this.isFavorite
    )
}

@Composable
fun RecipeItemCard(
    recipe: Recipe,
    onClick: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onFavoriteToggle: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onClone: (() -> Unit)? = null
) {
    val (kindLabel, kindColor, kindIcon) = when (recipe.recipeKind) {
        "BLACK_COFFEE" -> Triple("Café Negro", Color(0xFF234E3C), Icons.Default.LocalCafe)
        "MILK_DRINK" -> Triple("Bebida con Leche", Color(0xFFD97706), Icons.Default.EmojiFoodBeverage)
        "COLD_DRINK" -> Triple("Bebida Fría", Color(0xFF0D9488), Icons.Default.AcUnit)
        "SIGNATURE" -> Triple("Bebida de Autor", Color(0xFFC86D51), Icons.Default.AutoAwesome)
        "DESSERT" -> Triple("Postre", Color(0xFFE11D48), Icons.Default.Cake)
        else -> Triple("Fórmula", Color(0xFF4B6584), Icons.Default.ReceiptLong)
    }

    val suggestedMethod = recipe.suggestedMethodId ?: recipe.legacyMethodName ?: ""

    val parsedIngredients = remember(recipe.ingredientsSummary) {
        IngredientSuggestionEngine.parseIngredientsSummary(recipe.ingredientsSummary)
    }
    val stepCount = remember(recipe.stepsSummary) {
        if (recipe.stepsSummary.isBlank()) 0
        else recipe.stepsSummary.lines().count { it.isNotBlank() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BordeSuave, RoundedCornerShape(20.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category blob icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(kindColor.copy(alpha = 0.15f))
                        .border(1.dp, kindColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = kindIcon,
                        contentDescription = null,
                        tint = kindColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.name,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrincipal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = kindLabel,
                        fontSize = 11.sp,
                        color = kindColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (onFavoriteToggle != null) {
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (recipe.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorita",
                            tint = if (recipe.isFavorite) Color(0xFFFFB300) else TextSecundario,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (onClone != null) {
                    IconButton(onClick = onClone) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Clonar",
                            tint = AcentoPrincipal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = CafeCalidoOscuro,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Advertencia.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Visual Summary Badges (Rule: "ícono/color > dato > texto")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (suggestedMethod.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CafeCalidoOscuro.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CoffeeMaker, contentDescription = null, tint = CafeCalidoOscuro, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(suggestedMethod, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CafeCalidoOscuro)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AcentoPrincipal.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = AcentoPrincipal, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${parsedIngredients.size} ingr", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AcentoPrincipal)
                    }
                }

                if (stepCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TextSecundario.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FormatListNumbered, contentDescription = null, tint = TextSecundario, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$stepCount pasos", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecundario)
                        }
                    }
                }
            }

            if (recipe.intention.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = recipe.intention,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    color = TextSecundario,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                )
            }
        }
    }
}

@Composable
fun SavedRecipeDetailDialog(
    recipe: Recipe,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onClone: (() -> Unit)? = null
) {
    val (kindLabel, kindColor, kindIcon) = when (recipe.recipeKind) {
        "BLACK_COFFEE" -> Triple("Café Negro", Color(0xFF234E3C), Icons.Default.LocalCafe)
        "MILK_DRINK" -> Triple("Bebida con Leche", Color(0xFFD97706), Icons.Default.EmojiFoodBeverage)
        "COLD_DRINK" -> Triple("Bebida Fría", Color(0xFF0D9488), Icons.Default.AcUnit)
        "SIGNATURE" -> Triple("Bebida de Autor", Color(0xFFC86D51), Icons.Default.AutoAwesome)
        "DESSERT" -> Triple("Postre", Color(0xFFE11D48), Icons.Default.Cake)
        else -> Triple("Fórmula", Color(0xFF4B6584), Icons.Default.ReceiptLong)
    }

    val suggestedMethod = recipe.suggestedMethodId ?: recipe.legacyMethodName ?: ""

    val parsedIngredients = remember(recipe.ingredientsSummary) {
        IngredientSuggestionEngine.parseIngredientsSummary(recipe.ingredientsSummary)
    }

    val parsedSteps = remember(recipe.stepsSummary) {
        if (recipe.stepsSummary.isBlank()) emptyList()
        else recipe.stepsSummary.lines().map { it.replace(Regex("""^\d+[\.\)-]\s*"""), "").trim() }.filter { it.isNotBlank() }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceCard,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(kindColor.copy(alpha = 0.15f))
                            .border(1.dp, kindColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = kindIcon, contentDescription = null, tint = kindColor, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recipe.name,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = TextPrincipal
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(kindColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(kindLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = kindColor)
                            }
                            if (suggestedMethod.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CafeCalidoOscuro.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(suggestedMethod, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CafeCalidoOscuro)
                                }
                            }
                        }
                    }
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (recipe.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorita",
                            tint = if (recipe.isFavorite) Color(0xFFFFB300) else TextSecundario,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecundario)
                    }
                }

            // Intention Quote Box
            if (recipe.intention.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MainBackground)
                        .border(1.dp, BordeSuave, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CafeCalidoOscuro)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "\"${recipe.intention}\"",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Serif,
                            color = TextPrincipal,
                            style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = BordeSuave, thickness = 0.8.dp)

            // Ingredients Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = AcentoPrincipal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ingredientes", fontSize = 14.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = TextPrincipal)
                }

                parsedIngredients.forEach { ing ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MainBackground),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, BordeSuave, RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(kindColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(ing.name, fontSize = 13.sp, color = TextPrincipal, fontWeight = FontWeight.Medium)
                            }
                            if (ing.displayQuantity.isNotBlank()) {
                                Text(
                                    text = ing.displayQuantity,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = AcentoPrincipal
                                )
                            }
                        }
                    }
                }
            }

            // Steps Section
            if (parsedSteps.isNotEmpty()) {
                HorizontalDivider(color = BordeSuave, thickness = 0.8.dp)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = null, tint = CafeCalidoOscuro, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pasos de Preparación", fontSize = 14.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = TextPrincipal)
                    }

                    parsedSteps.forEachIndexed { idx, stepText ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MainBackground),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().border(1.dp, BordeSuave, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(kindColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${idx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(stepText, fontSize = 12.sp, color = TextPrincipal, lineHeight = 16.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Bottom action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Advertencia)
                }
                Spacer(modifier = Modifier.weight(1f))
                if (onClone != null) {
                    OutlinedButton(
                        onClick = onClone,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AcentoPrincipal),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AcentoPrincipal.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clonar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                if (onEdit != null) {
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = CafeCalidoOscuro),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecundario)
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeImporterDialog(
    onDismiss: () -> Unit,
    onDraftParsed: (RecipeDraft) -> Unit
) {
    var rawText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = AcentoPrincipal, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Importador Inteligente de Recetas", fontSize = 16.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Pega el texto libre de cualquier receta de café (de un blog, mensaje o nota). El parser detectará ingredientes, cantidades, pasos y categoría automáticamente.",
                    fontSize = 12.sp,
                    color = TextSecundario
                )
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = {
                        Text(
                            "Ejemplo:\nReceta: Espresso Tonic Menta\nIngredientes:\n- 30 ml Espresso extraído\n- 150 ml Agua tónica\n- 2 unidades Hielo\nPasos:\n1. Servir tónica y hielo\n2. Verter espresso",
                            fontSize = 11.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rawText.isNotBlank()) {
                        val draft = RecipeTextParser.parse(rawText)
                        onDraftParsed(draft)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CafeCalidoOscuro),
                shape = RoundedCornerShape(12.dp),
                enabled = rawText.isNotBlank()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Procesar e Importar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun CupItemCard(cup: Cup, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, BordeSuave, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(CafeCalidoOscuro.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.LocalCafe, contentDescription = null, tint = CafeCalidoOscuro, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(cup.beanNameSnapshot, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                    Text("Taza • ${cup.executedDoseG}g ➔ ${cup.executedWaterMl}ml • Score: ${cup.rating ?: 5.0}★", fontSize = 11.sp, color = TextSecundario)
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = Advertencia.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
            
            HorizontalDivider(color = BordeSuave.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AttributeTag(text = cup.cupLifeState, color = AcentoPrincipal)
                AttributeTag(text = "Molienda: ${cup.executedGrindSetting}", color = CafeCalidoClaro)
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (cup.comment.isNotBlank()) {
                Text(cup.comment, fontSize = 10.sp, color = TextSecundario)
            }
        }
    }
}

@Composable
fun ExperimentItemCard(exp: LabExperiment, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, BordeSuave, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(AcentoSecundario.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Science, contentDescription = null, tint = AcentoSecundario, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Experimento: 1:${exp.ratio}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                    Text("${exp.coffeeGrams}g • ${exp.waterMl}ml • ${exp.temperatureC}°C • ${exp.grindSetting} clks", fontSize = 11.sp, color = TextSecundario)
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = Advertencia.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
            if (exp.experimentNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(exp.experimentNotes, fontSize = 11.sp, color = TextSecundario, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
fun AttributeTag(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = color)
    }
}

// --- HORIZONTAL SUBMISSION FORMS CHANGER ---

@Composable
fun AddingFormSelector(
    category: String,
    viewModel: BaristaCalcViewModel,
    initialDraft: RecipeDraft? = null,
    onCompleted: () -> Unit
) {
    val formScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()

    FormAtmosphereBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(formScrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (category) {
                "Molinos" -> {
                    var brand by remember { mutableStateOf("") }
                    var model by remember { mutableStateOf("") }
                    var clickRange by remember { mutableStateOf("0 - 40 clicks") }
                    var calibracion by remember { mutableStateOf("") }

                    FormHeaderWithBlob(
                        title = "Registrar Molino del Taller",
                        subtitle = "Equipamiento de molienda y rangos de calibración",
                        icon = Icons.Default.Settings,
                        onClose = onCompleted
                    )

                    FormSubCard(
                        title = "Especificaciones del Molino",
                        titleIcon = Icons.Default.Tune
                    ) {
                        StyledOutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = "Marca (ej. Comandante, Timemore, Fellow)"
                        )
                        StyledOutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = "Modelo * (ej. C40 MK4, Chestnut C3)"
                        )
                        StyledOutlinedTextField(
                            value = clickRange,
                            onValueChange = { clickRange = it },
                            label = "Rango de Clicks Operativo",
                            placeholder = "ej. 12 - 28 clicks"
                        )
                        StyledOutlinedTextField(
                            value = calibracion,
                            onValueChange = { calibracion = it },
                            label = "Notas de Calibración",
                            placeholder = "ej. Punto cero en click 0, molienda uniforme"
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        StyledPrimaryButton(
                            text = "Archivar Molino",
                            icon = Icons.Default.Check,
                            onClick = {
                                if (model.isNotBlank()) {
                                    viewModel.addGrinder(brand, model, clickRange, calibracion)
                                    onCompleted()
                                }
                            }
                        )
                        StyledSecondaryButton(
                            text = "Cancelar",
                            onClick = onCompleted
                        )
                    }
                }
                "Recetas" -> {
                    var name by remember(initialDraft) { mutableStateOf(initialDraft?.name ?: "") }
                    var recipeKind by remember(initialDraft) { mutableStateOf(initialDraft?.recipeKind ?: "BLACK_COFFEE") }
                    var intention by remember(initialDraft) { mutableStateOf(initialDraft?.intention ?: "") }
                    var suggestedMethod by remember(initialDraft) { mutableStateOf(initialDraft?.suggestedMethod ?: "") }
                    var tagsText by remember(initialDraft) { mutableStateOf(initialDraft?.tags ?: "") }
                    var isFavorite by remember(initialDraft) { mutableStateOf(initialDraft?.isFavorite ?: false) }

                    val ingredientsList = remember(initialDraft) {
                        if (!initialDraft?.ingredients.isNullOrEmpty()) {
                            mutableStateListOf<RecipeIngredientInput>().apply { addAll(initialDraft!!.ingredients) }
                        } else {
                            mutableStateListOf(
                                RecipeIngredientInput(name = "Café de especialidad", amount = "18", unit = "G"),
                                RecipeIngredientInput(name = "Agua filtrada o Leche", amount = "150", unit = "ML")
                            )
                        }
                    }

                    val stepsList = remember(initialDraft) {
                        if (!initialDraft?.steps.isNullOrEmpty()) {
                            mutableStateListOf<RecipeStepInput>().apply { addAll(initialDraft!!.steps) }
                        } else {
                            mutableStateListOf(
                                RecipeStepInput(instruction = "Añadir la base y combinar los ingredientes"),
                                RecipeStepInput(instruction = "Servir a temperatura adecuada y disfrutar")
                            )
                        }
                    }

                    val suggestionIndex = remember(state.recipesList, state.beansList) {
                        IngredientSuggestionEngine.buildIndex(state.recipesList, beans = state.beansList)
                    }

                    fun addNewIngredientRow() {
                        ingredientsList.add(RecipeIngredientInput(name = "", amount = "", unit = "G"))
                        coroutineScope.launch {
                            delay(80)
                            formScrollState.animateScrollTo(formScrollState.maxValue)
                        }
                    }

                    fun addNewStepRow() {
                        stepsList.add(RecipeStepInput(instruction = ""))
                        coroutineScope.launch {
                            delay(80)
                            formScrollState.animateScrollTo(formScrollState.maxValue)
                        }
                    }

                    val isEditingExisting = initialDraft != null && state.recipesList.any { it.id == initialDraft.id }
                    val headerTitle = when {
                        isEditingExisting -> "Editar Receta Guardada"
                        initialDraft != null && initialDraft.name.startsWith("Copia de ") -> "Clonar y Personalizar Receta"
                        initialDraft != null -> "Revisar Receta Importada"
                        else -> "Nueva Receta"
                    }

                    FormHeaderWithBlob(
                        title = headerTitle,
                        subtitle = "Fórmula e ingredientes de bebida de autor",
                        icon = Icons.Default.LocalDrink,
                        onClose = onCompleted
                    )

                    FormSubCard(
                        title = "Datos Principales",
                        titleIcon = Icons.Default.ReceiptLong
                    ) {
                        StyledOutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Nombre de la Receta *",
                            placeholder = "ej. Espresso Tonic de Autor, Flat White"
                        )

                        Text("Categoría / Tipo de Bebida:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecundario)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val kinds = listOf(
                                "BLACK_COFFEE" to "Café Negro",
                                "MILK_DRINK" to "Leche",
                                "COLD_DRINK" to "Bebida Fría",
                                "SIGNATURE" to "Autor",
                                "DESSERT" to "Postre",
                                "OTHER" to "Otro"
                            )
                            items(kinds) { (code, label) ->
                                StyledCategoryChip(
                                    label = label,
                                    isSelected = recipeKind == code,
                                    onClick = { recipeKind = code }
                                )
                            }
                        }
                    }

                    FormSubCard(
                        title = "Ingredientes de la Receta",
                        titleIcon = Icons.Default.FormatListBulleted
                    ) {
                        ingredientsList.forEachIndexed { index, ing ->
                            var showSuggestions by remember { mutableStateOf(false) }
                            val suggestions = remember(ing.name, suggestionIndex) {
                                if (ing.name.length >= 2) {
                                    IngredientSuggestionEngine.getSuggestions(ing.name, suggestionIndex)
                                } else emptyList()
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceElevated, RoundedCornerShape(12.dp))
                                    .border(1.dp, BordeSuave, RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    StyledOutlinedTextField(
                                        value = ing.name,
                                        onValueChange = { newName ->
                                            ingredientsList[index] = ing.copy(name = newName)
                                            showSuggestions = true
                                        },
                                        label = "Ingrediente #${index + 1}",
                                        placeholder = "ej. Café extraído, Leche, Hielo",
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (ingredientsList.size > 1) {
                                        IconButton(
                                            onClick = { ingredientsList.removeAt(index) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Advertencia)
                                        }
                                    }
                                }

                                if (showSuggestions && suggestions.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                        shape = RoundedCornerShape(8.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Column {
                                            suggestions.forEach { suggestion ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            ingredientsList[index] = ing.copy(
                                                                name = suggestion.name,
                                                                unit = suggestion.defaultUnit
                                                            )
                                                            showSuggestions = false
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = if (suggestion.isFromStorageBean) Icons.Default.Inventory2 else Icons.Default.History,
                                                            contentDescription = null,
                                                            tint = if (suggestion.isFromStorageBean) AcentoPrincipal else TextSecundario,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(suggestion.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrincipal)
                                                    }

                                                    if (suggestion.isFromStorageBean) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(AcentoPrincipal.copy(alpha = 0.15f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("De tu almacén", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AcentoPrincipal)
                                                        }
                                                    } else {
                                                        Text("Unid: ${suggestion.defaultUnit}", fontSize = 10.sp, color = TextSecundario)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    StyledOutlinedTextField(
                                        value = ing.amount,
                                        onValueChange = { newAmt ->
                                            ingredientsList[index] = ing.copy(amount = newAmt)
                                        },
                                        label = "Cantidad",
                                        placeholder = "ej. 30",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                        modifier = Modifier.weight(1f)
                                    )

                                    var expandedUnit by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { expandedUnit = true },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().height(52.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(ing.unit, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = expandedUnit,
                                            onDismissRequest = { expandedUnit = false }
                                        ) {
                                            listOf("G", "ML", "UNIT", "TSP", "TBSP", "OZ", "OTHER").forEach { u ->
                                                DropdownMenuItem(
                                                    text = { Text(u) },
                                                    onClick = {
                                                        ingredientsList[index] = ing.copy(unit = u)
                                                        expandedUnit = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        StyledSecondaryButton(
                            text = "+ Agregar Ingrediente",
                            onClick = { addNewIngredientRow() }
                        )
                    }

                    FormSubCard(
                        title = "Pasos de Preparación",
                        titleIcon = Icons.Default.Numbers
                    ) {
                        stepsList.forEachIndexed { index, step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceElevated, RoundedCornerShape(12.dp))
                                    .border(1.dp, BordeSuave, RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StyledOutlinedTextField(
                                    value = step.instruction,
                                    onValueChange = { newInst ->
                                        stepsList[index] = step.copy(instruction = newInst)
                                    },
                                    label = "Paso ${index + 1}",
                                    placeholder = "ej. Vertir tónica en copa fría con hielo",
                                    modifier = Modifier.weight(1f)
                                )
                                if (stepsList.size > 1) {
                                    IconButton(
                                        onClick = { stepsList.removeAt(index) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Eliminar paso", tint = Advertencia)
                                    }
                                }
                            }
                        }

                        StyledSecondaryButton(
                            text = "+ Agregar Paso",
                            onClick = { addNewStepRow() }
                        )
                    }

                    FormSubCard(
                        title = "Detalles y Notas Organolépticas",
                        titleIcon = Icons.Default.Psychology
                    ) {
                        StyledOutlinedTextField(
                            value = suggestedMethod,
                            onValueChange = { suggestedMethod = it },
                            label = "Método Recomendado (opcional)",
                            placeholder = "ej. V60, Espresso, Prensa Francesa"
                        )

                        StyledOutlinedTextField(
                            value = intention,
                            onValueChange = { intention = it },
                            label = "Notas e Intención Organoléptica",
                            placeholder = "ej. Perfil floral y acidez brillante con final dulce",
                            maxLines = 2
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StyledOutlinedTextField(
                                value = tagsText,
                                onValueChange = { tagsText = it },
                                label = "Etiquetas (separadas por coma)",
                                placeholder = "ej. Verano, Dulce, Cítrico",
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { isFavorite = !isFavorite }
                                    .padding(8.dp)
                            ) {
                                Checkbox(
                                    checked = isFavorite,
                                    onCheckedChange = { isFavorite = it }
                                )
                                Text("Favorito", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        StyledPrimaryButton(
                            text = if (isEditingExisting) "Guardar" else "Guardar Receta",
                            icon = Icons.Default.Check,
                            onClick = {
                                if (name.isNotBlank()) {
                                    val ingSummary = ingredientsList
                                        .filter { it.name.isNotBlank() }
                                        .joinToString(", ") { ing ->
                                            val amtStr = ing.amount.trim()
                                            val unitStr = ing.unit.trim()
                                            val qtyPart = when {
                                                amtStr.isNotBlank() && unitStr.isNotBlank() -> "$amtStr $unitStr"
                                                amtStr.isNotBlank() -> amtStr
                                                unitStr.isNotBlank() -> unitStr
                                                else -> ""
                                            }
                                            if (qtyPart.isNotBlank()) "${ing.name.trim()} ($qtyPart)" else ing.name.trim()
                                        }
                                    val stepSummary = stepsList
                                        .filter { it.instruction.isNotBlank() }
                                        .mapIndexed { idx, st -> "${idx + 1}. ${st.instruction}" }
                                        .joinToString("\n")

                                    viewModel.addRecipe(
                                        name = name,
                                        recipeKind = recipeKind,
                                        ingredientsSummary = ingSummary,
                                        stepsSummary = stepSummary,
                                        intention = intention,
                                        suggestedMethod = suggestedMethod,
                                        tags = tagsText,
                                        isFavorite = isFavorite,
                                        ingredientsList = ingredientsList.filter { it.name.isNotBlank() },
                                        recipeId = if (isEditingExisting) initialDraft?.id else null
                                    )
                                    onCompleted()
                                }
                            }
                        )

                        StyledSecondaryButton(
                            text = "Cancelar",
                            onClick = onCompleted
                        )
                    }
                }
                "Tazas" -> {
                    var beanName by remember { mutableStateOf("") }
                    var method by remember { mutableStateOf("V60") }
                    var foundNotes by remember { mutableStateOf("") }
                    var comment by remember { mutableStateOf("") }

                    FormHeaderWithBlob(
                        title = "Archivar Taza Evaluada",
                        subtitle = "Evaluación organoléptica de la taza extraída",
                        icon = Icons.Default.EmojiFoodBeverage,
                        onClose = onCompleted
                    )

                    FormSubCard(
                        title = "Datos de Catación",
                        titleIcon = Icons.Default.Star
                    ) {
                        StyledOutlinedTextField(
                            value = beanName,
                            onValueChange = { beanName = it },
                            label = "Nombre del Café Catado *",
                            placeholder = "ej. Geisha Esmeralda Lote 2"
                        )
                        StyledOutlinedTextField(
                            value = method,
                            onValueChange = { method = it },
                            label = "Método Utilizado",
                            placeholder = "ej. V60, Chemex"
                        )
                        StyledOutlinedTextField(
                            value = foundNotes,
                            onValueChange = { foundNotes = it },
                            label = "Descriptores / Notas Sensoriales",
                            placeholder = "ej. Jazmín, durazno, miel, acidez málica"
                        )
                        StyledOutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            label = "Comentario Final",
                            placeholder = "ej. Excelente dulzor, cuerpo medio aterciopelado",
                            maxLines = 2
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        StyledPrimaryButton(
                            text = "Archivar Taza",
                            icon = Icons.Default.Check,
                            onClick = {
                                if (beanName.isNotBlank()) {
                                    viewModel.saveCup(
                                        notesFound = foundNotes,
                                        notesExpected = "",
                                        score = 4.5f,
                                        comment = "Café: $beanName. Método: $method. $comment"
                                    )
                                    onCompleted()
                                }
                            }
                        )
                        StyledSecondaryButton(
                            text = "Cancelar",
                            onClick = onCompleted
                        )
                    }
                }
                "Ciencia" -> {
                    var method by remember { mutableStateOf("V60") }
                    var coffeeStr by remember { mutableStateOf("15") }
                    var waterStr by remember { mutableStateOf("240") }
                    var tempStr by remember { mutableStateOf("93") }
                    var grindSize by remember { mutableStateOf("Media") }
                    var notes by remember { mutableStateOf("") }

                    FormHeaderWithBlob(
                        title = "Archivar Experimento de Laboratorio",
                        subtitle = "Parámetros técnicos e hipótesis de extracción",
                        icon = Icons.Default.Science,
                        onClose = onCompleted
                    )

                    FormSubCard(
                        title = "Parámetros de Extracción",
                        titleIcon = Icons.Default.Analytics
                    ) {
                        StyledOutlinedTextField(
                            value = method,
                            onValueChange = { method = it },
                            label = "Método o Hipótesis",
                            placeholder = "ej. Extracción a 93°C vs 88°C"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StyledOutlinedTextField(
                                value = coffeeStr,
                                onValueChange = { coffeeStr = it },
                                label = "Café (g)",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            StyledOutlinedTextField(
                                value = waterStr,
                                onValueChange = { waterStr = it },
                                label = "Agua (ml)",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            StyledOutlinedTextField(
                                value = tempStr,
                                onValueChange = { tempStr = it },
                                label = "Temp (°C)",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        StyledOutlinedTextField(
                            value = grindSize,
                            onValueChange = { grindSize = it },
                            label = "Tamaño de Molienda",
                            placeholder = "ej. Media fina (24 clicks)"
                        )
                        StyledOutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "Notas de Resultados o Hipótesis",
                            placeholder = "ej. Mayor extracción de azúcares con vertidos lentos",
                            maxLines = 2
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        StyledPrimaryButton(
                            text = "Archivar Experimento",
                            icon = Icons.Default.Check,
                            onClick = {
                                val c = coffeeStr.toFloatOrNull() ?: 15f
                                val w = waterStr.toIntOrNull() ?: 240
                                val t = tempStr.toIntOrNull() ?: 93
                                val r = if (c > 0) w / c else 16f
                                viewModel.addExperiment(method, c, w, r, t, grindSize, notes)
                                onCompleted()
                            }
                        )
                        StyledSecondaryButton(
                            text = "Cancelar",
                            onClick = onCompleted
                        )
                    }
                }
                else -> { // "Equipos"
                    var name by remember { mutableStateOf("") }
                    var type by remember { mutableStateOf("método") }
                    var notes by remember { mutableStateOf("") }

                    FormHeaderWithBlob(
                        title = "Registrar Equipo de Extracción",
                        subtitle = "Herramientas e instrumentos del taller de café",
                        icon = Icons.Default.Build,
                        onClose = onCompleted
                    )

                    FormSubCard(
                        title = "Información del Equipo",
                        titleIcon = Icons.Default.HomeRepairService
                    ) {
                        StyledOutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Nombre / Descripción *",
                            placeholder = "ej. Cafetera V60 Switch 02 Glass"
                        )

                        Text("Categoría del Equipo:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecundario)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("método", "tetera", "báscula", "accesorios").forEach { t ->
                                StyledCategoryChip(
                                    label = t.uppercase(),
                                    isSelected = type == t,
                                    onClick = { type = t },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        StyledOutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "Notas o Especificaciones Técnicas",
                            placeholder = "ej. Capacidad 600ml, flujo controlado"
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        StyledPrimaryButton(
                            text = "Archivar Equipo",
                            icon = Icons.Default.Check,
                            onClick = {
                                if (name.isNotBlank()) {
                                    viewModel.addEquipment(name, type, notes)
                                    onCompleted()
                                }
                            }
                        )
                        StyledSecondaryButton(
                            text = "Cancelar",
                            onClick = onCompleted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateLayout(
    text: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BordeSuave.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(imageVector = Icons.Default.AllInbox, contentDescription = "", tint = TextSecundario.copy(alpha = 0.6f), modifier = Modifier.size(36.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = TextSecundario,
                textAlign = TextAlign.Center
            )
            if (actionText != null && onActionClick != null) {
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CafeCalidoOscuro),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = actionText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
