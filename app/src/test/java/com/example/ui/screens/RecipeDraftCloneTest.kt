package com.example.ui.screens

import com.example.data.database.Recipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RecipeDraftCloneTest {
    @Test fun clonedDraftKeepsSourceIdentityWithoutReusingRecordId() {
        val source = Recipe(
            id = "recipe-source",
            name = "V60 dulce",
            ingredientsSummary = "Café (15 G), Agua (240 ML)",
            stepsSummary = "1. Bloom\n2. Vertido"
        )

        val draft = source.toRecipeDraft(isClone = true)

        assertNotEquals(source.id, draft.id)
        assertEquals(source.id, draft.sourceRecipeId)
        assertEquals("Copia de V60 dulce", draft.name)
        assertEquals(2, draft.ingredients.size)
        assertEquals(2, draft.steps.size)
    }
}
