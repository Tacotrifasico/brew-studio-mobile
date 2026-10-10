package com.example.data.engine

import kotlin.math.roundToInt

/** Recipe references, not universal sensory thresholds. Mirror: iOS LabModel.swift.
 * Points are not validated intervals. Grinder numbers belong to the user's grinder.
 * The visual sensory hypothesis is not measured TDS or extraction yield.
 */
data class LabTemperatureGuide(val temperatureC: Double, val altitudeMeters: Int, val fahrenheit: Boolean = false, val method: String = "V60") {
    val kind = when (method.lowercase().trim()) {
        "v60" -> "filter"; "chemex" -> "chemex"
        "prensa francesa", "french press" -> "press"; "aeropress" -> "aero"
        "espresso" -> "espresso"; "moka", "moka italiana" -> "moka"
        "cold brew", "coldbrew" -> "cold"; else -> "custom"
    }
    // Approximation, not measured pressure. Never cap a pressurized brewer.
    val boilingC = 100.0 - altitudeMeters.coerceIn(0, 5000) * 0.0034
    val openHotWater = kind in listOf("filter", "chemex", "press", "aero")
    val lowerC = when (kind) { "filter", "press" -> 92.0; "chemex" -> (200.0 - 32) / 1.8; "aero" -> 80.0; "espresso" -> 90.5; else -> 0.0 }
    val upperC = when (kind) { "filter", "press" -> 96.0; "chemex" -> (200.0 - 32) / 1.8; "aero" -> 85.0; "espresso" -> 96.1; else -> 100.0 }
    val hasReference = kind !in listOf("moka", "cold", "custom")
    val hasReachableBand = hasReference && (!openHotWater || boilingC >= lowerC)
    fun degrees(celsius: Double): Int = (if (fahrenheit) celsius * 1.8 + 32 else celsius).roundToInt()
    val sliderRange: ClosedFloatingPointRange<Float> get() {
        val low = when (kind) { "cold", "custom" -> 0.0; "moka" -> 20.0; else -> 70.0 }
        return degrees(low).toFloat()..degrees(if (kind == "cold") 35.0 else 100.0).toFloat()
    }
    val recommendedRange: ClosedFloatingPointRange<Float> get() = if (hasReachableBand) degrees(lowerC).toFloat()..degrees(if (openHotWater) minOf(upperC, boilingC) else upperC).toFloat() else sliderRange
    // On-screen working limits are the reachable limits, not the unclipped source range.
    val workingRangeText: String get() {
        if (!hasReference) return "Según técnica"
        if (!hasReachableBand) return "Sin zona a esta altura"
        val symbol = if (fahrenheit) "°F" else "°C"
        val low = recommendedRange.start.toInt(); val high = recommendedRange.endInclusive.toInt()
        if (kind == "aero") return "Puntos · $low / $high $symbol"
        if (low == high) return "Punto · $low $symbol"
        return "Zona de trabajo · $low–$high $symbol"
    }
    val rangeText get() = if (!hasReference) "Sin intervalo universal" else if (lowerC == upperC) "≈ ${degrees(lowerC)} ${if (fahrenheit) "°F" else "°C"}" else "${degrees(lowerC)}–${degrees(upperC)} ${if (fahrenheit) "°F" else "°C"}"
    val sourceName get() = when (kind) { "filter" -> "Hario · receta V60"; "press" -> "Bodum · prensa"; "chemex" -> "Chemex · punto de partida"; "aero" -> "AeroPress · por tueste"; "espresso" -> "SCAA · referencia histórica"; "moka" -> "Bialetti · manejo del calor"; "cold" -> "Toddy · protocolo ambiente"; else -> "Método personalizado" }
    val sourceURL get() = when (kind) {
        "filter" -> "https://www.hario.co.uk/pages/brew-guides-v60-expert"
        "press" -> "https://www.bodum.com/es/es/1918-913-caffettiera"
        "chemex" -> "https://assets.unilogcorp.com/187/ITEM/DOC/CHEMEX_102422786_Instruction_Installation_Manual.pdf"
        "aero" -> "https://aeropress.com/pages/whats-the-optimal-brewing-temperature-for-aeropress-coffee-makers"
        "espresso" -> "https://sca.coffee/sca-news/25-magazine/issue-3/defining-ever-changing-espresso-25-magazine-issue-3-zyx36"
        "moka" -> "https://bialetti-cookware.zendesk.com/hc/en-us/articles/5416235346322-How-to-use-the-Moka-Express"
        "cold" -> "https://toddycafe.com/cold-brew/instruction-manual"; else -> ""
    }
    val warning get() = (openHotWater && temperatureC > boilingC) || (hasReference && degrees(temperatureC) !in degrees(lowerC)..degrees(upperC))
    val headline get() = when {
        openHotWater && temperatureC > boilingC -> "Supera el hervor local"
        !hasReachableBand && hasReference -> "Referencia por encima del hervor"
        kind == "moka" -> "Controla la llama, no una zona V60"
        kind == "cold" -> "Extracción en frío: manda el tiempo"
        kind == "custom" -> "Falta una referencia para este método"
        kind == "chemex" -> if (warning) "Difiere del punto Chemex" else "Cerca del punto Chemex"
        kind == "aero" -> if (warning) "Otra receta AeroPress" else "Referencias AeroPress por tueste"
        degrees(temperatureC) < degrees(lowerC) -> "Calor bajo para $method"
        degrees(temperatureC) > degrees(upperC) -> "Calor alto para $method"
        else -> "Calor en zona de trabajo"
    }
    val compactDetail get() = when {
        openHotWater && temperatureC > boilingC -> "Usa el hervor estimado; compensa con molienda o tiempo."
        !hasReachableBand && hasReference -> "No inventamos otro rango. Ajusta molienda/tiempo y cata."
        kind == "aero" -> "Oscuro: ${degrees(80.0)}; medio/claro: ${degrees(85.0)} ${if (fahrenheit) "°F" else "°C"}. Otras técnicas usan más calor."
        kind == "moka" -> "Llama baja/media. Retira al terminar; no es temperatura de vertido."
        kind == "cold" -> "Agua ambiente · 8–24 h. Para refrigeración, elige otra técnica."
        kind == "custom" -> "Elige una técnica documentada; no heredamos V60."
        kind == "chemex" -> "Punto aproximado, no intervalo óptimo. Confirma en Cata."
        degrees(temperatureC) < degrees(lowerC) -> "Menos calor puede ralentizar la extracción: prueba menos gruesa o más tiempo."
        degrees(temperatureC) > degrees(upperC) -> "Más calor puede acelerar extracción; no garantiza amargor."
        else -> "Punto de partida. Confirma el sabor en Cata."
    }
    val detail get() = when {
        openHotWater && temperatureC > boilingC -> "A tu altura, el agua hierve antes. Usa el hervor estimado como límite; prueba molienda menos gruesa o más tiempo."
        !hasReachableBand && hasReference -> "La altura impide alcanzar esta referencia con agua abierta. No inventamos otro rango: ajusta molienda y tiempo, y compara en Cata."
        kind == "aero" -> "Oscuro: ${degrees(80.0)} ${if (fahrenheit) "°F" else "°C"}; medio/claro: ${degrees(85.0)} ${if (fahrenheit) "°F" else "°C"}. Son puntos de partida, no límites; otras técnicas usan más calor."
        kind == "moka" -> "Llama baja/media; retira al terminar. La temperatura inicial no describe la extracción interna bajo presión."
        kind == "cold" -> "Toddy indica agua ambiente y 8–24 h. En refrigeración usa una técnica específica; no aplican los minutos del filtrado caliente."
        kind == "custom" -> "Carga o elige una técnica documentada. No heredamos rangos ni consejos de V60."
        kind == "chemex" -> "El manual propone aproximadamente ${degrees(lowerC)} ${if (fahrenheit) "°F" else "°C"}; no define un intervalo óptimo universal. Confirma en Cata."
        degrees(temperatureC) < degrees(lowerC) -> "Menos calor puede ralentizar la extracción. Prueba subir hacia los puntos orientativos, molienda menos gruesa o más tiempo; confirma en Cata."
        degrees(temperatureC) > degrees(upperC) -> "Más calor puede acelerar la extracción; no demuestra amargor. Compara con los puntos orientativos y cambia una variable por vez."
        else -> "Punto de partida, no garantía de equilibrio. El sabor real se registra en Cata."
    }
}
