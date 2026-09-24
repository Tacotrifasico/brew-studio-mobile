package com.example.data.validation

object BrewInputRules {
    const val MIN_COFFEE_GRAMS = 1f
    const val MAX_COFFEE_GRAMS = 100f
    const val MIN_RATIO = 1f
    const val MAX_RATIO = 40f
    const val MIN_WATER_ML = 10
    const val MAX_WATER_ML = 2000
    const val MIN_TEMPERATURE_C = 60
    const val MAX_TEMPERATURE_C = 100

    data class NormalizedTechnique(
        val waterMl: Int,
        val ratio: Float,
        val totalTimeSeconds: Int,
        val accumulatedWaterMl: List<Int>
    )

    fun validCoffee(value: Float) = value in MIN_COFFEE_GRAMS..MAX_COFFEE_GRAMS
    fun validRatio(value: Float) = value in MIN_RATIO..MAX_RATIO
    fun validWater(value: Int) = value in MIN_WATER_ML..MAX_WATER_ML
    fun validTemperature(value: Int) = value in MIN_TEMPERATURE_C..MAX_TEMPERATURE_C

    fun experimentError(method: String, coffee: Float?, water: Int?, temperature: Int?): String? {
        if (method.isBlank()) return "Escribe el método o la hipótesis."
        if (coffee == null || !validCoffee(coffee)) return "El café debe estar entre 1 y 100 g."
        if (water == null || !validWater(water)) return "El agua debe estar entre 10 y 2000 ml."
        if (temperature == null || !validTemperature(temperature)) return "La temperatura debe estar entre 60 y 100 °C."
        if (!validRatio(water / coffee)) return "La proporción resultante debe estar entre 1:1 y 1:40."
        return null
    }

    fun techniqueError(
        name: String,
        coffee: Float?,
        temperature: Int?,
        stepTitles: List<String>,
        stepDurations: List<Int?>,
        stepWaters: List<Int?>
    ): String? {
        if (name.isBlank()) return "Escribe un nombre para la técnica."
        if (coffee == null || !validCoffee(coffee)) return "El café debe estar entre 1 y 100 g."
        if (temperature == null || !validTemperature(temperature)) return "La temperatura debe estar entre 60 y 100 °C."
        if (stepTitles.isEmpty() || stepTitles.any { it.isBlank() }) return "Todos los pasos necesitan un título."
        if (stepDurations.size != stepTitles.size || stepDurations.any { it == null || it <= 0 }) return "Cada paso necesita una duración mayor a 0 segundos."
        if (stepWaters.size != stepTitles.size || stepWaters.any { it == null || it < 0 }) return "El agua de cada paso debe ser 0 ml o más."
        val totalWater = stepWaters.filterNotNull().sum()
        if (!validWater(totalWater)) return "La suma de los vertidos debe estar entre 10 y 2000 ml."
        val calculatedRatio = totalWater / coffee
        if (!validRatio(calculatedRatio)) return "La proporción resultante debe estar entre 1:1 y 1:40."
        return null
    }

    fun normalizeTechnique(
        name: String,
        coffee: Float?,
        temperature: Int?,
        stepTitles: List<String>,
        stepDurations: List<Int?>,
        stepWaters: List<Int?>
    ): NormalizedTechnique? {
        if (techniqueError(name, coffee, temperature, stepTitles, stepDurations, stepWaters) != null) return null
        val validCoffee = coffee ?: return null
        val validDurations = stepDurations.map { it ?: return null }
        val validWaters = stepWaters.map { it ?: return null }
        var accumulated = 0
        val accumulatedWater = validWaters.map { added ->
            accumulated += added
            accumulated
        }
        return NormalizedTechnique(
            waterMl = accumulated,
            ratio = accumulated / validCoffee,
            totalTimeSeconds = validDurations.sum(),
            accumulatedWaterMl = accumulatedWater
        )
    }
}
