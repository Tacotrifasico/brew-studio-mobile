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
    // Keep the documented reference visible even when altitude makes it unreachable.
    val recipeBand: ClosedFloatingPointRange<Float>? get() = if (hasReference && kind !in listOf("aero", "chemex")) degrees(lowerC).toFloat()..degrees(upperC).toFloat() else null
    val recipePoints: List<Float> get() = when (kind) {
        "filter" -> listOf(83.0, 88.0, 93.0)
        "aero" -> listOf(80.0, 85.0)
        "chemex" -> listOf(lowerC)
        else -> emptyList()
    }.map { degrees(it).toFloat() }
    val recipeBandText: String get() = if (recipeBand != null) "${if (kind == "filter") "Clásico" else "Referencia"} · $rangeText" else workingRangeText
    val recipePointsText: String? get() = if (kind == "filter") "4:6 · oscuro ${degrees(83.0)} · medio ${degrees(88.0)} · claro ${degrees(93.0)} ${if (fahrenheit) "°F" else "°C"}" else null
    // On-screen working limits are the reachable limits, not the unclipped source range.
    val workingRangeText: String get() {
        if (!hasReference) return "Según técnica"
        if (!hasReachableBand) return "Hervor aprox. · ${degrees(boilingC)} ${if (fahrenheit) "°F" else "°C"}"
        val symbol = if (fahrenheit) "°F" else "°C"
        val low = recommendedRange.start.toInt(); val high = recommendedRange.endInclusive.toInt()
        if (kind == "aero") return "Puntos · $low / $high $symbol"
        if (low == high) return "Punto · $low $symbol"
        return "${if (kind == "filter") "Clásico" else "Zona de trabajo"} · $low–$high $symbol"
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
        kind == "filter" && degrees(temperatureC) >= degrees(83.0) && degrees(temperatureC) < degrees(lowerC) -> "V60 · calor para otra técnica"
        kind == "chemex" -> if (warning) "Difiere del punto Chemex" else "Cerca del punto Chemex"
        kind == "aero" -> if (warning) "Otra receta AeroPress" else "Referencias AeroPress por tueste"
        degrees(temperatureC) < degrees(lowerC) -> "Calor bajo para $method"
        degrees(temperatureC) > degrees(upperC) -> "Calor alto para $method"
        else -> "Calor en zona de trabajo"
    }
    val compactDetail get() = when {
        openHotWater && temperatureC > boilingC -> "A tu altura, el agua hierve cerca de ${degrees(boilingC)} ${if (fahrenheit) "°F" else "°C"}. Baja el calor."
        !hasReachableBand && hasReference -> "A esta altura no alcanzas $rangeText. Usa el hervor local y ajusta molienda o tiempo."
        kind == "moka" -> "Usa llama baja o media. Retira al terminar."
        kind == "cold" -> "Usa agua a temperatura ambiente y deja reposar 8–24 h."
        kind == "custom" -> "Este método aún no tiene un rango de temperatura. Añade una técnica."
        kind == "filter" && degrees(temperatureC) >= degrees(83.0) && degrees(temperatureC) < degrees(lowerC) -> "V60 4:6: calor según tueste; ajusta molienda y vertidos."
        degrees(temperatureC) < degrees(lowerC) -> if (kind == "aero") "Agua por debajo de los puntos AeroPress. Prueba $rangeText según el tueste." else "Sube el agua a $rangeText. Puede faltar extracción."
        degrees(temperatureC) > degrees(upperC) -> if (kind == "aero") "Por encima de los puntos AeroPress. Úsalo si tu técnica lo indica." else "Baja el agua hacia $rangeText para $method. Prueba antes de cambiar la molienda."
        kind == "aero" -> "Tueste oscuro: ${degrees(80.0)} ${if (fahrenheit) "°F" else "°C"}. Medio o claro: ${degrees(85.0)} ${if (fahrenheit) "°F" else "°C"}."
        kind == "chemex" -> "Cerca de $rangeText, el punto indicado por Chemex. Prueba y ajusta."
        else -> "Estás en $rangeText para $method. Prueba la taza antes de ajustar."
    }
    // Recipe-specific alternatives, not a wider universal green zone. Mirror: LabModel.swift.
    val recipeAdvice: List<String> get() {
        if (kind != "filter") return listOf(compactDetail, detail, "Cambia una variable por vez y compara la taza en Cata.")
        val symbol = if (fahrenheit) "°F" else "°C"
        val context = when {
            openHotWater && temperatureC > boilingC -> "A tu altura, el agua hierve cerca de ${degrees(boilingC)} $symbol. No podrás mantenerla por encima de ese punto en una tetera abierta."
            degrees(temperatureC) < degrees(83.0) -> "${degrees(temperatureC)} $symbol está por debajo de los puntos de partida de estas dos técnicas. Prueba más calor antes de cambiar varias cosas a la vez. No lo tratamos como una zona recomendada."
            degrees(temperatureC) < degrees(92.0) -> "Esta temperatura puede encajar en una V60 4:6 según el tueste. No basta con bajar el calor de la técnica clásica: cambia también la molienda y los vertidos."
            else -> "Elige la técnica según el tueste y el sabor que buscas; la temperatura sola no determina el resultado."
        }
        return listOf(
            context,
            "V60 clásico · ${degrees(92.0)}–${degrees(96.0)} $symbol. Ejemplo Hario: 15 g y 250 ml, molienda media-fina, floración de 30 s y vertido circular suave; unos 2:30 min en total.",
            "V60 4:6 · Kasuya. Oscuro: ≈ ${degrees(83.0)} $symbol; medio: ≈ ${degrees(88.0)} $symbol; claro: ≈ ${degrees(93.0)} $symbol. Molienda media-gruesa a gruesa; proporción 1:15.",
            "Ejemplo 4:6 · 20 g y 300 ml: 60 ml a 0:00, 0:45, 1:30, 2:15 y 2:45; termina cerca de 3:30. Espera a que casi drene antes del siguiente vertido. Para oscuro, prueba dividir los últimos 180 ml en dos vertidos, no tres.",
            "Si queda aguada o ácida · prueba molienda un poco menos gruesa o sube el calor hacia el punto de tu tueste. Si tarda demasiado en drenar · prueba más gruesa. Si queda áspera · reduce la agitación y compara. Son ajustes de prueba, no garantías.",
            "La 4:6 se diseñó con el V60 Kasuya, que drena más lento. Con un V60 estándar ajusta la molienda al drenaje; no copies números de otro molino."
        )
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
