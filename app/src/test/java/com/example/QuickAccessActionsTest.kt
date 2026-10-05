package com.example

import com.example.data.database.Recipe
import com.example.ui.screens.QuickAccessAction
import com.example.ui.screens.recipeShareText
import org.junit.Assert.*
import org.junit.Test

class QuickAccessActionsTest {
    @Test fun legacySectionShortcutsBecomeRealActionsWithoutLosingSupportedChoices() {
        assertEquals(QuickAccessAction.defaults, QuickAccessAction.normalize(setOf("cata", "lab", "storage", "social")))
        assertEquals(setOf("add_equipment"), QuickAccessAction.normalize(setOf("add_equipment", "storage")))
        assertEquals(listOf("add_coffee", "add_equipment", "share_recipe", "add_recipe", "add_technique", "add_grinder"), QuickAccessAction.entries.map { it.id })
    }
    @Test fun minimumOneChoiceAndPersistenceRoundTrip() {
        val selected = setOf("share_recipe")
        assertEquals(selected, QuickAccessAction.toggle(selected, "share_recipe"))
        val updated = QuickAccessAction.toggle(selected, "add_coffee")
        assertEquals(updated, QuickAccessAction.normalize(updated.sorted().joinToString(",").split(",").toSet()))
        assertEquals(updated, QuickAccessAction.toggle(updated, "invalid"))
    }
    @Test fun sharingIncludesInstructionsButNoPrivateSyncMetadata() {
        val recipe = Recipe(name = "V60 dulce", intention = "Taza redonda", legacyMethodName = "V60",
            ingredientsSummary = "15 g café\n240 ml agua", stepsSummary = "1. Bloom 45 ml\n2. Agregar 195 ml",
            ownerUserId = "private-owner", remoteId = "private-remote")
        val text = recipeShareText(recipe)
        listOf("V60 dulce", "Taza redonda", "15 g", "240 ml", "Bloom 45 ml", "Agregar 195 ml", "Método: V60").forEach { assertTrue(text.contains(it)) }
        assertFalse(text.contains("private-owner")); assertFalse(text.contains("private-remote"))
        assertTrue(recipeShareText(recipe.copy(legacyMethodName = null, suggestedMethodId = "AeroPress")).contains("Método: AeroPress"))
        val internalMethod = "00000000-0000-0000-0000-000000000001"
        assertFalse(recipeShareText(recipe.copy(legacyMethodName = null, suggestedMethodId = internalMethod)).contains(internalMethod))
    }
}
