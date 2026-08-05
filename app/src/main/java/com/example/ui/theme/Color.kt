package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

// Global theme state flag
var isDarkThemeGlobal by mutableStateOf(false)

// Premium, warm 4-color dynamic variables matching base image
var MainBackground by mutableStateOf(Color(0xFFF4F1EA))
var MainBackgroundAlt by mutableStateOf(Color(0xFFEBE6DC))
var MainBackgroundLight by mutableStateOf(Color(0xFFDFD8CC))

var SurfaceCard by mutableStateOf(Color(0xFFFFFFFF))

var TextPrincipal by mutableStateOf(Color(0xFF1A1C1A))
var TextSecundario by mutableStateOf(Color(0xFF5A655D))

var BordeSuave by mutableStateOf(Color(0xFFE2DDD2))
var BordeMedio by mutableStateOf(Color(0xFFD0C8B8))

var AcentoSuave by mutableStateOf(Color(0x1A234E3C))

// Muted Forest Green primary & Warm Terracotta accents from base design system
val AcentoPrincipal = Color(0xFF234E3C) // Muted Forest Green
val AcentoSecundario = Color(0xFFC86D51) // Warm Terracotta Accent

// Warm, complementary accents for specialized states
val CafeCalidoOscuro = Color(0xFFB85D42) // Rich terracotta
val CafeCalidoClaro = Color(0xFFC86D51) // Terracotta gold

val Advertencia = Color(0xFFD9534F) // Clean red warning

// Specialized ratio tones
val EspressoPrimary = Color(0xFFB85D42)
val EspressoSecondary = Color(0xFF964B34)

val IntensoPrimary = Color(0xFFD97706)
val IntensoSecondary = Color(0xFFB45309)

val BalancePrimary = Color(0xFF234E3C)
val BalanceSecondary = Color(0xFF121413)

val ClaridadPrimary = Color(0xFF2E5A44)
val ClaridadSecondary = Color(0xFF181C1A)

// Helper to switch theme colors
fun updateThemeColors(isDark: Boolean) {
    isDarkThemeGlobal = isDark
    if (isDark) {
        MainBackground = Color(0xFF121413) // Warm dark slate background
        MainBackgroundAlt = Color(0xFF181C1A)
        MainBackgroundLight = Color(0xFF222624)
        SurfaceCard = Color(0xFF1C211F) // Warm dark cards
        TextPrincipal = Color(0xFFF7F9F6)
        TextSecundario = Color(0xFF9CA3AF)
        BordeSuave = Color(0xFF2A302D)
        BordeMedio = Color(0xFF3B4440)
        AcentoSuave = Color(0x332E5A44)
    } else {
        MainBackground = Color(0xFFF4F1EA) // Warm bone/cream background
        MainBackgroundAlt = Color(0xFFEBE6DC)
        MainBackgroundLight = Color(0xFFDFD8CC)
        SurfaceCard = Color(0xFFFFFFFF) // Pure pristine white card surfaces
        TextPrincipal = Color(0xFF1A1C1A) // Deep charcoal text color
        TextSecundario = Color(0xFF5A655D) // Muted olive-gray
        BordeSuave = Color(0xFFE2DDD2) // Soft boundaries
        BordeMedio = Color(0xFFD0C8B8) // Medium contrast lines
        AcentoSuave = Color(0x1A234E3C) // Very soft forest green tint
    }
}



