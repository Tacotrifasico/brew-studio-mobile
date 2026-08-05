package com.example.ui.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.AppDatabase
import com.example.data.database.Recipe
import com.example.data.database.Technique
import com.example.data.database.TechniqueStep
import com.example.data.remote.models.RemoteShare
import com.example.ui.theme.*
import com.example.ui.viewmodel.SocialViewModel
import com.example.ui.viewmodel.SocialUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun FeedAndInboxTab(viewModel: SocialViewModel, state: SocialUiState) {
    var subTab by remember { mutableStateOf(0) } // 0 = Public Feed, 1 = Inbox (Buzón Directo)
    val scope = rememberCoroutineScope()
    var alertMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = subTab == 0,
                onClick = { subTab = 0; viewModel.fetchFeed() },
                label = { Text("Muro Público") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AcentoSuave,
                    selectedLabelColor = AcentoPrincipal
                )
            )
            FilterChip(
                selected = subTab == 1,
                onClick = { subTab = 1; viewModel.fetchInbox() },
                label = { Text("Bandeja Recibidos (Buzón)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AcentoSuave,
                    selectedLabelColor = AcentoPrincipal
                )
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (subTab == 0) {
                if (state.isFeedLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AcentoPrincipal)
                    }
                } else if (state.feed.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("El muro está tranquilo hoy.\n¡Inventa y comparte algo nuevo!", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = TextSecundario, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.feed) { share ->
                            ShareCard(
                                share = share,
                                currentUserId = state.userId,
                                onLike = { viewModel.likeShare(share.id) },
                                onUnlike = { viewModel.unlikeShare(share.id) },
                                onImport = {
                                    viewModel.importShare(share) { success, msg ->
                                        alertMessage = msg
                                    }
                                },
                                onFork = {
                                    viewModel.forkShare(share) { success, msg ->
                                        alertMessage = msg
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                if (state.isInboxLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AcentoPrincipal)
                    }
                } else if (state.inbox.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tu bandeja de correo está vacía.\nNo tienes transferencias directas.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = TextSecundario, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.inbox) { inboxItem ->
                            inboxItem.share?.let { share ->
                                ShareCard(
                                    share = share,
                                    currentUserId = state.userId,
                                    onLike = {},
                                    onUnlike = {},
                                    onImport = {
                                        viewModel.importShare(share) { success, msg ->
                                            alertMessage = msg
                                        }
                                    },
                                    onFork = {
                                        viewModel.forkShare(share) { success, msg ->
                                            alertMessage = msg
                                        }
                                    },
                                    isInboxView = true
                                )
                            }
                        }
                    }
                }
            }

            // Simple Dialog for Import/Fork feedback alerts
            alertMessage?.let { msg ->
                AlertDialog(
                    onDismissRequest = { alertMessage = null },
                    confirmButton = {
                        TextButton(onClick = { alertMessage = null }) {
                            Text("Aceptar", color = AcentoPrincipal, fontWeight = FontWeight.Bold)
                        }
                    },
                    title = { Text("Transferidor de Fórmulas", fontWeight = FontWeight.Bold, color = TextPrincipal) },
                    text = { Text(msg, color = TextSecundario) },
                    containerColor = SurfaceCard,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
fun ShareCard(
    share: RemoteShare,
    currentUserId: String?,
    onLike: () -> Unit,
    onUnlike: () -> Unit,
    onImport: () -> Unit,
    onFork: () -> Unit,
    isInboxView: Boolean = false
) {
    var isLiked by remember { mutableStateOf(false) } // simplistic localtoggle for immediate UI feedback
    val snap = share.payloadSnapshotJson

    val ratio = snap["ratio"]?.toString() ?: "16"
    val methodValue = snap["method"]?.toString() ?: "Filtrado"
    val coffee = snap["coffeeGrams"]?.toString() ?: "15"
    val water = snap["waterMl"]?.toString() ?: "240"
    val tempValue = snap["temperature"]?.toString() ?: "90"
    val clicksValue = snap["clicks"]?.toString() ?: snap["grindClicks"]?.toString() ?: "Mundial"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Type + Badge + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (share.entityType == "recipe") CafeCalidoOscuro else AcentoPrincipal,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (share.entityType == "recipe") "RECETA" else "TÉCNICA",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "@${share.fromHandle ?: "barista"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecundario,
                    modifier = Modifier.weight(1f)
                )

                // Render attribution badges cleanly
                if (share.originalAuthorName != null && share.originalAuthorName != share.fromName) {
                    Text(
                        text = "Fork",
                        color = CafeCalidoClaro,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0x18C28B46), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body: Title & message
            Text(share.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
            
            if (!share.message.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\"${share.message}\"",
                    fontSize = 13.sp,
                    color = TextPrincipal,
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Spec row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MainBackgroundAlt, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SpecItem("Método", methodValue)
                SpecItem("Ratio", "1:$ratio")
                SpecItem("Café", "${coffee}g")
                SpecItem("Agua", "${water}ml")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🌡️ Temp: $tempValue°C", fontSize = 11.sp, color = TextSecundario, fontWeight = FontWeight.SemiBold)
                Text("⚙️ Molienda: $clicksValue clicks", fontSize = 11.sp, color = TextSecundario, fontWeight = FontWeight.SemiBold)
            }

            // Attribution display
            share.originalAuthorName?.let { original ->
                if (original != share.fromName) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = BordeSuave)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = "Credit", modifier = Modifier.size(14.dp), tint = CafeCalidoClaro)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Fórmula original editada de: $original",
                            fontSize = 11.sp,
                            color = CafeCalidoClaro,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = BordeSuave)

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isInboxView) {
                    IconButton(onClick = {
                        isLiked = !isLiked
                        if (isLiked) onLike() else onUnlike()
                    }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color.Red else TextSecundario
                        )
                    }

                    val rawCount = share.likesCount?.firstOrNull()?.count ?: 0
                    val displayCount = if (isLiked) rawCount + 1 else rawCount
                    Text(
                        displayCount.toString(),
                        fontSize = 12.sp,
                        color = TextSecundario,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Actions: exact COPY or Fork variant
                TextButton(
                    onClick = onImport,
                    colors = ButtonDefaults.textButtonColors(contentColor = AcentoPrincipal)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Import", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Registrar Copia", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                TextButton(
                    onClick = onFork,
                    colors = ButtonDefaults.textButtonColors(contentColor = CafeCalidoOscuro)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CallSplit, contentDescription = "Fork", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hacer Fork", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SpecItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 9.sp, color = TextSecundario)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
    }
}

@Composable
fun MyLocalFormulasTab(viewModel: SocialViewModel) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    
    val recipesFlow = remember { database.recipeDao().getAllRecipes() }
    val recipes by recipesFlow.collectAsState(initial = emptyList())
    
    val techFlow = remember { database.techniqueDao().getAllTechniques() }
    val techniques by techFlow.collectAsState(initial = emptyList())

    var activeViewMode by remember { mutableStateOf(0) } // 0 = Recipes, 1 = Techniques
    
    // Sharing modal properties
    var selectedRecipeToShare by remember { mutableStateOf<Recipe?>(null) }
    var selectedTechToShare by remember { mutableStateOf<Technique?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = activeViewMode,
            containerColor = MainBackgroundAlt,
            contentColor = AcentoPrincipal
        ) {
            Tab(selected = activeViewMode == 0, onClick = { activeViewMode = 0 }) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Recetas")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mis Recetas (${recipes.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = activeViewMode == 1, onClick = { activeViewMode = 1 }) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = "Técnicas")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mis Técnicas (${techniques.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (activeViewMode == 0) {
                if (recipes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tienes recetas locales.", color = TextSecundario)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recipes) { rec ->
                            LocalItemCard(
                                title = rec.name,
                                subtitle = "Categoría ${rec.recipeKind} · ${rec.intention.take(30)}",
                                isShared = rec.isShared || rec.remoteId != null,
                                onShare = { selectedRecipeToShare = rec }
                            )
                        }
                    }
                }
            } else {
                if (techniques.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tienes técnicas locales.", color = TextSecundario)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(techniques) { tech ->
                            LocalItemCard(
                                title = tech.name,
                                subtitle = "Dosis ${tech.doseG}g · ${tech.notes.take(40)}...",
                                isShared = tech.isShared || tech.remoteId != null,
                                onShare = { selectedTechToShare = tech }
                            )
                        }
                    }
                }
            }

            // Recipe Share Composer Composable Dialog
            selectedRecipeToShare?.let { entry ->
                ShareComposerSheet(
                    titleName = entry.name,
                    onDismiss = { selectedRecipeToShare = null },
                    onPost = { desc, target, visibility ->
                        viewModel.shareRecipeToFeed(entry, desc, target, visibility) { success, msg ->
                            selectedRecipeToShare = null
                        }
                    }
                )
            }

            // Technique Share Composer Dialog
            selectedTechToShare?.let { entry ->
                val scope = rememberCoroutineScope()
                ShareComposerSheet(
                    titleName = entry.name,
                    onDismiss = { selectedTechToShare = null },
                    onPost = { desc, target, visibility ->
                        scope.launch {
                            val steps = database.techniqueStepDao().getStepsForTechniqueSync(entry.id)
                            viewModel.shareTechniqueToFeed(entry, steps, desc, target, visibility) { success, msg ->
                                selectedTechToShare = null
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LocalItemCard(title: String, subtitle: String, isShared: Boolean, onShare: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
        border = if (isShared) BorderStroke(1.dp, AcentoSecundario) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrincipal)
                Text(subtitle, fontSize = 11.sp, color = TextSecundario)
                
                if (isShared) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudQueue, contentDescription = "Cloud", modifier = Modifier.size(12.dp), tint = AcentoSecundario)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Registrado en Servidor", fontSize = 9.sp, color = AcentoSecundario, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = if (isShared) CafeCalidoClaro else AcentoPrincipal),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Compartir", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Compartir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareComposerSheet(
    titleName: String,
    onDismiss: () -> Unit,
    onPost: (description: String, targetUserId: String?, visibility: String) -> Unit
) {
    var desc by remember { mutableStateOf("") }
    var visMode by remember { mutableStateOf("public") } // "public" or "direct"
    var recipientId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onPost(desc, if (visMode == "direct") recipientId else null, visMode) },
                colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal)
            ) {
                Text("Publicar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecundario)
            }
        },
        title = { Text("Compartir Fórmula Real", fontWeight = FontWeight.Bold, color = TextPrincipal) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Preparando envío de: $titleName", fontSize = 12.sp, color = TextSecundario)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { if (it.length <= 280) desc = it },
                    label = { Text("Tu opinión (máx. 280 carc.)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Destino / Visibilidad del envío:", fontSize = 12.sp, color = TextPrincipal, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = visMode == "public",
                        onClick = { visMode = "public" },
                        label = { Text("Muro Público") }
                    )
                    FilterChip(
                        selected = visMode == "direct",
                        onClick = { visMode = "direct" },
                        label = { Text("Buzón Directo (Inbox)") }
                    )
                }

                if (visMode == "direct") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recipientId,
                        onValueChange = { recipientId = it },
                        label = { Text("UUID del receptor Supabase") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ActivityTimelineTab(state: SocialUiState) {
    if (state.activity.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aún no tienes registro de actividades.", color = TextSecundario)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.activity) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isShare = log.action.contains("share")
                        Icon(
                            imageVector = if (isShare) Icons.Default.CloudUpload else Icons.Default.AssignmentReturned,
                            contentDescription = "Event",
                            tint = if (isShare) AcentoPrincipal else CafeCalidoClaro
                        )
                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = log.note ?: "Operación registrada",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrincipal
                            )
                            Text(
                                text = log.action.uppercase(),
                                fontSize = 10.sp,
                                color = TextSecundario,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
