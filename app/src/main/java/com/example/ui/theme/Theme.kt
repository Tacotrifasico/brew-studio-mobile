package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  val colorScheme = if (isDarkThemeGlobal) {
    darkColorScheme(
      primary = AcentoPrincipal,
      onPrimary = Color.White,
      primaryContainer = AcentoSuave,
      onPrimaryContainer = AcentoSecundario,
      secondary = AcentoSecundario,
      onSecondary = Color.Black,
      background = MainBackground,
      onBackground = TextPrincipal,
      surface = SurfaceCard,
      onSurface = TextPrincipal,
      surfaceVariant = MainBackgroundAlt,
      onSurfaceVariant = TextSecundario,
      outline = BordeMedio,
      error = Advertencia
    )
  } else {
    lightColorScheme(
      primary = AcentoPrincipal,
      onPrimary = Color.White,
      primaryContainer = AcentoSuave,
      onPrimaryContainer = AcentoSecundario,
      secondary = AcentoSecundario,
      onSecondary = Color.Black,
      background = MainBackground,
      onBackground = TextPrincipal,
      surface = SurfaceCard,
      onSurface = TextPrincipal,
      surfaceVariant = MainBackgroundAlt,
      onSurfaceVariant = TextSecundario,
      outline = BordeMedio,
      error = Advertencia
    )
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
