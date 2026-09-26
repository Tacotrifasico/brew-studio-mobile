package com.example.feature.social.data

import com.example.data.remote.models.RemoteShare
import com.example.data.validation.BrewInputRules
import com.example.domain.model.Attribution
import com.example.domain.model.BrewShare
import com.example.domain.model.DomainRecipe
import com.example.domain.model.ExecutionStep
import com.example.domain.model.PreparationTechnique
import com.example.domain.model.RecipeIngredient
import com.example.domain.model.RecipeStepItem
import com.example.domain.model.ShareEntityType
import com.example.domain.model.ShareVisibility
import com.example.domain.model.SharedPayload
import kotlin.math.abs
import kotlin.math.roundToInt

class IncompleteRemoteShareException(detail: String) : IllegalArgumentException(
    "La publicación está incompleta o contiene datos contradictorios: $detail. No se creó ninguna copia."
)

/** Converts an immutable remote snapshot into a domain share without inventing brew data. */
object RemoteShareDomainMapper {
    fun map(share: RemoteShare): Result<BrewShare> = runCatching {
        val entityType = when (share.entityType.trim().lowercase()) {
            "recipe" -> ShareEntityType.RECIPE
            "technique" -> ShareEntityType.TECHNIQUE
            else -> invalid("el tipo de contenido no es compatible")
        }
        val snapshot = share.payloadSnapshotJson.child(
            if (entityType == ShareEntityType.RECIPE) "recipe" else "technique"
        ) ?: share.payloadSnapshotJson
        val originalAuthorId = share.originalAuthorUserId.nonBlankOrNull() ?: share.fromUserId
        val originalAuthorName = share.originalAuthorName.nonBlankOrNull() ?: share.fromName
        val originalEntityId = share.originalEntityId.nonBlankOrNull() ?: share.entityId
        requireValue(originalAuthorId.isNotBlank() && originalAuthorName.isNotBlank() && originalEntityId.isNotBlank(), "falta la atribución de origen")
        val attribution = Attribution(
            required = true,
            originalAuthorUserId = originalAuthorId,
            originalAuthorName = originalAuthorName,
            originalEntityId = originalEntityId
        )
        val payload = when (entityType) {
            ShareEntityType.RECIPE -> SharedPayload.RecipePayload(mapRecipe(share, snapshot, attribution))
            ShareEntityType.TECHNIQUE -> SharedPayload.TechniquePayload(mapTechnique(share, snapshot, attribution))
        }
        val visibility = when (share.visibility.trim().lowercase()) {
            "public" -> ShareVisibility.PUBLIC
            "direct" -> ShareVisibility.DIRECT
            else -> invalid("la visibilidad no es compatible")
        }
        requireValue(visibility != ShareVisibility.DIRECT || !share.targetUserId.isNullOrBlank(), "una publicación directa no tiene destinatario")
        BrewShare(
            id = share.id,
            entityType = entityType,
            entityId = share.entityId,
            name = snapshot.text("name").nonBlankOrNull() ?: share.name,
            subtitle = share.subtitle,
            fromUserId = share.fromUserId,
            fromDisplayName = share.fromName,
            fromHandle = share.fromHandle,
            targetUserId = share.targetUserId,
            visibility = visibility,
            message = share.message,
            attribution = attribution,
            payload = payload
        )
    }

