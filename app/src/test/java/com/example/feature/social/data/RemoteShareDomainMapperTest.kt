package com.example.feature.social.data

import com.example.data.remote.models.RemoteShare
import com.example.domain.model.SharedPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteShareDomainMapperTest {
    @Test
    fun `iOS technique snapshot preserves every pour and accumulated target`() {
        val mapped = RemoteShareDomainMapper.map(remoteShare("technique", validTechniquePayload())).getOrThrow()
        val technique = (mapped.payload as SharedPayload.TechniquePayload).technique

        assertEquals("V60", technique.method)
        assertEquals(120, technique.totalTimeSeconds)
        assertEquals(listOf(50, 190), technique.executionSteps.map { it.waterAddedMl })
        assertEquals(listOf(50, 240), technique.executionSteps.map { it.waterAccumulatedMl })
        assertEquals(listOf(1, 2), technique.executionSteps.map { it.stepNumber })
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `contradictory technique snapshot is rejected before creating a copy`() {
        val badSteps = (validTechniquePayload()["steps"] as List<Map<String, Any>>).mapIndexed { index, step ->
            if (index == 1) step + ("target_water_ml" to 230) else step
        }
        val result = RemoteShareDomainMapper.map(remoteShare("technique", validTechniquePayload() + ("steps" to badSteps)))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("No se creó ninguna copia"))
    }

    @Test
    fun `recipe snapshot preserves ingredients steps and suggested method`() {
        val payload: Map<String, Any> = mapOf(
            "name" to "V60 frutal",
            "recipeKind" to "BLACK_COFFEE",
            "intention" to "Dulzor",
            "suggestedMethodId" to "method-v60",
            "suggestedMethodName" to "V60",
            "tags" to "frutal, diario",
            "ingredients" to listOf(
                mapOf("name" to "Café", "amount" to 15.0, "unit" to "GRAMS"),
                mapOf("name" to "Agua", "amount" to 240.0, "unit" to "MILLILITERS")
            ),
            "steps" to listOf(
                mapOf("stepNumber" to 1, "instruction" to "Preinfusionar", "durationSeconds" to 30),
                mapOf("stepNumber" to 2, "instruction" to "Completar vertido", "durationSeconds" to 90)
            )
        )

        val mapped = RemoteShareDomainMapper.map(remoteShare("recipe", payload)).getOrThrow()
        val recipe = (mapped.payload as SharedPayload.RecipePayload).recipe

        assertEquals("method-v60", recipe.methodId)
        assertEquals("V60", recipe.method)
        assertEquals(listOf("Café", "Agua"), recipe.ingredients.map { it.name })
        assertEquals(listOf("Preinfusionar", "Completar vertido"), recipe.steps.map { it.instruction })
        assertEquals(listOf(1, 2), recipe.steps.map { it.stepNumber })
    }

    @Test
    fun `summary-only recipe is rejected instead of producing an empty local copy`() {
        val payload: Map<String, Any> = mapOf(
            "name" to "Receta incompleta",
            "ingredientsSummary" to "15 g café",
            "stepsSummary" to "Preparar"
        )

        val result = RemoteShareDomainMapper.map(remoteShare("recipe", payload))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IncompleteRemoteShareException)
    }

    private fun validTechniquePayload(): Map<String, Any> = mapOf(
        "name" to "Dos vertidos",
        "method" to "V60",
        "methodId" to "method-v60",
        "coffeeGrams" to 15.0,
        "waterMl" to 240,
        "ratio" to 16.0,
        "temperature" to 93,
        "executionMode" to "GUIDED",
        "grindValue" to 24.0,
        "totalTimeSeconds" to 120,
        "steps" to listOf(
            mapOf(
                "step_order" to 1, "title" to "Preinfusión", "duration_sec" to 30,
                "water_add_ml" to 50, "target_water_ml" to 50, "gesture" to "BLOOM", "intensity" to "MEDIUM"
            ),
            mapOf(
                "step_order" to 2, "title" to "Vertido", "duration_sec" to 90,
                "water_add_ml" to 190, "target_water_ml" to 240, "gesture" to "CIRCULAR_POUR", "intensity" to "MEDIUM"
            )
        )
    )

    private fun remoteShare(type: String, payload: Map<String, Any>) = RemoteShare(
        id = "share-1",
        entityType = type,
        entityId = "entity-1",
        fromUserId = "author-1",
        fromName = "Ana",
        fromHandle = "ana",
        targetUserId = null,
        visibility = "public",
        name = payload["name"] as? String ?: "Publicación",
        subtitle = null,
        message = "Pruébala",
        payloadSnapshotJson = payload,
        originalAuthorUserId = "author-1",
        originalAuthorName = "Ana",
        originalEntityId = "entity-original",
        createdAt = "2026-09-26T00:00:00Z",
        updatedAt = "2026-09-26T00:00:00Z"
    )
}
