package com.example.data.catalog

import com.example.data.database.BrewMethod
import com.example.data.database.Technique
import com.example.data.database.TechniqueStep
import com.example.data.database.UserMethodPreference

data class CatalogStep(
    val title: String,
    val durationSeconds: Int,
    val waterWeight: Int,
    val gesture: String,
    val note: String
)

data class CatalogTechnique(
    val id: String,
    val methodId: String,
    val name: String,
    val temperatureC: Int,
    val grindDescription: String,
    val grindValue: Double,
    val description: String,
    val steps: List<CatalogStep>
)

/** Catálogo local y determinista compartido por Calculadora y Preparar café. */
object BrewTechniqueCatalog {
    private const val METHOD_PREFIX = "11111111-1111-4000-8000-"
    private const val TECHNIQUE_PREFIX = "22222222-2222-4000-8000-"

    val methods = listOf(
        BrewMethod("${METHOD_PREFIX}000000000001", "v60", "V60", "POUR_OVER", 16f),
        BrewMethod("${METHOD_PREFIX}000000000002", "aeropress", "AeroPress", "HYBRID", 13f),
        BrewMethod("${METHOD_PREFIX}000000000003", "espresso", "Espresso", "PRESSURE", 2f),
        BrewMethod("${METHOD_PREFIX}000000000004", "french_press", "Prensa francesa", "IMMERSION", 15f),
        BrewMethod("${METHOD_PREFIX}000000000006", "chemex", "Chemex", "POUR_OVER", 16f),
        BrewMethod("${METHOD_PREFIX}000000000007", "moka", "Moka", "PRESSURE", 10f),
        BrewMethod("${METHOD_PREFIX}000000000008", "cold_brew", "Cold brew", "COLD", 8f)
    )

    val preferences = methods.mapIndexed { index, method ->
        UserMethodPreference(
            id = "33333333-3333-4000-8000-${(index + 1).toString().padStart(12, '0')}",
            methodId = method.id,
            isPinnedToCalculator = true,
            isActive = true,
            addedAt = "2026-01-${(index + 1).toString().padStart(2, '0')}T00:00:00Z"
        )
    }

    private fun t(method: Int, variant: Int, name: String, temp: Int, grind: String, grindValue: Double,
                  description: String, vararg steps: CatalogStep) = CatalogTechnique(
        id = "$TECHNIQUE_PREFIX${method.toString().padStart(2, '0')}${variant.toString().padStart(2, '0')}00000000",
        methodId = methods[method - 1].id,
        name = name,
        temperatureC = temp,
        grindDescription = grind,
        grindValue = grindValue,
        description = description,
        steps = steps.toList()
    )

    private fun s(title: String, seconds: Int, waterWeight: Int, gesture: String, note: String) =
        CatalogStep(title, seconds, waterWeight, gesture, note)

