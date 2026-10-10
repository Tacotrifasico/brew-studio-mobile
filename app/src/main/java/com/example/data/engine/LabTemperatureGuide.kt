package com.example.data.engine

import kotlin.math.roundToInt

/** Shared with LabTemperatureGuide in iOS. Guidance is a risk, not a sensory verdict.
 * Canonical Celsius is untouched; comparisons use selectable whole display degrees.
 * At altitude the useful window moves below the local boiling ceiling.
 */
data class LabTemperatureGuide(val temperatureC: Double, val altitudeMeters: Int, val fahrenheit: Boolean = false) {
    val boilingC = 100.0 - altitudeMeters.coerceIn(0, 5000) * 0.0034
    val upperC = minOf(96.0, boilingC)
    val lowerC = maxOf(80.0, minOf(90.0, upperC - 6.0))
    fun degrees(celsius: Double): Int = (if (fahrenheit) celsius * 1.8 + 32 else celsius).roundToInt()
    val recommendedRange: ClosedFloatingPointRange<Float> get() = degrees(lowerC).toFloat()..degrees(upperC).toFloat()
    val rangeText get() = "${degrees(lowerC)}–${degrees(upperC)} ${if (fahrenheit) "°F" else "°C"}"
    val warning: Boolean get() = temperatureC > boilingC || degrees(temperatureC) !in degrees(lowerC)..degrees(upperC)
    val headline: String get() = when {
        temperatureC > boilingC -> "Supera el hervor local"
        degrees(temperatureC) < degrees(lowerC - 3) -> "Agua demasiado fría"
        degrees(temperatureC) < degrees(lowerC) -> "Agua por debajo de la zona útil"
        degrees(temperatureC) > degrees(upperC) -> "Calor alto: vigila el amargor"
        else -> "En zona útil"
    }
    val detail: String get() = when (headline) {
        "Supera el hervor local" -> "A tu altura, el agua hierve antes de alcanzar esa temperatura. Ajusta molienda o tiempo."
        "Agua demasiado fría" -> "Riesgo de subextracción: taza agria o débil. Sube la temperatura hacia la zona útil; valida el resultado en Cata."
        "Agua por debajo de la zona útil" -> "Puede faltar extracción y dulzor. Sube hacia la zona útil o compensa con molienda y tiempo."
        "Calor alto: vigila el amargor" -> "Puede aumentar el amargor o la sequedad. Prueba bajar hacia la zona útil, especialmente con tueste oscuro."
        else -> "Buen punto de partida; molienda, tiempo y grano también definen el sabor."
    }
}