    private fun mapRecipe(share: RemoteShare, value: Map<String, Any>, attribution: Attribution): DomainRecipe {
        val name = value.text("name").nonBlankOrNull() ?: share.name.nonBlankOrNull() ?: invalid("falta el nombre de la receta")
        val ingredientRows = value.objectList("ingredients") ?: invalid("faltan los ingredientes de la receta")
        requireValue(ingredientRows.isNotEmpty(), "faltan los ingredientes de la receta")
        val ingredients = ingredientRows.mapIndexed { index, row ->
            val ingredientName = row.text("name").nonBlankOrNull() ?: invalid("el ingrediente ${index + 1} no tiene nombre")
            val amount = row.number("amount")?.toFloat() ?: invalid("el ingrediente ${index + 1} no tiene cantidad")
            requireValue(amount.isFinite() && amount > 0f, "la cantidad del ingrediente ${index + 1} no es válida")
            val unit = row.text("unit").nonBlankOrNull() ?: invalid("el ingrediente ${index + 1} no tiene unidad")
            RecipeIngredient(name = ingredientName, amount = amount, unit = unit, orderIndex = index)
        }

        val stepRows = value.objectList("steps") ?: invalid("faltan los pasos de la receta")
        requireValue(stepRows.isNotEmpty(), "faltan los pasos de la receta")
        val steps = stepRows.mapIndexed { index, row ->
            val suppliedOrder = row.exactInt("stepNumber", "step_number", "step_order")
            requireValue(suppliedOrder == null || suppliedOrder == index + 1, "los pasos de la receta están desordenados")
            val instruction = row.text("instruction", "title").nonBlankOrNull() ?: invalid("el paso ${index + 1} no tiene instrucción")
            val durationRaw = row.first("durationSeconds", "duration_seconds", "duration_sec")
            val duration = durationRaw?.toExactInt()
            requireValue(durationRaw == null || (duration != null && duration > 0), "la duración del paso ${index + 1} no es válida")
            RecipeStepItem(instruction = instruction, stepNumber = index + 1, durationSeconds = duration)
        }

        val method = value.text("suggestedMethodName", "suggested_method_name", "method", "methodName", "method_name").nonBlankOrNull()
        val methodId = value.text("suggestedMethodId", "suggested_method_id", "methodId", "method_id").nonBlankOrNull()
        return DomainRecipe(
            id = share.entityId,
            ownerUserId = share.fromUserId,
            name = name,
            method = method,
            methodId = methodId,
            recipeKind = value.text("recipeKind", "recipe_kind").nonBlankOrNull() ?: "BLACK_COFFEE",
            intention = value.text("intention") ?: "",
            ingredients = ingredients,
            steps = steps,
            ingredientsSummary = value.text("ingredientsSummary", "ingredients_summary") ?: ingredients.joinToString(" · ") { "${it.amount} ${it.unit} ${it.name}" },
            stepsSummary = value.text("stepsSummary", "steps_summary") ?: steps.joinToString(" · ") { it.instruction },
            tags = value.tags(),
            originalAuthorUserId = attribution.originalAuthorUserId,
            originalAuthorName = attribution.originalAuthorName,
            originalEntityId = attribution.originalEntityId,
            attribution = attribution
        )
    }