    val techniques = listOf(
        t(1, 1, "Clásica en 3 vertidos", 93, "Media fina", 20.0, "Balance y dulzura con tres vertidos controlados.",
            s("Bloom", 40, 2, "BLOOM", "Moja todo el café y agita suavemente."),
            s("Primer vertido", 45, 4, "CIRCULAR_POUR", "Vierte en círculos pequeños."),
            s("Vertido final", 55, 4, "CENTER_POUR", "Completa el agua y deja drenar.")),
        t(1, 2, "Pulsos 4:6", 92, "Media gruesa", 24.0, "Cinco pulsos para modular brillo, dulzura y cuerpo.",
            s("Primer pulso", 45, 2, "BLOOM", "Satura uniformemente."), s("Segundo pulso", 35, 2, "CIRCULAR_POUR", "Vierte con flujo medio."),
            s("Tercer pulso", 35, 2, "CENTER_POUR", "Espera a que baje el nivel."), s("Cuarto pulso", 35, 2, "CIRCULAR_POUR", "Mantén el lecho nivelado."),
            s("Pulso final", 45, 2, "CENTER_POUR", "Completa y deja drenar.")),
        t(1, 3, "Vertido continuo", 94, "Media fina", 19.0, "Flujo constante para una taza limpia y uniforme.",
            s("Bloom amplio", 45, 2, "BLOOM", "Agita para eliminar zonas secas."), s("Vertido continuo", 90, 8, "CIRCULAR_POUR", "Conserva una altura y flujo constantes.")),

        t(2, 1, "Estándar limpia", 85, "Media fina", 16.0, "Inmersión breve y presión suave.",
            s("Carga y mezcla", 30, 8, "STIR", "Añade agua y mezcla durante 10 segundos."), s("Inmersión", 45, 0, "WAIT", "Coloca el émbolo para conservar temperatura."), s("Presión", 30, 0, "PRESS", "Presiona lentamente hasta el silbido.")),
        t(2, 2, "Invertida dulce", 88, "Media", 18.0, "Mayor contacto para resaltar dulzura y cuerpo.",
            s("Preinfusión invertida", 35, 3, "BLOOM", "Mezcla con cuidado."), s("Completar agua", 45, 7, "STIR", "Llena y remueve dos veces."), s("Giro y presión", 35, 0, "PRESS", "Gira con seguridad y presiona suave.")),
        t(2, 3, "Bypass brillante", 82, "Media fina", 15.0, "Concentrado corto diluido al final para mayor claridad.",
            s("Concentrado", 45, 7, "STIR", "Mezcla el concentrado durante 10 segundos."), s("Presión breve", 25, 0, "PRESS", "Presiona de forma constante."), s("Bypass", 15, 3, "CENTER_POUR", "Añade el agua restante directamente a la taza.")),

        t(3, 1, "Flujo clásico", 93, "Fina", 8.0, "Extracción estable con una salida uniforme.",
            s("Preinfusión", 6, 1, "WAIT", "Observa que toda la pastilla comience a humedecerse."), s("Extracción", 24, 9, "PRESS", "Detén al alcanzar el rendimiento calculado.")),
        t(3, 2, "Preinfusión suave", 92, "Fina", 7.0, "Inicio lento para reducir canalización.",
            s("Saturación", 8, 1, "WAIT", "Usa flujo bajo para saturar la pastilla."), s("Pausa", 4, 0, "WAIT", "Deja que la pastilla se expanda."), s("Flujo principal", 23, 9, "PRESS", "Busca un hilo continuo y centrado.")),
        t(3, 3, "Presión descendente", 94, "Fina", 9.0, "Final suave para una extracción más redonda.",
            s("Inicio intenso", 10, 3, "PRESS", "Inicia con flujo firme."), s("Flujo estable", 12, 4, "PRESS", "Mantén el color uniforme."), s("Final suave", 10, 3, "PRESS", "Reduce el flujo antes de cortar.")),

        t(4, 1, "Inmersión clásica", 94, "Gruesa", 28.0, "Cuerpo redondo con cuatro minutos de inmersión.",
            s("Saturar", 30, 10, "STIR", "Añade toda el agua y mezcla suavemente."), s("Inmersión", 210, 0, "WAIT", "Espera sin mover la prensa."), s("Prensar", 25, 0, "PRESS", "Baja el émbolo sin forzar.")),
        t(4, 2, "Costra limpia", 93, "Media gruesa", 26.0, "Decantación lenta para reducir sedimento.",
            s("Inmersión", 240, 10, "WAIT", "Añade toda el agua y deja formar la costra."), s("Romper costra", 30, 0, "STIR", "Rompe la costra y retira la espuma."), s("Decantar", 300, 0, "WAIT", "Espera y sirve sin hundir hasta el fondo.")),
        t(4, 3, "Agitación breve", 92, "Gruesa", 29.0, "Más extracción inicial con una mezcla corta.",
            s("Llenar", 25, 10, "CIRCULAR_POUR", "Añade toda el agua."), s("Agitar", 15, 0, "STIR", "Mezcla de adelante hacia atrás."), s("Reposar y prensar", 210, 0, "PRESS", "Reposa y presiona lentamente.")),

        t(5, 1, "Clásica en pulsos", 93, "Media gruesa", 25.0, "Vertidos espaciados para una taza limpia.",
            s("Bloom", 45, 2, "BLOOM", "Satura hasta los bordes."), s("Primer pulso", 55, 4, "CIRCULAR_POUR", "Vierte sin tocar el papel."), s("Pulso final", 70, 4, "CENTER_POUR", "Completa y permite el drenado.")),
        t(5, 2, "Vertido continuo", 94, "Media", 23.0, "Flujo estable para mayor cuerpo.",
            s("Bloom", 45, 2, "BLOOM", "Agita la cama suavemente."), s("Flujo continuo", 120, 8, "CIRCULAR_POUR", "Conserva el nivel de agua constante.")),
        t(5, 3, "Alta claridad", 92, "Gruesa", 27.0, "Cuatro pulsos suaves para favorecer claridad.",
            s("Bloom", 45, 2, "BLOOM", "Moja de manera uniforme."), s("Pulso uno", 45, 3, "CIRCULAR_POUR", "Vierte despacio."), s("Pulso dos", 45, 3, "CENTER_POUR", "Espera a que baje el agua."), s("Pulso final", 60, 2, "CIRCULAR_POUR", "Completa sin agitar.")),

        t(6, 1, "Clásica controlada", 90, "Media fina", 14.0, "Calentamiento gradual y extracción sin hervor agresivo.",
            s("Cargar cámara", 20, 10, "CENTER_POUR", "Llena la base con el agua calculada."), s("Calentar", 180, 0, "WAIT", "Usa fuego medio y deja la tapa abierta."), s("Finalizar", 30, 0, "WAIT", "Retira al aclararse el flujo y enfría la base.")),
        t(6, 2, "Agua precalentada", 92, "Media fina", 13.0, "Preparación rápida para limitar sabores tostados.",
            s("Montar", 25, 10, "CENTER_POUR", "Añade agua caliente y monta con una toalla."), s("Extraer", 120, 0, "WAIT", "Usa fuego bajo."), s("Cortar extracción", 20, 0, "WAIT", "Enfría la base antes del borboteo fuerte.")),
        t(6, 3, "Suave para leche", 88, "Media", 16.0, "Flujo lento y redondo pensado para combinar con leche.",
            s("Preparar", 25, 10, "CENTER_POUR", "Nivela el café sin compactarlo."), s("Flujo lento", 210, 0, "WAIT", "Mantén fuego bajo."), s("Mezclar", 15, 0, "STIR", "Revuelve el café servido para homogeneizar.")),

        t(7, 1, "Inmersión balanceada 12 h", 20, "Muy gruesa", 32.0, "Extracción en frío lista para beber.",
            s("Saturar", 60, 10, "STIR", "Añade toda el agua y mezcla."), s("Reposar 12 horas", 43200, 0, "WAIT", "Tapa y conserva en refrigeración."), s("Filtrar", 180, 0, "WAIT", "Filtra sin presionar el café.")),
        t(7, 2, "Brillante 8 h", 20, "Gruesa", 29.0, "Tiempo corto para conservar brillo y ligereza.",
            s("Mezcla inicial", 60, 10, "STIR", "Satura todo el café."), s("Reposar 8 horas", 28800, 0, "WAIT", "Mantén refrigerado."), s("Filtrado fino", 240, 0, "WAIT", "Pasa por filtro de papel.")),
        t(7, 3, "Concentrado 16 h", 20, "Muy gruesa", 34.0, "Perfil intenso para servir con agua, hielo o leche.",
            s("Saturar", 90, 10, "STIR", "Mezcla hasta eliminar bolsas secas."), s("Reposar 16 horas", 57600, 0, "WAIT", "Tapa y refrigera."), s("Filtrar concentrado", 300, 0, "WAIT", "Filtra lentamente y diluye al servir."))
    )

