package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

/** Same normalized spheres, tint and light wash as iOS BrewOrganicCanvas.
 * Static vector paint keeps the screen responsive and leaves cards opaque/readable. */
@Composable
fun BrewOrganicBackground(warmTop: Boolean = false, modifier: Modifier = Modifier) {
    val dark = isDarkThemeGlobal
    Canvas(modifier.fillMaxSize()) {
        drawRect(MainBackground)
        val forest = AcentoPrincipal
        val clay = Color(if (dark) 0xFFC86D51 else 0xFFC26638)
        fun sphere(center: Offset, radius: Float, color: Color) {
            drawCircle(color.copy(alpha = if (dark) 0.11f else 0.065f), radius, center)
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = if (dark) 0.10f else 0.06f), color.copy(alpha = 0f)),
                center = center - Offset(radius * 0.28f, radius * 0.3f), radius = radius * 1.2f), radius, center)
        }
        sphere(Offset(size.width * 0.88f, size.height * 0.10f), size.width * 0.55f, if (warmTop) clay else forest)
        sphere(Offset(size.width * 0.12f, size.height * 0.82f), size.width * 0.48f, if (warmTop) forest else clay)
        drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = if (dark) 0.015f else 0.24f), Color.Transparent),
            center = Offset(size.width * 0.42f, 0f), radius = size.width * 1.15f))
    }
}
