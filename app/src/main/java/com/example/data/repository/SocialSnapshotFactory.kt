package com.example.data.repository

import com.example.data.database.Recipe
import com.example.data.database.RecipeIngredient
import com.example.data.database.RecipeStep
import com.example.data.database.Technique
import com.example.data.database.TechniqueStep
import com.example.data.validation.BrewInputRules
import kotlin.math.abs

/** Builds the immutable payload shared by Android and iOS after validating the full aggregate. */
object SocialSnapshotFactory {
    fun recipe(
        recipe: Recipe,
        ingredients: List<RecipeIngredient>,
        steps: List<RecipeStep>,
        methodName: String
    ): Result<Map<String, Any>> = runCatching {
        require(ingredients.isNotEmpty() && steps.isNotEmpty()) {
            "La receta necesita ingredientes y pasos completos antes de compartirse."
        }
        require(ingredients.none { it.name.isBlank() || !it.amount.isFinite() || it.amount <= 0f || it.unit.isBlank() }) {
            "La receta contiene un ingrediente incompleto o una cantidad inválida."
        }
        require(steps.map { it.stepNumber } == (1..steps.size).toList() && steps.none { it.instruction.isBlank() || (it.durationSeconds != null && it.durationSeconds <= 0) }) {
            "La receta contiene pasos incompletos o desordenados."
        }
        val ingredientPayload = ingredients.mapIndexed { index, ingredient ->
            mapOf<String, Any>(
                "name" to ingredient.name,
                "amount" to ingredient.amount,
                "unit" to ingredient.unit,
                "orderIndex" to index
            )
        }
        val stepPayload = steps.map { step ->
            linkedMapOf<String, Any>(
                "stepNumber" to step.stepNumber,
                "step_order" to step.stepNumber,
                "instruction" to step.instruction
            ).apply {
                step.durationSeconds?.let {
                    put("durationSeconds", it)
                    put("duration_sec", it)
                }
            }
        }
        mapOf(
            "name" to recipe.name,
            "recipeKind" to recipe.recipeKind,
            "intention" to recipe.intention,
            "suggestedMethodId" to (recipe.suggestedMethodId ?: ""),
            "suggestedMethodName" to methodName,
            "method" to methodName,
            "ingredients" to ingredientPayload,
            "steps" to stepPayload,
            "ingredientsSummary" to recipe.ingredientsSummary,
            "stepsSummary" to recipe.stepsSummary,
            "tags" to recipe.tags
        )
    }

    fun technique(
        technique: Technique,
        steps: List<TechniqueStep>,
        methodName: String
    ): Result<Map<String, Any>> = runCatching {
        require(methodName.isNotBlank()) { "La técnica no tiene un método reconocible y no puede compartirse." }
        val normalized = BrewInputRules.normalizeTechnique(
            name = technique.name,
            coffee = technique.doseG,
            temperature = technique.temperatureC,
            stepTitles = steps.map { it.title },
            stepDurations = steps.map { it.durationSeconds },
            stepWaters = steps.map { it.waterAddedMl }
        ) ?: throw IllegalStateException("La técnica contiene cantidades o pasos inválidos y no puede compartirse.")
        require(
            normalized.waterMl == technique.waterMl &&
                abs(normalized.ratio - technique.ratio) <= 0.05f &&
                normalized.totalTimeSeconds == technique.totalTimeSeconds &&
                steps.map { it.stepNumber } == (1..steps.size).toList() &&
                steps.map { it.waterAccumulatedMl } == normalized.accumulatedWaterMl
        ) { "La técnica contradice sus vertidos, proporción o tiempo total. Corrígela antes de compartir." }

        val stepsPayload = steps.map { step ->
            mapOf<String, Any>(
                "stepNumber" to step.stepNumber,
                "step_order" to step.stepNumber,
                "title" to step.title,
                "durationSeconds" to step.durationSeconds,
                "duration_sec" to step.durationSeconds,
                "waterAddedMl" to step.waterAddedMl,
                "water_add_ml" to step.waterAddedMl,
                "waterAccumulatedMl" to step.waterAccumulatedMl,
                "target_water_ml" to step.waterAccumulatedMl,
                "gesture" to step.gesture,
                "intensity" to step.intensity,
                "stepNote" to step.stepNote,
                "note" to step.stepNote
            )
        }
        mapOf(
            "name" to technique.name,
            "method" to methodName,
            "methodName" to methodName,
            "methodId" to technique.methodId,
            "doseG" to technique.doseG,
            "coffeeGrams" to technique.doseG,
            "waterMl" to technique.waterMl,
            "ratio" to technique.ratio,
            "temperatureC" to technique.temperatureC,
            "temperature" to technique.temperatureC,
            "executionMode" to technique.executionMode,
            "grindValue" to (technique.grindValue ?: 18.0),
            "grindDescription" to (technique.grindDescription ?: ""),
            "grindUnit" to technique.grindUnit,
            "notes" to technique.notes,
            "description" to (technique.description ?: ""),
            "totalTimeSeconds" to technique.totalTimeSeconds,
            "steps" to stepsPayload
        )
    }
}
