package com.example

import com.example.data.database.Bean
import com.example.data.database.Recipe
import com.example.data.database.RecipeIngredient
import com.example.data.engine.IngredientSuggestionEngine
import com.example.data.engine.RecipeTextParser
import org.junit.Assert.*
import org.junit.Test

class RecipeModuleTest {

    @Test
    fun testRecipeTextParser_basicParsing() {
        val text = """
            Receta: Espresso Tonic Menta
            Ingredientes:
            - 30 ml Espresso extraído
            - 150 ml Agua tónica
            - 2 unidades Hielo
            Pasos:
            1. Llenar vaso con hielo
            2. Verter agua tónica
            3. Verter espresso encima
            Notas:
            Refrescante y efervescente.
        """.trimIndent()

        val draft = RecipeTextParser.parse(text)

        assertEquals("Espresso Tonic Menta", draft.name)
        assertEquals("COLD_DRINK", draft.recipeKind)
        assertEquals(3, draft.ingredients.size)
        assertEquals("Espresso extraído", draft.ingredients[0].name)
        assertEquals("30", draft.ingredients[0].amount)
        assertEquals("ML", draft.ingredients[0].unit)
        assertEquals(3, draft.steps.size)
        assertTrue(draft.intention.contains("Refrescante"))
    }

    @Test
    fun testIngredientSuggestionEngine_indexingAndFiltering() {
        val recipes = listOf(
            Recipe(name = "Espresso Tonic", ingredientsSummary = "Espresso extraído (30ML), Agua tónica (150ML)"),
            Recipe(name = "V60 Clásico", ingredientsSummary = "Café de especialidad (15G), Agua filtrada (240ML)"),
            Recipe(name = "Capuchino", ingredientsSummary = "Espresso extraído (30ML), Leche entera (150ML)")
        )

        val beans = listOf(
            Bean(roaster = "Finca Santa Rosa", name = "Geisha Lavado", origin = "Guatemala", altitude = "1800m", process = "Lavado", roastDate = "2026-07-01", firstUseDate = "2026-07-10", notes = "", stockGrams = 250f)
        )

        val index = IngredientSuggestionEngine.buildIndex(recipes = recipes, beans = beans)

        // Suggestion query matching substring "esp"
        val suggestions = IngredientSuggestionEngine.getSuggestions("esp", index)
        assertTrue(suggestions.isNotEmpty())
        assertEquals("Espresso extraído", suggestions.first().name)
        assertEquals("ML", suggestions.first().defaultUnit)

        // Suggestion query matching bean "geisha"
        val beanSuggestions = IngredientSuggestionEngine.getSuggestions("geisha", index)
        assertTrue(beanSuggestions.isNotEmpty())
        assertTrue(beanSuggestions.first().isFromStorageBean)
        assertEquals("Finca Santa Rosa Geisha Lavado", beanSuggestions.first().name)
    }

    @Test
    fun testIngredientFieldBindingAndNoCrossing() {
        val inputName = "café"
        val inputQuantity = "20"
        val inputUnit = "G"

        // 1. Test creation of RecipeIngredientInput
        val input = com.example.data.engine.RecipeIngredientInput(
            name = inputName,
            amount = inputQuantity,
            unit = inputUnit
        )
        assertEquals("café", input.name)
        assertEquals("20", input.amount)
        assertEquals("G", input.unit)

        // 2. Format as summary string
        val summary = "${input.name} (${input.amount} ${input.unit})"
        assertEquals("café (20 G)", summary)

        // 3. Parse summary back and assert fields return in exact position without crossing
        val parsedList = IngredientSuggestionEngine.parseIngredientsSummary(summary)
        assertEquals(1, parsedList.size)
        val parsed = parsedList.first()

        assertEquals("café", parsed.name)
        assertEquals("20", parsed.amount)
        assertEquals("G", parsed.unit)
        assertEquals("20 G", parsed.displayQuantity)

        // 4. Test Entity mapping
        val entity = RecipeIngredient(
            recipeId = "rec-123",
            name = inputName,
            amount = inputQuantity.toFloat(),
            unit = inputUnit,
            orderIndex = 0
        )
        assertEquals("café", entity.name)
        assertEquals(20f, entity.amount)
        assertEquals("G", entity.unit)
    }
}