    fun methodName(methodId: String): String = methods.firstOrNull { it.id == methodId }?.nameKey ?: methodId
    fun methodId(methodName: String): String? = methods.firstOrNull { it.nameKey.equals(methodName, true) }?.id
    fun isBuiltInTechnique(id: String): Boolean = techniques.any { it.id == id }
    fun firstTechniqueFor(methodName: String): CatalogTechnique? = methodId(methodName)?.let { id -> techniques.firstOrNull { it.methodId == id } }

    fun entity(template: CatalogTechnique): Technique {
        val method = methods.first { it.id == template.methodId }
        val dose = if (method.code == "cold_brew") 50f else if (method.code == "espresso") 18f else 15f
        val water = (dose * method.defaultRatio).toInt()
        return Technique(
            id = template.id, name = template.name, methodId = template.methodId,
            doseG = dose, waterMl = water, ratio = method.defaultRatio,
            temperatureC = template.temperatureC, grindValue = template.grindValue,
            grindDescription = template.grindDescription, notes = template.description,
            totalTimeSeconds = template.steps.sumOf { it.durationSeconds }, author = "Cupa",
            description = template.description, visibility = "PUBLIC"
        )
    }

    fun steps(template: CatalogTechnique, waterMl: Int, techniqueId: String = template.id): List<TechniqueStep> {
        val positive = template.steps.filter { it.waterWeight > 0 }
        val totalWeight = positive.sumOf { it.waterWeight }.coerceAtLeast(1)
        var accumulated = 0
        var positiveIndex = 0
        return template.steps.mapIndexed { index, step ->
            val added = if (step.waterWeight <= 0) 0 else {
                positiveIndex++
                if (positiveIndex == positive.size) waterMl - accumulated
                else ((waterMl.toDouble() * step.waterWeight) / totalWeight).toInt().coerceAtLeast(0)
            }
            accumulated += added
            TechniqueStep(
                id = "$techniqueId-step-${index + 1}", techniqueId = techniqueId,
                stepNumber = index + 1, title = step.title, durationSeconds = step.durationSeconds,
                waterAddedMl = added, waterAccumulatedMl = accumulated,
                intensity = "MEDIUM", gesture = step.gesture, stepNote = step.note,
                targetWaterMl = accumulated
            )
        }
    }

    fun scaleSteps(steps: List<TechniqueStep>, sourceWaterMl: Int, targetWaterMl: Int): List<TechniqueStep> {
        if (steps.isEmpty() || sourceWaterMl <= 0 || sourceWaterMl == targetWaterMl) return steps
        val waterSteps = steps.filter { it.waterAddedMl > 0 }
        var accumulated = 0
        var waterIndex = 0
        return steps.map { step ->
            val added = if (step.waterAddedMl <= 0) 0 else {
                waterIndex++
                if (waterIndex == waterSteps.size) targetWaterMl - accumulated
                else ((step.waterAddedMl.toDouble() / sourceWaterMl) * targetWaterMl).toInt().coerceAtLeast(0)
            }
            accumulated += added
            step.copy(waterAddedMl = added, waterAccumulatedMl = accumulated, targetWaterMl = accumulated)
        }
    }
}