    private fun mapTechnique(share: RemoteShare, value: Map<String, Any>, attribution: Attribution): PreparationTechnique {
        val name = value.text("name").nonBlankOrNull() ?: share.name.nonBlankOrNull() ?: invalid("falta el nombre de la técnica")
        val method = value.text("method", "methodName", "method_name").nonBlankOrNull()
        val methodId = value.text("methodId", "method_id").nonBlankOrNull()
        requireValue(method != null || methodId != null, "falta el método de preparación")
        val coffee = value.number("coffeeGrams", "coffee_grams", "doseG", "dose_g")
            ?: invalid("falta la dosis de café")
        val water = value.number("waterMl", "water_ml")?.toExactInt()
            ?: invalid("falta el agua total")
        val temperature = value.number("temperature", "temperatureC", "temperature_c")?.toExactInt()
            ?: invalid("falta la temperatura")
        requireValue(coffee.isFinite() && BrewInputRules.validCoffee(coffee.toFloat()), "la dosis de café está fuera de rango")
        requireValue(BrewInputRules.validWater(water), "el agua total está fuera de rango")
        requireValue(BrewInputRules.validTemperature(temperature), "la temperatura está fuera de rango")

        val stepRows = value.objectList("steps") ?: invalid("faltan los pasos de la técnica")
        requireValue(stepRows.isNotEmpty(), "faltan los pasos de la técnica")
        var accumulated = 0
        val steps = stepRows.mapIndexed { index, row ->
            val order = row.exactInt("stepNumber", "step_number", "step_order")
                ?: invalid("el paso ${index + 1} no tiene orden")
            requireValue(order == index + 1, "los pasos de la técnica no son consecutivos")
            val title = row.text("title").nonBlankOrNull() ?: invalid("el paso ${index + 1} no tiene título")
            val duration = row.exactInt("durationSeconds", "duration_seconds", "duration_sec")
                ?: invalid("el paso ${index + 1} no tiene duración")
            val added = row.exactInt("waterAddedMl", "water_added_ml", "water_add_ml")
                ?: invalid("el paso ${index + 1} no indica cuánta agua agregar")
            val target = row.exactInt("waterAccumulatedMl", "water_accumulated_ml", "targetWaterMl", "target_water_ml")
                ?: invalid("el paso ${index + 1} no indica el total acumulado")
            requireValue(duration > 0, "la duración del paso ${index + 1} no es válida")
            requireValue(added >= 0, "el agua del paso ${index + 1} no es válida")
            accumulated += added
            requireValue(target == accumulated, "el total acumulado del paso ${index + 1} no coincide")
            ExecutionStep(
                stepNumber = order,
                title = title,
                durationSeconds = duration,
                waterAddedMl = added,
                waterAccumulatedMl = target,
                intensity = row.text("intensity").nonBlankOrNull() ?: "MEDIUM",
                gesture = row.text("gesture").nonBlankOrNull() ?: "CIRCULAR_POUR",
                stepNote = row.text("note", "stepNote", "step_note") ?: ""
            )
        }
        requireValue(accumulated == water, "la suma de los vertidos no coincide con el agua total")
        val calculatedRatio = water / coffee
        requireValue(BrewInputRules.validRatio(calculatedRatio.toFloat()), "la proporción está fuera de rango")
        value.number("ratio")?.let { publishedRatio ->
            requireValue(publishedRatio.isFinite() && abs(publishedRatio - calculatedRatio) <= 0.05, "la proporción no coincide con café y agua")
        }
        val calculatedDuration = steps.sumOf { it.durationSeconds }
        value.exactInt("totalTimeSeconds", "total_time_seconds")?.let { publishedDuration ->
            requireValue(publishedDuration == calculatedDuration, "el tiempo total no coincide con los pasos")
        }

        return PreparationTechnique(
            id = share.entityId,
            ownerUserId = share.fromUserId,
            name = name,
            method = method,
            methodId = methodId,
            coffeeGrams = coffee,
            waterMl = water.toDouble(),
            grind = value.number("grind", "grindValue", "grind_value", "grindClicks", "grind_clicks"),
            grindDescription = value.text("grindDescription", "grind_description", "grindClicks", "grind_clicks"),
            temperatureC = temperature.toDouble(),
            totalTimeSeconds = calculatedDuration,
            executionMode = value.text("executionMode", "execution_mode").nonBlankOrNull() ?: "GUIDED",
            executionSteps = steps,
            notes = value.text("notes"),
            originalAuthorUserId = attribution.originalAuthorUserId,
            originalAuthorName = attribution.originalAuthorName,
            originalEntityId = attribution.originalEntityId,
            attribution = attribution
        )
    }

    private fun invalid(detail: String): Nothing = throw IncompleteRemoteShareException(detail)
    private fun requireValue(condition: Boolean, detail: String) { if (!condition) invalid(detail) }

    private fun Map<String, Any>.first(vararg keys: String): Any? = keys.firstNotNullOfOrNull { this[it] }
    private fun Map<String, Any>.text(vararg keys: String): String? = when (val raw = first(*keys)) {
        is String -> raw
        else -> null
    }
    private fun Map<String, Any>.number(vararg keys: String): Double? = when (val raw = first(*keys)) {
        is Number -> raw.toDouble()
        is String -> raw.trim().replace(',', '.').toDoubleOrNull()
        else -> null
    }
    private fun Map<String, Any>.exactInt(vararg keys: String): Int? = first(*keys)?.toExactInt()
    private fun Any.toExactInt(): Int? {
        val value = when (this) {
            is Number -> toDouble()
            is String -> trim().replace(',', '.').toDoubleOrNull()
            else -> null
        } ?: return null
        if (!value.isFinite() || value < Int.MIN_VALUE || value > Int.MAX_VALUE || abs(value - value.roundToInt()) > 0.000_001) return null
        return value.roundToInt()
    }
    private fun Map<String, Any>.child(key: String): Map<String, Any>? = first(key).asStringMap()
    private fun Map<String, Any>.objectList(key: String): List<Map<String, Any>>? = (first(key) as? List<*>)?.map { item ->
        item.asStringMap() ?: return null
    }
    private fun Any?.asStringMap(): Map<String, Any>? {
        val raw = this as? Map<*, *> ?: return null
        val result = linkedMapOf<String, Any>()
        for ((key, value) in raw) if (key is String && value != null) result[key] = value
        return result
    }
    private fun Map<String, Any>.tags(): String = when (val raw = first("tags")) {
        is String -> raw
        is List<*> -> raw.filterIsInstance<String>().joinToString(", ")
        else -> ""
    }
    private fun String?.nonBlankOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
