package com.example.ui.user

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.SocialViewModel
import com.example.ui.viewmodel.SocialUiState
import kotlinx.coroutines.launch

const val INSTAGRAM_URL = "https://instagram.com/brewstudio.app"
const val INSTAGRAM_HANDLE = "@brewstudio.app"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(viewModel: SocialViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Perfil", "Bandeja Feed", "Mis Fórmulas", "Mi Historial")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Brew Studio Hub", fontWeight = FontWeight.Bold, color = TextPrincipal) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = TextPrincipal)
                    }
                },
                actions = {
                    if (state.isLoggedIn) {
                        IconButton(onClick = { viewModel.triggerSync() }) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sincronizar",
                                tint = if (state.isSyncing) CafeCalidoClaro else AcentoPrincipal
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MainBackground)
            )
        },
        containerColor = MainBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!state.isLoggedIn) {
                UnauthenticatedView(viewModel = viewModel)
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Profile Header card
                    UserProfileCard(state = state, onLogout = { viewModel.logout() })

                    // Horizontal tab navigation
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MainBackground,
                        contentColor = AcentoPrincipal,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = AcentoPrincipal
                            )
                        }
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }

                    // Selected Sub-Screen
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> PersonalProfileTab(viewModel = viewModel, state = state)
                            1 -> FeedAndInboxTab(viewModel = viewModel, state = state)
                            2 -> MyLocalFormulasTab(viewModel = viewModel)
                            3 -> ActivityTimelineTab(state = state)
                        }
                    }
                }
            }

            // Global non-blocking Toast alert messages or sync statuses
            state.syncMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TextPrincipal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = MainBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, color = MainBackground, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileCard(state: SocialUiState, onLogout: () -> Unit) {
    val hexColor = try {
        Color(android.graphics.Color.parseColor(state.avatarColor))
    } catch (e: Exception) {
        AcentoPrincipal
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded Avatar Color Box
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(hexColor),
                contentAlignment = Alignment.Center
            ) {
                val initial = if (state.displayName.isNotEmpty()) state.displayName.first().uppercaseChar().toString() else "B"
                Text(initial, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(state.displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrincipal)
                Text("@${state.handle}", fontSize = 13.sp, color = TextSecundario, fontWeight = FontWeight.Medium)
                Text(state.email, fontSize = 11.sp, color = TextSecundario)
            }

            IconButton(onClick = onLogout) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar Sesión", tint = Advertencia)
            }
        }
    }
}

@Composable
fun UnauthenticatedView(viewModel: SocialViewModel) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }

    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = "Social Hub logo",
                modifier = Modifier.size(72.dp),
                tint = AcentoPrincipal
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Taller del Brewther",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextPrincipal,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Backend y base de datos social sincrónico.",
                fontSize = 13.sp,
                color = TextSecundario,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isSignUpMode) "Regístrate en la red" else "Inicia Sesión Real",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrincipal,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AcentoPrincipal,
                            unfocusedBorderColor = BordeMedio
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña (mín. 6 carc.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AcentoPrincipal,
                            unfocusedBorderColor = BordeMedio
                        )
                    )

                    if (isSignUpMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Nombre Público") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AcentoPrincipal)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = handle,
                            onValueChange = { handle = it },
                            label = { Text("Handle único (e.g. barista_v60)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AcentoPrincipal)
                        )
                    }

                    state.authError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(err, color = Advertencia, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (isSignUpMode) {
                                viewModel.register(email, password, displayName, handle)
                            } else {
                                viewModel.login(email, password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !state.isAuthLoading
                    ) {
                        if (state.isAuthLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text(if (isSignUpMode) "Crear Perfil en Supabase" else "Ingresar con Supabase Auth")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.loginDemo() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AcentoPrincipal),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AcentoPrincipal)
                    ) {
                        Icon(Icons.Default.PersonOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ingresar en Modo Demo (Local / Offline)")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { isSignUpMode = !isSignUpMode }) {
                Text(
                    text = if (isSignUpMode) "¿Ya tienes un usuario? Ingresa aquí" else "¿No tienes una cuenta? Regístrate gratis en Postgres",
                    color = AcentoPrincipal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun PersonalProfileTab(viewModel: SocialViewModel, state: SocialUiState) {
    var editMode by remember { mutableStateOf(false) }
    var dispName by remember { mutableStateOf(state.displayName) }
    var hand by remember { mutableStateOf(state.handle) }
    var colorHex by remember { mutableStateOf(state.avatarColor) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Editar Datos del Perfil", fontWeight = FontWeight.Bold, color = TextPrincipal)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { editMode = !editMode }) {
                        Icon(
                            imageVector = if (editMode) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = AcentoPrincipal
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (editMode) {
                    OutlinedTextField(
                        value = dispName,
                        onValueChange = { dispName = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = hand,
                        onValueChange = { hand = it },
                        label = { Text("Handle (sin @)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = colorHex,
                        onValueChange = { colorHex = it },
                        label = { Text("Color Avatar Hex (e.g. #3F7A63)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.updateProfile(dispName, hand, colorHex)
                            editMode = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AcentoPrincipal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar Cambios")
                    }
                } else {
                    ProfileStatRow("Registrado en", state.email)
                    ProfileStatRow("Estatus de Cuenta", "Sincronizado con Supabase Postgres")
                    ProfileStatRow("Publicación", "Reglas Row Level Security Activas")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Stats card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Estadísticas en Dispositivo (Cache)", fontWeight = FontWeight.Bold, color = TextPrincipal)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ProfileStatMetric("Recetas", state.recipesCount.toString())
                    ProfileStatMetric("Técnicas", state.techniquesCount.toString())
                    ProfileStatMetric("Shared", state.activity.filter { it.action.contains("share") }.size.toString())
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        AboutTeamCard()
    }
}

@Composable
fun AboutTeamCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MainBackgroundAlt),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-30).dp)
                    .clip(CircleShape)
                    .background(AcentoSuave)
            )

            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "SOBRE EL EQUIPO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AcentoPrincipal,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hecho por Brewthers, para Brewthers",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrincipal
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Estamos construyendo esto contigo. Síguenos y cuéntanos qué quieres ver.",
                    fontSize = 13.sp,
                    color = TextSecundario,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(INSTAGRAM_URL))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                    color = SurfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BordeSuave)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AcentoPrincipal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Instagram",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Síguenos en Instagram",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrincipal
                            )
                            Text(
                                text = INSTAGRAM_HANDLE,
                                fontSize = 12.sp,
                                color = TextSecundario
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Abrir enlace",
                            tint = TextSecundario,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecundario, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = TextPrincipal, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProfileStatMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = AcentoPrincipal)
        Text(label, fontSize = 11.sp, color = TextSecundario)
    }
}
