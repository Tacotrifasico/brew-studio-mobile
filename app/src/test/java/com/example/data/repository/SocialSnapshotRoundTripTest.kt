package com.example.data.repository

import com.example.data.database.Recipe
import com.example.data.database.RecipeIngredient
import com.example.data.database.RecipeStep
import com.example.data.database.Technique
import com.example.data.database.TechniqueStep
import com.example.data.remote.models.RemoteShare
import com.example.domain.model.SharedPayload
import com.example.feature.social.data.RemoteShareDomainMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialSnapshotRoundTripTest {
    @Test
    fun `published recipe contains executable children and survives import mapping`() {
        val recipe = Recipe(
            id = "recipe-local", name = "V60 frutal", recipeKind = "BLACK_COFFEE", intention = "Dulzor",
            suggestedMethodId = "method-v60", ingredientsSummary = "15 g café · 240 ml agua",
            stepsSummary = "Preinfusión · Vertido", tags = "frutal", remoteId = "recipe-remote"
        )
        val ingredients = listOf(
            RecipeIngredient(recipeId = recipe.id, name = "Café", amount = 15f, unit = "GRAMS", orderIndex = 0),
            RecipeIngredient(recipeId = recipe.id, name = "Agua", amount = 240f, unit = "MILLILITERS", orderIndex = 1)
        )
        val steps = listOf(
            RecipeStep(recipeId = recipe.id, instruction = "Preinfusionar", stepNumber = 1, durationSeconds = 30),
            RecipeStep(recipeId = recipe.id, instruction = "Completar vertido", stepNumber = 2, durationSeconds = 90)
        )

        val snapshot = SocialSnapshotFactory.recipe(recipe, ingredients, steps, "V60").getOrThrow()
        val imported = (RemoteShareDomainMapper.map(remoteShare("recipe", snapshot)).getOrThrow().payload as SharedPayload.RecipePayload).recipe

        assertEquals("method-v60", imported.methodId)
        assertEquals(listOf("Café", "Agua"), imported.ingredients.map { it.name })
        assertEquals(listOf("Preinfusionar", "Completar vertido"), imported.steps.map { it.instruction })
    }

    @Test
    fun `published technique keeps cumulative targets and survives import mapping`() {
        val technique = Technique(
            id = "tech-local", name = "Dos vertidos", methodId = "method-v60", doseG = 15f,
            waterMl = 240, ratio = 16f, temperatureC = 93, totalTimeSeconds = 120, remoteId = "tech-remote"
        )
        val steps = listOf(
            TechniqueStep(techniqueId = technique.id, stepNumber = 1, title = "Preinfusión", durationSeconds = 30, waterAddedMl = 50, waterAccumulatedMl = 50),
            TechniqueStep(techniqueId = technique.id, stepNumber = 2, title = "Vertido", durationSeconds = 90, waterAddedMl = 190, waterAccumulatedMl = 240)
        )

        val snapshot = SocialSnapshotFactory.technique(technique, steps, "V60").getOrThrow()
        val imported = (RemoteShareDomainMapper.map(remoteShare("technique", snapshot)).getOrThrow().payload as SharedPayload.TechniquePayload).technique

        assertEquals(listOf(50, 240), imported.executionSteps.map { it.waterAccumulatedMl })
        assertEquals(120, imported.totalTimeSeconds)
        assertEquals("method-v60", imported.methodId)
    }

    @Test
    fun `contradictory local technique cannot be published`() {
        val technique = Technique(
            id = "tech-local", name = "Inválida", methodId = "method-v60", doseG = 15f,
            waterMl = 240, ratio = 16f, temperatureC = 93, totalTimeSeconds = 120, remoteId = "tech-remote"
        )
        val steps = listOf(
            TechniqueStep(techniqueId = technique.id, stepNumber = 1, title = "Vertido", durationSeconds = 120, waterAddedMl = 230, waterAccumulatedMl = 230)
        )

        assertTrue(SocialSnapshotFactory.technique(technique, steps, "V60").isFailure)
    }

    private fun remoteShare(type: String, payload: Map<String, Any>) = RemoteShare(
        id = "share-1", entityType = type, entityId = "entity-1", fromUserId = "author-1", fromName = "Ana",
        fromHandle = "ana", targetUserId = null, visibility = "public", name = payload["name"] as String,
        subtitle = null, message = null, payloadSnapshotJson = payload, originalAuthorUserId = "author-1",
        originalAuthorName = "Ana", originalEntityId = "entity-original", createdAt = "", updatedAt = ""
    )
}
