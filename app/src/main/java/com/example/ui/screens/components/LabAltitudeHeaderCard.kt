package com.example.ui.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.LabCalibratedSlider
import com.example.ui.theme.AcentoPrincipal
import com.example.ui.theme.AcentoSuave
import com.example.ui.theme.Advertencia
import com.example.ui.theme.BordeSuave
import com.example.ui.theme.CafeCalidoClaro
import com.example.ui.theme.CafeCalidoOscuro
import com.example.ui.theme.MainBackgroundAlt
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrincipal
import com.example.ui.theme.TextSecundario
import com.example.ui.viewmodel.BaristaCalcState
import com.example.ui.viewmodel.BaristaCalcViewModel

@Composable
fun LabAltitudeHeaderCard(
    state: BaristaCalcState,
    viewModel: BaristaCalcViewModel,
    isFahrenheit: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenCustomCityDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val altitudeMeters = state.labAltitudeMeters
    val tBoilC = (100.0f - (altitudeMeters.coerceIn(0, 5000) * 0.0034f)).coerceIn(80.0f, 100.0f)
    val tBoilF = Math.round(tBoilC * 9f / 5f + 32f)
    val displayBoil = if (isFahrenheit) "$tBoilF °F" else "${String.format(java.util.Locale.US, "%.1f", tBoilC)} °C"
    val isTempCapped = state.labTemp > tBoilC

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(18.dp), spotColor = CafeCalidoOscuro.copy(alpha = 0.08f))
            .border(1.dp, if (isTempCapped) Advertencia.copy(alpha = 0.5f) else BordeSuave, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Bar (Collapsed State)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AcentoSuave),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terrain,
                            contentDescription = null,
                            tint = AcentoPrincipal,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = state.labCityName.ifBlank { "Nivel del mar (0m)" },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrincipal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Hervor: $displayBoil • $altitudeMeters msnm",
                            fontSize = 10.5.sp,
                            color = if (isTempCapped) Advertencia else TextSecundario,
                            fontWeight = if (isTempCapped) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Ocultar" else "Calibrar Altitud",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AcentoPrincipal
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AcentoPrincipal,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Warning Banner if temperature set exceeds boiling point
            if (isTempCapped) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Advertencia.copy(alpha = 0.12f))
                        .border(1.dp, Advertencia.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Advertencia,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "El agua hierve a $displayBoil en tu altitud. La temperatura real está acotada al hervor.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Advertencia,
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            // Expanded Controls
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "CIUDADES CAFETERAS & REFERENCIAS MUNDIALES",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecundario,
                        letterSpacing = 0.5.sp
                    )

                    // Worldwide coffee capitals & benchmark cities
                    val worldCoffeeCities = listOf(
                        Triple("Costa / Mar", 0, "Costa (0m)"),
                        Triple("Seattle / Tokio", 50, "Seattle/Tokio (50m)"),
                        Triple("Roma / Paris", 100, "Roma/París (100m)"),
                        Triple("São Paulo", 760, "São Paulo (760m)"),
                        Triple("Medellín", 1495, "Medellín (1,495m)"),
                        Triple("Guatemala", 1500, "Guatemala (1,500m)"),
                        Triple("San José CR", 1170, "San José (1,170m)"),
                        Triple("CDMX / Oaxaca", 2240, "CDMX (2,240m)"),
                        Triple("Addis Abeba", 2355, "Addis Abeba (2,355m)"),
                        Triple("Bogotá", 2600, "Bogotá (2,600m)"),
                        Triple("Cusco", 3399, "Cusco (3,399m)"),
                        Triple("La Paz", 3640, "La Paz (3,640m)")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(worldCoffeeCities.size) { idx ->
                            val (cityLabel, altM, fullName) = worldCoffeeCities[idx]
                            val isSelected = altitudeMeters == altM && (state.labCityName == fullName || state.labCityName.startsWith(cityLabel))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AcentoPrincipal else MainBackgroundAlt)
                                    .border(1.dp, if (isSelected) AcentoPrincipal else BordeSuave, RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.updateLabVariables(
                                            altitudeMeters = altM,
                                            cityName = fullName
                                        )
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cityLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextPrincipal
                                )
                            }
                        }
                    }

                    // Button to add custom city
                    OutlinedButton(
                        onClick = { onOpenCustomCityDialog() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AcentoPrincipal.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AcentoPrincipal)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLocationAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Agregar mi ciudad y su altura", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Manual altitude calibration slider
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ajuste manual de elevación", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrincipal)
                            Text("$altitudeMeters msnm", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CafeCalidoClaro)
                        }

                        LabCalibratedSlider(
                            value = altitudeMeters.toFloat(),
                            onValueChange = {
                                val meters = it.toInt()
                                viewModel.updateLabVariables(
                                    altitudeMeters = meters,
                                    cityName = "Manual (${meters}m)"
                                )
                            },
                            range = 0f..4000f,
                            recommendedRange = 0f..2600f,
                            step = 25f,
                            activeColor = CafeCalidoClaro
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("0 m", fontSize = 9.5.sp, color = TextSecundario)
                            Text("Punto de Hervor: $displayBoil", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = AcentoPrincipal)
                            Text("4000 m", fontSize = 9.5.sp, color = TextSecundario)
                        }
                    }

                    // Collapse button
                    Button(
                        onClick = { onToggleExpand() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CafeCalidoOscuro),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Listo, guardar y ocultar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
